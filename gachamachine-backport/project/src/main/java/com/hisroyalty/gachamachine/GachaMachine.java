package com.hisroyalty.gachamachine;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(GachaMachine.MOD_ID)
public class GachaMachine {
    public static final String MOD_ID = "gachamachine";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final Map<String, RegistryObject<GachaMachineBlock>> MACHINES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> COINS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<CapsuleItem>> CAPSULES = new LinkedHashMap<>();
    public static final RegistryObject<BlockEntityType<GachaMachineBlockEntity>> MACHINE_BE;

    static {
        for (int i = 1; i <= 10; i++) {
            final int n = i;
            final String machineName = n == 1 ? "gacha_machine" : "gacha_machine_" + n;
            final String coinName = n == 1 ? "gacha_coin" : "gacha_coin_" + n;
            final String tagName = n == 1 ? "currency_items" : "currency_items_" + n;

            RegistryObject<GachaMachineBlock> block = BLOCKS.register(machineName, () ->
                new GachaMachineBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F).noOcclusion(),
                    TagKey.create(Registries.ITEM, id(tagName)),
                    machineName));
            MACHINES.put(machineName, block);
            ITEMS.register(machineName, () -> new BlockItem(block.get(), new Item.Properties()));
            COINS.put(coinName, ITEMS.register(coinName, () -> new Item(new Item.Properties())));
        }

        for (char row = 'a'; row <= 'j'; row++) {
            for (int col = 1; col <= 10; col++) {
                String name = "capsule_" + row + col;
                CAPSULES.put(name, ITEMS.register(name, () -> new CapsuleItem(new Item.Properties().stacksTo(64))));
            }
        }

        MACHINE_BE = BLOCK_ENTITIES.register("gacha_machine", () ->
            BlockEntityType.Builder.of(
                GachaMachineBlockEntity::new,
                MACHINES.values().stream().map(RegistryObject::get).toArray(Block[]::new)
            ).build(null));
    }

    public GachaMachine() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
