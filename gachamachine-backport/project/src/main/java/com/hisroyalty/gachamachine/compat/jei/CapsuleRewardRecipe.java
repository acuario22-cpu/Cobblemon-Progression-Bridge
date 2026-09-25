package com.hisroyalty.gachamachine.compat.jei;

import net.minecraft.world.item.ItemStack;

public record CapsuleRewardRecipe(ItemStack capsule, ItemStack reward, float chance) {}
