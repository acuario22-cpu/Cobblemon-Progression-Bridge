package com.hisroyalty.cobbledgacha.compat.jei;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public record PokemonGachaRecipe(ItemStack machine, List<ItemStack> currencies, String species, String bucket,
                                 int minLevel, int maxLevel, float chance, int cost) {}
