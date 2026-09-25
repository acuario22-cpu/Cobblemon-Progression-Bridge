package dev.cobblemodern.compat.jei;

import dev.cobblemodern.ModernBackport;
import dev.cobblemodern.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class ModernBackportJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
        new ResourceLocation(ModernBackport.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(
            new ItemStack(ModItems.MOVE_DEX.get()),
            Component.literal("Move Dex"),
            Component.literal("Registra permanentemente los movimientos conocidos por los Pokémon que posees."),
            Component.literal("Escanea tu equipo y también los Pokémon guardados en el PC."),
            Component.literal("Clic derecho para abrir la interfaz.")
        );

        registration.addItemStackInfo(
            new ItemStack(ModItems.TM_MACHINE.get()),
            Component.literal("Máquina de MT"),
            Component.literal("Permite fabricar MT de movimientos registrados en tu Move Dex."),
            Component.literal("Coste: 1 MT en blanco + 1 gema del tipo + 1 redstone."),
            Component.literal("Clic derecho para abrir la interfaz.")
        );
    }
}
