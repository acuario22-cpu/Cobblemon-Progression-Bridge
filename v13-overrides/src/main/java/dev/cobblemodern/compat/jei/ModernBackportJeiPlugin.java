package dev.cobblemodern.compat.jei;

import dev.cobblemodern.ModernBackport;
import dev.cobblemodern.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
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
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(ModItems.MOVE_DEX.get()));
        stacks.add(new ItemStack(ModItems.TM_MACHINE.get()));
        registration.addExtraItemStacks(stacks);
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

        Item blank = ForgeRegistries.ITEMS.getValue(new ResourceLocation("simpletms", "tm_blank"));
        if (blank != null && blank != Items.AIR) {
            registration.addItemStackInfo(
                new ItemStack(blank),
                Component.literal("MT en blanco"),
                Component.literal("Material base de la Máquina de MT."),
                Component.literal("Ahora es crafteable mediante Cobblemon Modern Backport.")
            );
        }
    }
}
