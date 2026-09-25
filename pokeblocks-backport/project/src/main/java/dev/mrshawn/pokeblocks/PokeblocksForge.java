package dev.mrshawn.pokeblocks;

import dev.mrshawn.pokeblocks.block.DollBlock;
import dev.mrshawn.pokeblocks.block.DollBlockEntity;
import dev.mrshawn.pokeblocks.client.DollBlockRenderer;
import dev.mrshawn.pokeblocks.item.DollBlockItem;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(PokeblocksForge.MOD_ID)
public class PokeblocksForge {
    public static final String MOD_ID = "pokeblocks";

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final Map<String, RegistryObject<DollBlock>> DOLLS = new LinkedHashMap<>();
    public static final RegistryObject<BlockEntityType<DollBlockEntity>> DOLL_BE;

    static {
        for (GeneratedDolls.Def def : GeneratedDolls.ALL) {
            RegistryObject<DollBlock> block = BLOCKS.register(
                def.id(),
                () -> new DollBlock(
                    BlockBehaviour.Properties.of()
                        .strength(0.4F)
                        .sound(SoundType.WOOL)
                        .noOcclusion(),
                    def.id(),
                    def.gigantic()
                )
            );

            DOLLS.put(def.id(), block);
            ITEMS.register(
                def.id(),
                () -> new DollBlockItem(block.get(), new Item.Properties(), def.id())
            );
        }

        DOLL_BE = BLOCK_ENTITIES.register(
            "pokedoll",
            () -> BlockEntityType.Builder.of(
                DollBlockEntity::new,
                DOLLS.values().stream().map(RegistryObject::get).toArray(Block[]::new)
            ).build(null)
        );

        TABS.register(
            "pokedolls",
            () -> CreativeModeTab.builder()
                .title(Component.literal("Pokeblocks"))
                .icon(() -> DOLLS.isEmpty()
                    ? ItemStack.EMPTY
                    : new ItemStack(DOLLS.values().iterator().next().get()))
                .displayItems((params, output) ->
                    DOLLS.values().forEach(value -> output.accept(value.get())))
                .build()
        );
    }

    public PokeblocksForge() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() ->
                DOLLS.values().forEach(doll ->
                    ItemBlockRenderTypes.setRenderLayer(doll.get(), RenderType.cutout()))
            );
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(DOLL_BE.get(), DollBlockRenderer::new);
        }
    }
}
