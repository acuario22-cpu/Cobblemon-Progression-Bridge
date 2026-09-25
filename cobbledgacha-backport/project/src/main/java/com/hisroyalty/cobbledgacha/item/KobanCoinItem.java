package com.hisroyalty.cobbledgacha.item;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class KobanCoinItem extends Item {
    public KobanCoinItem(Properties p){super(p);}
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag){
        tooltip.add(Component.translatable(net.minecraft.client.gui.screens.Screen.hasShiftDown()?"tooltip.cobbledgacha.koban_coin":"tooltip.cobbledgacha.koban_coin.hint").withStyle(ChatFormatting.GRAY));
    }
}
