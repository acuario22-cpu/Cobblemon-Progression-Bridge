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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class PlushCategory implements IRecipeCategory<PlushRecipe> {
    public static final RecipeType<PlushRecipe> TYPE =
        RecipeType.create(CobbledGacha.MOD_ID, "plush_o_matic", PlushRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public PlushCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(210, 72);
        icon = gui.createDrawableIngredient(
            mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
            new ItemStack(CobbledGacha.MACHINES.get("gacha_machine_12").get())
        );
    }

    @Override public RecipeType<PlushRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.cobbledgacha.plush_o_matic"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PlushRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 8, 24).addItemStack(recipe.machine());
        builder.addSlot(RecipeIngredientRole.INPUT, 56, 24).addItemStack(recipe.yarn());
        if (!recipe.doll().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 178, 24).addItemStack(recipe.doll());
        }
    }

    @Override
    public void draw(PlushRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal("3x"), 78, 28, 0x404040, false);

        graphics.drawString(
            font,
            Component.literal(recipe.dollName()),
            104,
            8,
            recipe.available() ? 0x303030 : 0xAA2222,
            false
        );

        graphics.drawString(
            font,
            Component.translatable("jei.cobbledgacha.plush_variant", Component.translatable(recipe.variant())),
            104,
            21,
            0x404040,
            false
        );

        graphics.drawString(
            font,
            Component.literal(String.format(java.util.Locale.ROOT, "%.2f%%", recipe.chance())),
            104,
            34,
            0x404040,
            false
        );

        if (!recipe.available()) {
            graphics.drawString(
                font,
                Component.translatable("jei.cobbledgacha.pokeblocks_missing")
                    .withStyle(ChatFormatting.RED),
                104,
                49,
                0xAA2222,
                false
            );
        }
    }
}
