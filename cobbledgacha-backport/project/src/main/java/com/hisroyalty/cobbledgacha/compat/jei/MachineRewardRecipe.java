package com.hisroyalty.cobbledgacha.compat.jei;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public record MachineRewardRecipe(List<ItemStack> machines, List<ItemStack> currencies, ItemStack reward, float chance, int cost) {}
