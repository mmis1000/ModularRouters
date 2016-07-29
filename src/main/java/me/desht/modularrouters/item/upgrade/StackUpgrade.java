package me.desht.modularrouters.item.upgrade;

import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.gui.GuiItemRouter;
import me.desht.modularrouters.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class StackUpgrade extends Upgrade {
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemstack, EntityPlayer player, List<String> list, boolean par4) {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiItemRouter) {
            TileEntityItemRouter router = ((GuiItemRouter) Minecraft.getMinecraft().currentScreen).tileEntityItemRouter;
            if (router != null) {
                MiscUtil.appendMultiline(list, "itemText.misc.stackUpgrade", router.getItemsPerTick(), 6);
            }
        }
    }
}
