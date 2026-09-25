package com.hisroyalty.gachamachine.compat.jei;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public record GachaRewardRecipe(List<ItemStack> machines, List<ItemStack> currencies, ItemStack reward, float chance) {}
