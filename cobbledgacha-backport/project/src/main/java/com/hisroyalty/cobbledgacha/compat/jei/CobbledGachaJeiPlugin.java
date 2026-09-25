package com.hisroyalty.cobbledgacha.compat.jei;

import com.hisroyalty.cobbledgacha.CobbledGacha;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class CobbledGachaJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = CobbledGacha.id("jei_plugin");

    @Override public ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new MachineRewardCategory(gui),
            new CapsuleRewardCategory(gui),
            new PokemonGachaCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MachineRewardCategory.TYPE, CobbledJeiData.machineRewards());
        registration.addRecipes(CapsuleRewardCategory.TYPE, CobbledJeiData.capsuleRewards());
        registration.addRecipes(PokemonGachaCategory.TYPE, CobbledJeiData.pokemonRewards());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (int i = 1; i <= 12; i++) {
            String name = i == 1 ? "gacha_machine" : "gacha_machine_" + i;
            var machine = CobbledGacha.MACHINES.get(name);
            if (machine == null) continue;
            registration.addRecipeCatalyst(new ItemStack(machine.get()),
                i == 4 ? PokemonGachaCategory.TYPE : MachineRewardCategory.TYPE);
        }

        for (int i = 0; i < Math.min(CobbledGacha.USEFUL_CAPSULE_COUNT, CobbledGacha.CAPSULES.size()); i++) {
            registration.addRecipeCatalyst(new ItemStack(CobbledGacha.CAPSULES.get(i).get()), CapsuleRewardCategory.TYPE);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        var hidden = CobbledJeiData.hiddenCapsules();
        if (!hidden.isEmpty()) {
            jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        }
    }
}
