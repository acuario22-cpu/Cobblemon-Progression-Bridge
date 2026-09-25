package com.hisroyalty.cobbledgacha.compat.jei;

import com.hisroyalty.cobbledgacha.CobbledGacha;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class PokemonGachaCategory implements IRecipeCategory<PokemonGachaRecipe> {
    public static final RecipeType<PokemonGachaRecipe> TYPE = RecipeType.create(CobbledGacha.MOD_ID, "pokemon_gacha", PokemonGachaRecipe.class);
    private final IDrawable background, icon;

    public PokemonGachaCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(190, 62);
        icon = gui.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
            new ItemStack(CobbledGacha.MACHINES.get("gacha_machine_4").get()));
    }

    @Override public RecipeType<PokemonGachaRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.cobbledgacha.pokemon_rewards"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PokemonGachaRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 7, 23).addItemStack(recipe.machine());
        builder.addSlot(RecipeIngredientRole.INPUT, 42, 23).addItemStacks(recipe.currencies());
    }

    @Override
    public void draw(PokemonGachaRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        String species = recipe.species().replace('_', ' ');
        if (!species.isEmpty()) species = Character.toUpperCase(species.charAt(0)) + species.substring(1);
        graphics.drawString(font, Component.literal(species), 72, 8, 0x202020, false);
        graphics.drawString(font, Component.literal("Rareza: " + recipe.bucket()), 72, 21, 0x404040, false);
        String levels = recipe.minLevel() == recipe.maxLevel() ? "Nv. " + recipe.minLevel() : "Nv. " + recipe.minLevel() + "-" + recipe.maxLevel();
        graphics.drawString(font, Component.literal(levels), 72, 34, 0x404040, false);
        graphics.drawString(font, Component.literal(String.format(java.util.Locale.ROOT, "%.3f%%", recipe.chance())), 135, 34, 0x404040, false);
        graphics.drawString(font, Component.literal(recipe.cost() + "x"), 48, 45, 0x404040, false);
    }
}
