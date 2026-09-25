package com.hisroyalty.cobbledgacha.config;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.InputStreamReader;
import java.util.*;

public final class DatapackConfig {
    private static final Map<String,Integer> LIMITS=new HashMap<>(), COOLDOWNS=new HashMap<>(), USES=new HashMap<>(), BUCKETS=new HashMap<>();
    private static final Map<String,MachineType> TYPES=new HashMap<>();
    private static boolean pickup, automation;
    private DatapackConfig(){}

    public static void reload(ResourceManager manager) {
        LIMITS.clear(); COOLDOWNS.clear(); USES.clear(); TYPES.clear(); BUCKETS.clear(); pickup=false; automation=false;
        manager.getResource(new ResourceLocation("cobbledgacha","config/server_config.json")).ifPresent(res -> {
            try (InputStreamReader r=new InputStreamReader(res.open())) {
                JsonObject root=JsonParser.parseReader(r).getAsJsonObject();
                pickup=bool(root,"pickup",false); automation=bool(root,"automation",false);
                for(int i=1;i<=20;i++){
                    String key="gacha_machine_"+i;
                    if(root.has(key)&&root.get(key).isJsonPrimitive()) LIMITS.put(key,Math.max(1,root.get(key).getAsInt()));
                    String tk=key+"_type";
                    if(root.has(tk)&&root.get(tk).isJsonPrimitive()) TYPES.put(key,MachineType.parse(root.get(tk).getAsString()));
                }
                readMap(root,"cooldowns",COOLDOWNS); readMap(root,"usesBeforeCooldown",USES); readMap(root,"buckets",BUCKETS);
            } catch(Exception ignored){}
        });
        LIMITS.putIfAbsent("gacha_machine_1",5); LIMITS.putIfAbsent("gacha_machine_2",20); LIMITS.putIfAbsent("gacha_machine_3",20);
        LIMITS.putIfAbsent("gacha_machine_4",5); LIMITS.putIfAbsent("gacha_machine_11",1); LIMITS.putIfAbsent("gacha_machine_12",3);
        TYPES.putIfAbsent("gacha_machine_4",MachineType.SPAWNER); TYPES.putIfAbsent("gacha_machine_12",MachineType.SPECIFIC);
        BUCKETS.putIfAbsent("common",100); BUCKETS.putIfAbsent("uncommon",40); BUCKETS.putIfAbsent("rare",15);
        BUCKETS.putIfAbsent("ultra_rare",5); BUCKETS.putIfAbsent("legendary",1);
    }
    private static boolean bool(JsonObject o,String k,boolean d){ try{return o.has(k)?o.get(k).getAsBoolean():d;}catch(Exception e){return d;} }
    private static void readMap(JsonObject root,String key,Map<String,Integer> out){
        if(!root.has(key)||!root.get(key).isJsonObject())return;
        for(var e:root.getAsJsonObject(key).entrySet())try{out.put(e.getKey(),Math.max(0,e.getValue().getAsInt()));}catch(Exception ignored){}
    }
    public static int maxCurrency(String k){return LIMITS.getOrDefault(k,5);}
    public static int cooldownSeconds(String k){return COOLDOWNS.getOrDefault(k,0);}
    public static int usesBeforeCooldown(String k){return Math.max(1,USES.getOrDefault(k,1));}
    public static MachineType type(String k){return TYPES.getOrDefault(k,MachineType.GENERIC);}
    public static boolean pickup(){return pickup;}
    public static boolean automation(){return automation;}
    public static int bucketWeight(String k){return Math.max(0,BUCKETS.getOrDefault(k.toLowerCase(Locale.ROOT),1));}
}
