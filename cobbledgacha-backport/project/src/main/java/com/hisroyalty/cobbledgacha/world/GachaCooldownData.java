package com.hisroyalty.cobbledgacha.world;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public class GachaCooldownData extends SavedData {
    private static final String NAME="cobbledgacha_cooldowns";
    private record Entry(long until,int uses){}
    private final Map<String,Entry> entries=new HashMap<>();
    public static GachaCooldownData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(GachaCooldownData::load,GachaCooldownData::new,NAME);}
    public long remaining(ServerLevel level,String machine,UUID id){Entry e=entries.get(machine+"|"+id);return e==null?0:Math.max(0,e.until-level.getGameTime());}
    public void recordUse(ServerLevel level,String machine,UUID id,int limit,int ticks){
        String k=machine+"|"+id; Entry old=entries.get(k); int uses=(old==null?0:old.uses)+1;
        entries.put(k,ticks>0&&uses>=Math.max(1,limit)?new Entry(level.getGameTime()+ticks,0):new Entry(0,uses)); setDirty();
    }
    public static GachaCooldownData load(CompoundTag tag){
        GachaCooldownData d=new GachaCooldownData(); ListTag l=tag.getList("Entries",Tag.TAG_COMPOUND);
        for(int i=0;i<l.size();i++){CompoundTag e=l.getCompound(i);d.entries.put(e.getString("Key"),new Entry(e.getLong("Until"),e.getInt("Uses")));} return d;
    }
    @Override public CompoundTag save(CompoundTag tag){
        ListTag l=new ListTag(); entries.forEach((k,v)->{CompoundTag e=new CompoundTag();e.putString("Key",k);e.putLong("Until",v.until);e.putInt("Uses",v.uses);l.add(e);});tag.put("Entries",l);return tag;
    }
}
