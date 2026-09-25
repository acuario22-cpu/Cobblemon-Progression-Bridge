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
        background = gui.createBlankDrawable(230, 126);
        icon = gui.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
            new ItemStack(CobbledGacha.MACHINES.get("gacha_machine_4").get()));
    }

    @Override public RecipeType<PokemonGachaRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.cobbledgacha.pokemon_rewards"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PokemonGachaRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 7, 7).addItemStack(recipe.machine());
        builder.addSlot(RecipeIngredientRole.INPUT, 39, 7).addItemStacks(recipe.currencies());
    }

    @Override
    public void draw(PokemonGachaRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal(recipe.cost() + "x"), 62, 12, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.cobbledgacha.pokemon_headers"), 7, 31, 0x202020, false);

        int y = 45;
        for (PokemonRewardLine entry : recipe.entries()) {
            String species = entry.species().replace('_', ' ');
            if (!species.isEmpty()) species = Character.toUpperCase(species.charAt(0)) + species.substring(1);
            if (species.length() > 18) species = species.substring(0, 18) + "…";
            String level = entry.minLevel() == entry.maxLevel()
                ? Integer.toString(entry.minLevel())
                : entry.minLevel() + "-" + entry.maxLevel();
            graphics.drawString(font, Component.literal(species), 7, y, 0x303030, false);
            graphics.drawString(font, Component.literal(entry.bucket()), 105, y, 0x505050, false);
            graphics.drawString(font, Component.literal(level), 160, y, 0x505050, false);
            graphics.drawString(font, Component.literal(String.format(java.util.Locale.ROOT, "%.3f%%", entry.chance())), 190, y, 0x505050, false);
            y += 11;
        }
    }
}
