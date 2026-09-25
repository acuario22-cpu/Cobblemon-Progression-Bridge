package com.hisroyalty.gachamachine.compat.jei;

import com.hisroyalty.gachamachine.GachaMachine;
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

public class GachaRewardCategory implements IRecipeCategory<GachaRewardRecipe> {
    public static final RecipeType<GachaRewardRecipe> TYPE =
        RecipeType.create(GachaMachine.MOD_ID, "gacha_rewards", GachaRewardRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public GachaRewardCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(154, 48);
        icon = gui.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
            new ItemStack(GachaMachine.MACHINES.get("gacha_machine").get()));
    }

    @Override public RecipeType<GachaRewardRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.gachamachine.gacha_rewards"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GachaRewardRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 8, 18).addItemStack(recipe.machine());
        builder.addSlot(RecipeIngredientRole.INPUT, 46, 18).addItemStack(recipe.currency());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 18).addItemStack(recipe.reward());
    }

    @Override
    public void draw(GachaRewardRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal("5x"), 67, 22, 0x404040, false);
        graphics.drawString(font, Component.literal(String.format(java.util.Locale.ROOT, "%.1f%%", recipe.chance())),
            88, 22, 0x404040, false);
    }
}
