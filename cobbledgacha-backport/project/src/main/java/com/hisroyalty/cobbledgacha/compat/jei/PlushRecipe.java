package com.hisroyalty.cobbledgacha.compat.jei;

import net.minecraft.world.item.ItemStack;

public record PlushRecipe(
    ItemStack machine,
    ItemStack yarn,
    ItemStack doll,
    String dollName,
    String variant,
    float chance,
    boolean available
) {}
