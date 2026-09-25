package com.hisroyalty.cobbledgacha;

import com.hisroyalty.cobbledgacha.block.*;
import com.hisroyalty.cobbledgacha.client.GachaMachineRenderer;
import com.hisroyalty.cobbledgacha.cobblemon.GachaSpawnPools;
import com.hisroyalty.cobbledgacha.item.*;
import com.hisroyalty.cobbledgacha.loot.RewardTableManager;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod(CobbledGacha.MOD_ID)
public class CobbledGacha {
    public static final String MOD_ID="cobbledgacha";
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,MOD_ID);
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MOD_ID);
    public static final Map<String,RegistryObject<GachaMachineBlock>> MACHINES=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> COINS=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> EXTRA_ITEMS=new LinkedHashMap<>();
    public static final List<RegistryObject<Item>> CAPSULES=new ArrayList<>();
    public static final int USEFUL_CAPSULE_COUNT=5;
    public static final RegistryObject<BlockEntityType<GachaMachineBlockEntity>> GACHA_MACHINE_BE;

    static {
        for(int i=1;i<=12;i++){
            final int tier=i;
            String name=i==1?"gacha_machine":"gacha_machine_"+i;
            String tag=i==1?"currency_items":"currency_items_"+i;
            RegistryObject<GachaMachineBlock> block=BLOCKS.register(name,()->new GachaMachineBlock(
                BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(.5f).noOcclusion().lightLevel(s->tier==4?12:0),
                TagKey.create(Registries.ITEM,id(tag)),name,tier));
            MACHINES.put(name,block);
            ITEMS.register(name,()->new GeoMachineBlockItem(block.get(),new Item.Properties()));
        }

        for(int i=1;i<=10;i++){
            String n=i==1?"gacha_coin":"gacha_coin_"+i;
            COINS.put(n,ITEMS.register(n,()->new Item(new Item.Properties())));
        }

        EXTRA_ITEMS.put("koban_coin",ITEMS.register("koban_coin",()->new KobanCoinItem(new Item.Properties())));
        EXTRA_ITEMS.put("rocket_ball",ITEMS.register("rocket_ball",()->new PokemonSpawnCapsuleItem("rocket_ball",new Item.Properties().stacksTo(16))));
        for(int i=1;i<=6;i++){
            String n="gacha_ball_"+i;
            EXTRA_ITEMS.put(n,ITEMS.register(n,()->new PokemonSpawnCapsuleItem(n,new Item.Properties().stacksTo(16))));
        }

        for(String n:new String[]{
            "creepy_yarn","fantasy_yarn","feathery_yarn","fiery_yarn","frosty_yarn","grassy_yarn",
            "hardy_yarn","plain_yarn","soggy_yarn","sparky_yarn","toothy_yarn"
        }) {
            EXTRA_ITEMS.put(n,ITEMS.register(n,()->new Item(new Item.Properties())));
        }

        for(char row='a';row<='j';row++){
            for(int col=1;col<=10;col++){
                String n="capsule_"+row+col;
                CAPSULES.add(ITEMS.register(n,()->new CapsuleItem(new Item.Properties())));
            }
        }

        GACHA_MACHINE_BE=BLOCK_ENTITIES.register("gacha_animated",()->BlockEntityType.Builder.of(
            GachaMachineBlockEntity::new,
            MACHINES.values().stream().map(RegistryObject::get).toArray(Block[]::new)
        ).build(null));

        TABS.register("cobbled_gacha",()->CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cobbled_gacha"))
            .icon(()->new ItemStack(MACHINES.get("gacha_machine").get()))
            .displayItems((params,out)->{
                MACHINES.values().forEach(v->out.accept(v.get()));
                COINS.values().forEach(v->out.accept(v.get()));
                EXTRA_ITEMS.values().forEach(v->out.accept(v.get()));
                for(int i=0;i<Math.min(USEFUL_CAPSULE_COUNT,CAPSULES.size());i++){
                    out.accept(CAPSULES.get(i).get());
                }
            }).build());
    }

    public CobbledGacha(){
        IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void addReloadListeners(AddReloadListenerEvent e){
        e.addListener(new GachaSpawnPools());
        e.addListener(new RewardTableManager());
    }

    public static ResourceLocation id(String path){
        return new ResourceLocation(MOD_ID,path);
    }

    @Mod.EventBusSubscriber(modid=MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static class ClientEvents{
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent e){
            e.enqueueWork(()->MACHINES.values().forEach(m->
                ItemBlockRenderTypes.setRenderLayer(m.get(), RenderType.translucent())));
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e){
            e.registerBlockEntityRenderer(GACHA_MACHINE_BE.get(),GachaMachineRenderer::new);
        }
    }
}
