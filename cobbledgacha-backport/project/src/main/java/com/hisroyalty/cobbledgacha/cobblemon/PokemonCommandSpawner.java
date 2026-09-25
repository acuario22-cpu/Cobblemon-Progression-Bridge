package com.hisroyalty.cobbledgacha.cobblemon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import java.util.Random;

public final class PokemonCommandSpawner {
    private PokemonCommandSpawner(){}
    public static boolean spawn(ServerLevel level,BlockPos pos,ServerPlayer player,boolean give,String pool){
        var e=GachaSpawnPools.roll(pool,new Random(level.random.nextLong())); if(e==null)return false;
        int lvl=e.minLevel(); if(e.maxLevel()>e.minLevel())lvl+=level.random.nextInt(e.maxLevel()-e.minLevel()+1);
        String props=e.species()+" level="+lvl;
        try{
            var source=level.getServer().createCommandSourceStack().withPermission(4).withLevel(level).withPosition(Vec3.atCenterOf(pos));
            int result=give&&player!=null
                ? level.getServer().getCommands().performPrefixedCommand(source,"givepokemonother "+player.getGameProfile().getName()+" "+props)
                : level.getServer().getCommands().performPrefixedCommand(source,"spawnpokemonat "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+props);
            return result>0;
        }catch(Exception ex){return false;}
    }
}
