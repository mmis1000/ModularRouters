package me.desht.modularrouters.logic.compiled;

import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.core.ModItems;
import me.desht.modularrouters.item.augment.ItemAugment.AugmentCounter;
import me.desht.modularrouters.item.module.IRangedModule;
import me.desht.modularrouters.item.module.ItemModule;
import me.desht.modularrouters.item.module.ItemModule.RelativeDirection;
import me.desht.modularrouters.logic.ModuleTarget;
import me.desht.modularrouters.logic.RouterRedstoneBehaviour;
import me.desht.modularrouters.logic.filter.Filter;
import me.desht.modularrouters.util.BlockUtil;
import me.desht.modularrouters.util.CountedItemStacks;
import me.desht.modularrouters.util.MiscUtil;
import me.desht.modularrouters.util.ModuleHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public abstract class CompiledModule {
    private final Filter filter;
    private final ItemModule module;
    private final RelativeDirection direction;
    private final List<ModuleTarget> targets;
    private final RouterRedstoneBehaviour behaviour;
    private final boolean termination;
    private final Direction facing;
    private final Direction routerFacing;
    private final int regulationAmount;
    private final AugmentCounter augmentCounter;
    private final int range, rangeSquared;

    private int lastMatchPos = 0;

    /**
     * Base constructor for compiled modules.  This can be called for both installed and uninstalled modules;
     * when the module is not installed in a router, null can be passed, so any subclass constructors must be able to
     * handle a null router.
     *
     * @param router router the module is installed in, may be null for an uninstalled module
     * @param stack item stack of the module item being compiled
     */
    CompiledModule(@Nullable TileEntityItemRouter router, ItemStack stack) {
        Validate.isTrue(stack.getItem() instanceof ItemModule, "expected module item, got " + stack);

        module = (ItemModule) stack.getItem();
        augmentCounter = new AugmentCounter(stack);
        direction = ModuleHelper.getDirectionFromNBT(stack);
        range = module instanceof IRangedModule ?
                ((IRangedModule) module).getCurrentRange(getRangeModifier()) : 0;
        rangeSquared = range * range;
        targets = setupTargets(router, stack);
        filter = new Filter(stack, shouldStoreRawFilterItems());
        termination = ModuleHelper.terminates(stack);
        behaviour = ModuleHelper.getRedstoneBehaviour(stack);
        regulationAmount = ModuleHelper.getRegulatorAmount(stack);
        facing = router == null ? null : router.getAbsoluteFacing(direction);
        routerFacing = router == null ? null : router.getAbsoluteFacing(RelativeDirection.FRONT);
    }

    /**
     * Execute this installed module.  When this is called, the router has already verified that the module has a
     * valid target, i.e. getTarget() will not return null.  This should only be called by the router.
     *
     * @param router router the module is installed in, may <strong>not</strong> be null
     * @return true if the module did some work, false otherwise
     */
    public abstract boolean execute(@Nonnull TileEntityItemRouter router);

    @Nonnull
    Filter getFilter() {
        return filter;
    }

    public RelativeDirection getDirection() {
        return direction;
    }

    protected boolean shouldStoreRawFilterItems() {
        return false;
    }

    /**
     * Get the static target for a router module.  This is set up when the router is compiled, and does not
     * necessarily reflect the true target for all modules.  Use {@link #getEffectiveTarget(TileEntityItemRouter)}
     * to get the actual target that should be interacted with when executing the module.
     *
     * @return the first target as set up by {@link #setupTargets(TileEntityItemRouter, ItemStack)}
     */
    ModuleTarget getTarget() {
        return targets == null || targets.isEmpty() ? null : targets.get(0);
    }

    /**
     * Used by modules which can store multiple targets, e.g. the distributor module.  See also
     * {@link #getTarget()}.
     *
     * @return a list of the defined targets as set up by {@link #setupTargets(TileEntityItemRouter, ItemStack)}
     */
    List<ModuleTarget> getTargets() {
        return targets;
    }

    public boolean hasTarget() { return targets != null && !targets.isEmpty(); }

    public boolean termination() {
        return termination;
    }

    RouterRedstoneBehaviour getRedstoneBehaviour() {
        return behaviour;
    }

    public int getRegulationAmount() {
        return augmentCounter.getAugmentCount(ModItems.REGULATOR_AUGMENT.get()) > 0 ? regulationAmount : 0;
    }

    int getAugmentCount(Item augmentType) {
        return augmentCounter.getAugmentCount(augmentType);
    }

    /**
     * Get the absolute direction this module faces, based on its relative direction and the facing of the router
     * in which it's installed.  This will be null if the module isn't installed in a router, or the router's facing
     * direction if the module's relative direction is NONE.
     *
     * @return absolute direction of the module
     */
    Direction getFacing() {
        return facing;
    }

    public void onCompiled(TileEntityItemRouter router) {
        if (behaviour == RouterRedstoneBehaviour.PULSE) {
            router.setHasPulsedModules(true);
        }
    }

    public void cleanup(TileEntityItemRouter router) {
        // does nothing by default
    }

    /**
     * Get the last position where we found a match.  Caching this can help reduce the amount of inventory searching
     * needed for some modules.
     *
     * @param offset offset from the last saved position
     * @param size size of the inventory being searched
     * @return the last position including offset, and wrapped to start of inventory if necessary
     */
    private int getLastMatchPos(int offset, int size) {
        int pos = lastMatchPos + offset;
        while (pos >= size) pos -= size;
        return pos;
    }

    /**
     * Store the last position where we found a match.
     *
     * @param lastMatchPos last matched position
     */
    private void setLastMatchPos(int lastMatchPos) {
        this.lastMatchPos = lastMatchPos;
    }

    /**
     * Default target for this module is the block adjacent to the router, in the module's
     * configured direction.  Can be overridden by submodules.
     *
     * @param router router in which the module is installed
     * @param stack the module itemstack
     * @return a list of router target objects (for most modules this is a singleton list)
     */
    List<ModuleTarget> setupTargets(TileEntityItemRouter router, ItemStack stack) {
        if (router == null || (module.isDirectional() && direction == RelativeDirection.NONE)) {
            return null;
        }
        Direction facing = router.getAbsoluteFacing(direction);
        BlockPos pos = router.getPos().offset(facing);
        String blockName = BlockUtil.getBlockName(router.getWorld(), pos);
        GlobalPos gPos = MiscUtil.makeGlobalPos(router.getWorld(), pos);
        return Collections.singletonList(new ModuleTarget(gPos, facing.getOpposite(), blockName));
    }

    int getItemsPerTick(TileEntityItemRouter router) {
        int n = augmentCounter.getAugmentCount(ModItems.STACK_AUGMENT.get());
        return n > 0 ? Math.min(1 << n, 64) : router.getItemsPerTick();
    }

    /**
     * Try to transfer some items from the given ItemHandler to the given router.  The number of
     * items attempted depends on the router's stack upgrades.
     *
     * @param handler the item handler
     * @param router  the router
     * @return items actually transferred
     */
    ItemStack transferToRouter(IItemHandler handler, TileEntityItemRouter router) {
        CountedItemStacks count = getRegulationAmount() > 0 ? new CountedItemStacks(handler) : null;

        ItemStack wanted = findItemToPull(router, handler, getItemsPerTick(router), count);
        if (wanted.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack transferred = ItemStack.EMPTY;
        for (int i = 0; i < handler.getSlots(); i++) {
            int pos = getLastMatchPos(i, handler.getSlots());
            ItemStack toPull = handler.extractItem(pos, wanted.getCount(), true);
            if (toPull.isEmpty()) {
                // we'd found an item to pull but it looks like this handler doesn't allow us to extract it
                // give up, but advance the last match pos so we don't get stuck trying this slot forever
                setLastMatchPos((pos + 1) % handler.getSlots());
                return ItemStack.EMPTY;
            }
            if (ItemHandlerHelper.canItemStacksStack(wanted, toPull)) {
                // this item is suitable for pulling
                ItemStack notInserted = router.insertBuffer(toPull);
                int inserted = toPull.getCount() - notInserted.getCount();
                transferred = handler.extractItem(pos, inserted, false);
                wanted.shrink(inserted);
                if (wanted.isEmpty() || router.isBufferFull()) {
                    setLastMatchPos(handler.getStackInSlot(pos).isEmpty() ? (pos + 1) % handler.getSlots() : pos);
                    return transferred;
                }
            }
        }
        return transferred;
    }

    private ItemStack findItemToPull(TileEntityItemRouter router, IItemHandler handler, int nToTake, CountedItemStacks count) {
        ItemStack stackInRouter = router.peekBuffer(1);
        if (!stackInRouter.isEmpty() && getFilter().test(stackInRouter)) {
            // something in the router - try to pull more of that
            return ItemHandlerHelper.copyStackWithSize(stackInRouter, nToTake);
        } else if (stackInRouter.isEmpty()) {
            // router empty - just pull the next item that passes the filter
            for (int i = 0; i < handler.getSlots(); i++) {
                int pos = getLastMatchPos(i, handler.getSlots());
                ItemStack stack = handler.getStackInSlot(pos);
                if (getFilter().test(stack) && (count == null || count.getInt(stack) - nToTake >= getRegulationAmount())) {
                    setLastMatchPos(pos);
                    return ItemHandlerHelper.copyStackWithSize(stack, nToTake);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Get the effective target for this module, which is not necessarily the same as the result of getTarget().
     * E.g. for a Sender Mk1, the real target may be a few blocks away, and may change without router recompilation
     * if blocks are placed or removed.
     *
     * @return the real target for this module
     */
    public ModuleTarget getEffectiveTarget(TileEntityItemRouter router) {
        return getTarget();
    }

    public boolean shouldRun(boolean powered, boolean pulsed) {
        return getRedstoneBehaviour().shouldRun(powered, pulsed);
    }

    boolean isRegulationOK(TileEntityItemRouter router, boolean inbound) {
        if (regulationAmount == 0) return true; // no regulation
        int items = router.getBufferItemStack().getCount();
        return inbound && regulationAmount > items || !inbound && regulationAmount < items;
    }

    int getRange() {
        return range;
    }

    int getRangeSquared() {
        return rangeSquared;
    }

    private int getRangeModifier() {
        return getAugmentCount(ModItems.RANGE_UP_AUGMENT.get()) - getAugmentCount(ModItems.RANGE_DOWN_AUGMENT.get());
    }

    Direction getRouterFacing() {
        return routerFacing;
    }

    public void onNeighbourChange(TileEntityItemRouter router) {
    }
}
