package com.hisroyalty.cobbledgacha.item;
import com.hisroyalty.cobbledgacha.cobblemon.PokemonCommandSpawner;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class PokemonSpawnCapsuleItem extends Item {
    private final String pool;
    public PokemonSpawnCapsuleItem(String pool,Properties p){super(p);this.pool=pool;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack held=player.getItemInHand(hand); if(level.isClientSide||!(player instanceof ServerPlayer sp))return InteractionResultHolder.success(held);
        boolean ok=PokemonCommandSpawner.spawn((ServerLevel)level,player.blockPosition(),sp,true,pool);
        if(ok&&!player.getAbilities().instabuild)held.shrink(1);
        level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.SNIFFER_EGG_PLOP,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        return ok?InteractionResultHolder.consume(held):InteractionResultHolder.fail(held);
    }
}
