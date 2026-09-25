package com.hisroyalty.gachamachine.compat.jei;

import com.hisroyalty.gachamachine.GachaMachine;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class GachaJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = GachaMachine.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new GachaRewardCategory(gui), new CapsuleRewardCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(GachaRewardCategory.TYPE, GachaJeiData.machineRewards());
        registration.addRecipes(CapsuleRewardCategory.TYPE, GachaJeiData.capsuleRewards());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        GachaMachine.MACHINES.values().forEach(machine ->
            registration.addRecipeCatalyst(new ItemStack(machine.get()), GachaRewardCategory.TYPE));
        GachaMachine.CAPSULES.values().forEach(capsule ->
            registration.addRecipeCatalyst(new ItemStack(capsule.get()), CapsuleRewardCategory.TYPE));
    }
}
