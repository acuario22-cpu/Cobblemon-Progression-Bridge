package com.hisroyalty.gachamachine.compat.jei;

import net.minecraft.world.item.ItemStack;

public record GachaRewardRecipe(ItemStack machine, ItemStack currency, ItemStack reward, float chance) {}
