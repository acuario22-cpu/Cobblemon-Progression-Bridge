package dev.cobblemodern.compat.jei;

import dev.cobblemodern.ModernBackport;
import dev.cobblemodern.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public final class ModernBackportJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
        new ResourceLocation(ModernBackport.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(List.of(
            new ItemStack(ModItems.MOVE_DEX.get()),
            new ItemStack(ModItems.TM_MACHINE.get()),
            new ItemStack(ModItems.BLANK_TM.get())
        ));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(
            new ItemStack(ModItems.MOVE_DEX.get()),
            Component.literal("Move Dex"),
            Component.literal("Registra permanentemente movimientos de los Pokémon que posees."),
            Component.literal("Escanea tu equipo y también los Pokémon guardados en el PC."),
            Component.literal("Clic derecho para abrir la interfaz.")
        );

        registration.addItemStackInfo(
            new ItemStack(ModItems.TM_MACHINE.get()),
            Component.literal("Máquina de MT"),
            Component.literal("Fabrica MT de movimientos registrados en el Move Dex."),
            Component.literal("Coste por MT: 1 MT en blanco + 1 gema del tipo + 1 redstone."),
            Component.literal("Clic derecho para abrir la interfaz.")
        );

        registration.addItemStackInfo(
            new ItemStack(ModItems.BLANK_TM.get()),
            Component.literal("MT en blanco"),
            Component.literal("Material base de la Máquina de MT."),
            Component.literal("Se fabrica con papel, cobre y redstone.")
        );
    }
}
