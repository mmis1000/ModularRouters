package me.desht.modularrouters.client.gui.module;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.modularrouters.client.gui.widgets.button.ItemStackButton;
import me.desht.modularrouters.client.gui.widgets.button.TexturedCyclerButton;
import me.desht.modularrouters.client.gui.widgets.button.TexturedToggleButton;
import me.desht.modularrouters.client.gui.widgets.textfield.IntegerTextField;
import me.desht.modularrouters.client.gui.widgets.textfield.TextFieldManager;
import me.desht.modularrouters.client.util.GuiUtil;
import me.desht.modularrouters.config.MRConfig;
import me.desht.modularrouters.container.ContainerModule;
import me.desht.modularrouters.core.ModBlocks;
import me.desht.modularrouters.item.module.FluidModule1.FluidDirection;
import me.desht.modularrouters.logic.compiled.CompiledFluidModule1;
import me.desht.modularrouters.util.MiscUtil;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.gui.widget.ExtendedButton;
import org.apache.commons.lang3.Range;

import java.util.Collections;
import java.util.List;

import static me.desht.modularrouters.client.util.ClientUtil.xlate;

public class GuiModuleFluid extends GuiModule {
    private static final ItemStack bucketStack = new ItemStack(Items.BUCKET);
    private static final ItemStack routerStack = new ItemStack(ModBlocks.ITEM_ROUTER.get());
    private static final ItemStack waterStack = new ItemStack(Items.WATER_BUCKET);

    private ForceEmptyButton forceEmptyButton;
    private RegulateAbsoluteButton regulationTypeButton;
    private FluidDirectionButton fluidDirButton;
    private IntegerTextField maxTransferField;

    public GuiModuleFluid(ContainerModule container, PlayerInventory inv, ITextComponent displayName) {
        super(container, inv, displayName);
    }

    @Override
    public void init() {
        super.init();

        CompiledFluidModule1 cfm = new CompiledFluidModule1(null, moduleItemStack);

        TextFieldManager manager = getOrCreateTextFieldManager();

        int max = MRConfig.Common.Router.baseTickRate * MRConfig.Common.Router.fluidMaxTransferRate;
        maxTransferField = new IntegerTextField(manager, font, guiLeft + 152, guiTop + 23, 34, 12,
                Range.between(0, max));
        maxTransferField.setValue(cfm.getMaxTransfer());
        maxTransferField.setResponder(str -> sendModuleSettingsDelayed(5));
        maxTransferField.setIncr(100, 10, 10);
        maxTransferField.useGuiTextBackground();
        manager.focus(0);

        addButton(new TooltipButton(guiLeft + 130, guiTop + 19, 16, 16, bucketStack));
        addButton(fluidDirButton = new FluidDirectionButton(guiLeft + 148, guiTop + 44, cfm.getFluidDirection()));
        addButton(forceEmptyButton = new ForceEmptyButton(guiLeft + 168, guiTop + 69, cfm.isForceEmpty()));
        addButton(regulationTypeButton = new RegulateAbsoluteButton(regulatorTextField.x + regulatorTextField.getWidth() + 2, regulatorTextField.y - 1, 18, 14, b -> toggleRegulationType(), cfm.isRegulateAbsolute()));

        getMouseOverHelp().addHelpRegion(guiLeft + 128, guiTop + 17, guiLeft + 183, guiTop + 35, "modularrouters.guiText.popup.fluid.maxTransfer");
        getMouseOverHelp().addHelpRegion(guiLeft + 126, guiTop + 42, guiLeft + 185, guiTop + 61, "modularrouters.guiText.popup.fluid.direction");
        getMouseOverHelp().addHelpRegion(guiLeft + 128, guiTop + 67, guiLeft + 185, guiTop + 86, "modularrouters.guiText.popup.fluid.forceEmpty");
    }

    @Override
    protected IntegerTextField buildRegulationTextField(TextFieldManager manager) {
        IntegerTextField tf = new IntegerTextField(manager, font, guiLeft + 128, guiTop + 90, 40, 12, Range.between(0, Integer.MAX_VALUE));
        tf.setValue(getRegulatorAmount());
        tf.setResponder((str) -> {
            setRegulatorAmount(str.isEmpty() ? 0 : Integer.parseInt(str));
            sendModuleSettingsDelayed(5);
        });
        return tf;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(matrixStack, partialTicks, mouseX, mouseY);

        // text entry field custom background - super has already bound the correct texture
        this.blit(matrixStack, guiLeft + 146, guiTop + 20, LARGE_TEXTFIELD_XY.x, LARGE_TEXTFIELD_XY.y, 35, 14);

        GuiUtil.renderItemStack(matrixStack, minecraft, routerStack, guiLeft + 128, guiTop + 44, "");
        GuiUtil.renderItemStack(matrixStack, minecraft, waterStack, guiLeft + 168, guiTop + 44, "");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(matrixStack, mouseX, mouseY);

        if (forceEmptyButton.visible) {
            String s = I18n.format("modularrouters.guiText.label.fluidForceEmpty");
            font.drawString(matrixStack, s, 165 - font.getStringWidth(s), 73, 0x202040);
        }
    }

    @Override
    public void tick() {
        super.tick();

        regulationTypeButton.visible = regulatorTextField.visible;
        regulationTypeButton.setText();
        regulatorTextField.setRange(Range.between(0, regulationTypeButton.regulateAbsolute ? Integer.MAX_VALUE : 100));
        forceEmptyButton.visible = fluidDirButton.getState() == FluidDirection.OUT;
    }

    @Override
    protected CompoundNBT buildMessageData() {
        CompoundNBT compound = super.buildMessageData();
        compound.putInt(CompiledFluidModule1.NBT_MAX_TRANSFER, maxTransferField.getValue());
        compound.putByte(CompiledFluidModule1.NBT_FLUID_DIRECTION, (byte) fluidDirButton.getState().ordinal());
        compound.putBoolean(CompiledFluidModule1.NBT_FORCE_EMPTY, forceEmptyButton.isToggled());
        compound.putBoolean(CompiledFluidModule1.NBT_REGULATE_ABSOLUTE, regulationTypeButton.regulateAbsolute);
        return compound;
    }

    private class TooltipButton extends ItemStackButton {
        TooltipButton(int x, int y, int width, int height, ItemStack renderStack) {
            super(x, y, width, height, renderStack, true, p -> {});
            MiscUtil.appendMultilineText(tooltip1, TextFormatting.WHITE, "modularrouters.guiText.tooltip.fluidTransferTooltip");
            tooltip1.add(StringTextComponent.EMPTY.copyRaw());
            getItemRouter().ifPresent(router -> {
                int ftRate = router.getFluidTransferRate();
                int tickRate = router.getTickRate();
                tooltip1.add(xlate("modularrouters.guiText.tooltip.maxFluidPerOp", ftRate * tickRate, tickRate, ftRate));
                tooltip1.add(StringTextComponent.EMPTY.copyRaw());
            });
            MiscUtil.appendMultilineText(tooltip1, TextFormatting.WHITE, "modularrouters.guiText.tooltip.numberFieldTooltip");
        }

        @Override
        public void playDownSound(SoundHandler soundHandlerIn) {
            // no sound
        }
    }

    private void toggleRegulationType() {
        regulationTypeButton.toggle();
        regulatorTextField.setRange(regulationTypeButton.regulateAbsolute ? Range.between(0, Integer.MAX_VALUE) : Range.between(0, 100));
        sendToServer();
    }

    private class FluidDirectionButton extends TexturedCyclerButton<FluidDirection> {
        private final List<List<ITextComponent>> tooltips = Lists.newArrayList();

        FluidDirectionButton(int x, int y, FluidDirection initialVal) {
            super(x, y, 16, 16, initialVal, GuiModuleFluid.this);
            for (FluidDirection dir : FluidDirection.values()) {
                tooltips.add(Collections.singletonList(xlate(dir.getTranslationKey())));
            }
        }

        @Override
        protected int getTextureX() {
            return 160 + getState().ordinal() * 16;
        }

        @Override
        protected int getTextureY() {
            return 16;
        }

        @Override
        public List<ITextComponent> getTooltip() {
            return tooltips.get(getState().ordinal());
        }
    }

    private class ForceEmptyButton extends TexturedToggleButton {
        ForceEmptyButton(int x, int y, boolean initialVal) {
            super(x, y, 16, 16, initialVal, GuiModuleFluid.this);
            MiscUtil.appendMultilineText(tooltip1, TextFormatting.WHITE, "modularrouters.guiText.tooltip.fluidForceEmpty.false");
            MiscUtil.appendMultilineText(tooltip2, TextFormatting.WHITE, "modularrouters.guiText.tooltip.fluidForceEmpty.true");
        }

        @Override
        protected int getTextureX() {
            return isToggled() ? 192 : 112;
        }

        @Override
        protected int getTextureY() {
            return 16;
        }
    }

    private static class RegulateAbsoluteButton extends ExtendedButton {
        private boolean regulateAbsolute;

        public RegulateAbsoluteButton(int xPos, int yPos, int width, int height, IPressable pressable, boolean regulateAbsolute) {
            super(xPos, yPos, width, height, StringTextComponent.EMPTY, pressable);
            this.regulateAbsolute = regulateAbsolute;
        }

        private void toggle() {
            regulateAbsolute = !regulateAbsolute;
        }

        void setText() {
            setMessage(new StringTextComponent(regulateAbsolute ? "mB" : "%"));
        }
    }
}
