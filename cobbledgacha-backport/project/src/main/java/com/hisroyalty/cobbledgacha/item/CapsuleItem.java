package com.hisroyalty.cobbledgacha.item;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.registries.ForgeRegistries;

public class CapsuleItem extends Item {
    public CapsuleItem(Properties p){super(p);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack held=player.getItemInHand(hand); if(level.isClientSide)return InteractionResultHolder.success(held);
        ResourceLocation id=ForgeRegistries.ITEMS.getKey(this); if(id==null)return InteractionResultHolder.fail(held);
        ServerLevel sl=(ServerLevel)level;
        LootTable table=sl.getServer().getLootData().getLootTable(new ResourceLocation(id.getNamespace(),"gacha_capsules/"+id.getPath()));
        LootParams params=new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN,player.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.CHEST);
        var rewards=table.getRandomItems(params);
        if(rewards.isEmpty())player.displayClientMessage(Component.translatable("message.capsule.empty_loot_table").withStyle(ChatFormatting.RED),true);
        else for(ItemStack s:rewards)if(!player.addItem(s))player.drop(s,false);
        if(!player.getAbilities().instabuild)held.shrink(1);
        level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.SNIFFER_EGG_PLOP,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        return InteractionResultHolder.consume(held);
    }
}
