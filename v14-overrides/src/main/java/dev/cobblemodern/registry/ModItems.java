package dev.cobblemodern.registry;

import dev.cobblemodern.ModernBackport;
import dev.cobblemodern.item.MoveDexItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, ModernBackport.MOD_ID);

    public static final RegistryObject<Item> TM_MACHINE = ITEMS.register("tm_machine",
        () -> new BlockItem(ModBlocks.TM_MACHINE.get(), new Item.Properties()));

    public static final RegistryObject<Item> HABITAT_CORE = ITEMS.register("habitat_core",
        () -> new BlockItem(ModBlocks.HABITAT_CORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> MOVE_DEX = ITEMS.register("move_dex",
        () -> new MoveDexItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BLANK_TM = ITEMS.register("blank_tm",
        () -> new Item(new Item.Properties().stacksTo(64)));

    private ModItems() {}
}
