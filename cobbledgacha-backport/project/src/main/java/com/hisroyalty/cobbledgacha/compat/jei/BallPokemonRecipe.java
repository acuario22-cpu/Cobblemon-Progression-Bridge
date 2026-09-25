package com.hisroyalty.cobbledgacha.compat.jei;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public record BallPokemonRecipe(ItemStack ball, List<PokemonRewardLine> entries) {}
