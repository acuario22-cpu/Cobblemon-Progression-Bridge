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

public class CapsuleRewardCategory implements IRecipeCategory<CapsuleRewardRecipe> {
    public static final RecipeType<CapsuleRewardRecipe> TYPE =
        RecipeType.create(GachaMachine.MOD_ID, "capsule_rewards", CapsuleRewardRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public CapsuleRewardCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(132, 48);
        icon = gui.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
            new ItemStack(GachaMachine.CAPSULES.get("capsule_a1").get()));
    }

    @Override public RecipeType<CapsuleRewardRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.gachamachine.capsule_rewards"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CapsuleRewardRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 18).addItemStack(recipe.capsule());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 18).addItemStack(recipe.reward());
    }

    @Override
    public void draw(CapsuleRewardRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.drawString(Minecraft.getInstance().font,
            Component.literal(String.format(java.util.Locale.ROOT, "%.1f%%", recipe.chance())),
            55, 22, 0x404040, false);
    }
}
