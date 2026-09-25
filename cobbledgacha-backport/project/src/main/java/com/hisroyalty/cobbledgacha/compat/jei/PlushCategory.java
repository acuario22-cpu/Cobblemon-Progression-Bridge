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

public class PlushCategory implements IRecipeCategory<PlushRecipe> {
    public static final RecipeType<PlushRecipe> TYPE =
        RecipeType.create(CobbledGacha.MOD_ID, "plush_o_matic", PlushRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public PlushCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(188, 62);
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
        builder.addSlot(RecipeIngredientRole.CATALYST, 8, 22).addItemStack(recipe.machine());
        builder.addSlot(RecipeIngredientRole.INPUT, 55, 22).addItemStack(recipe.yarn());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 157, 22).addItemStack(recipe.doll());
    }

    @Override
    public void draw(PlushRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal("3x"), 77, 26, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.cobbledgacha.plush_variant", recipe.variant()), 95, 10, 0x303030, false);
        graphics.drawString(font,
            Component.literal(String.format(java.util.Locale.ROOT, "%.2f%%", recipe.chance())),
            105, 30, 0x404040, false);
    }
}
