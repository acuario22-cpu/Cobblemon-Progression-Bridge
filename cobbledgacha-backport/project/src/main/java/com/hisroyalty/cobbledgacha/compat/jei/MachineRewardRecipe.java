package com.hisroyalty.cobbledgacha.compat.jei;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public record MachineRewardRecipe(ItemStack machine, List<ItemStack> currencies, ItemStack reward, float chance, int cost) {}
