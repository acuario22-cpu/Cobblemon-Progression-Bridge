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

public class MachineRewardCategory implements IRecipeCategory<MachineRewardRecipe> {
    public static final RecipeType<MachineRewardRecipe> TYPE = RecipeType.create(CobbledGacha.MOD_ID, "machine_rewards", MachineRewardRecipe.class);
    private final IDrawable background, icon;

    public MachineRewardCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(176, 54);
        icon = gui.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
            new ItemStack(CobbledGacha.MACHINES.get("gacha_machine").get()));
    }

    @Override public RecipeType<MachineRewardRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.cobbledgacha.machine_rewards"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRewardRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 7, 20).addItemStacks(recipe.machines());
        builder.addSlot(RecipeIngredientRole.INPUT, 52, 20).addItemStacks(recipe.currencies());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 146, 20).addItemStack(recipe.reward());
    }

    @Override
    public void draw(MachineRewardRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal(recipe.cost() + "x"), 74, 24, 0x404040, false);
        graphics.drawString(font, Component.literal(String.format(java.util.Locale.ROOT, "%.2f%%", recipe.chance())), 101, 24, 0x404040, false);
    }
}
