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

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new MachineRewardCategory(gui),
            new CapsuleRewardCategory(gui),
            new PokemonGachaCategory(gui),
            new PlushCategory(gui),
            new BallPokemonCategory(gui)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            MachineRewardCategory.TYPE,
            CobbledJeiData.machineRewards()
        );
        registration.addRecipes(
            CapsuleRewardCategory.TYPE,
            CobbledJeiData.capsuleRewards()
        );
        registration.addRecipes(
            PokemonGachaCategory.TYPE,
            CobbledJeiData.pokemonRewards()
        );
        registration.addRecipes(
            PlushCategory.TYPE,
            CobbledJeiData.plushRewards()
        );
        registration.addRecipes(
            BallPokemonCategory.TYPE,
            CobbledJeiData.ballPokemonRewards()
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (int i = 1; i <= 12; i++) {
            String name = i == 1 ? "gacha_machine" : "gacha_machine_" + i;
            var machine = CobbledGacha.MACHINES.get(name);
            if (machine == null) continue;

            if (i == 4) {
                registration.addRecipeCatalyst(
                    new ItemStack(machine.get()),
                    PokemonGachaCategory.TYPE
                );
            } else if (i == 12) {
                registration.addRecipeCatalyst(
                    new ItemStack(machine.get()),
                    PlushCategory.TYPE
                );
            } else {
                registration.addRecipeCatalyst(
                    new ItemStack(machine.get()),
                    MachineRewardCategory.TYPE
                );
            }
        }

        for (int i = 0;
             i < Math.min(CobbledGacha.USEFUL_CAPSULE_COUNT, CobbledGacha.CAPSULES.size());
             i++) {
            registration.addRecipeCatalyst(
                new ItemStack(CobbledGacha.CAPSULES.get(i).get()),
                CapsuleRewardCategory.TYPE
            );
        }

        for (var entry : CobbledGacha.EXTRA_ITEMS.entrySet()) {
            String name = entry.getKey();

            if (name.endsWith("_yarn")) {
                registration.addRecipeCatalyst(
                    new ItemStack(entry.getValue().get()),
                    PlushCategory.TYPE
                );
            }

            if (name.equals("rocket_ball") || name.startsWith("gacha_ball_")) {
                registration.addRecipeCatalyst(
                    new ItemStack(entry.getValue().get()),
                    BallPokemonCategory.TYPE
                );
            }
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        var hidden = CobbledJeiData.hiddenCapsules();
        if (!hidden.isEmpty()) {
            jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(
                VanillaTypes.ITEM_STACK,
                hidden
            );
        }
    }
}
