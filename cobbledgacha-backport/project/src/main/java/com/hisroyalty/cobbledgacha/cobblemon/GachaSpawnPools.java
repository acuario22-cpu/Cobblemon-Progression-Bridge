package com.hisroyalty.cobbledgacha.cobblemon;

import com.google.gson.*;
import com.hisroyalty.cobbledgacha.config.DatapackConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.io.InputStreamReader;
import java.util.*;

public class GachaSpawnPools extends SimplePreparableReloadListener<Map<String,List<GachaSpawnPools.Entry>>> {
    public record Entry(String species,String bucket,double weight,int minLevel,int maxLevel){}
    private static volatile Map<String,List<Entry>> pools=Map.of();

    @Override protected Map<String,List<Entry>> prepare(ResourceManager manager, ProfilerFiller profiler){
        DatapackConfig.reload(manager);
        Map<String,List<Entry>> result=new HashMap<>();
        Map<ResourceLocation,Resource> resources=manager.listResources("spawn_pool_files",
            id->id.getNamespace().equals("cobbledgacha")&&id.getPath().endsWith(".json"));
        for(var file:resources.entrySet()){
            String p=file.getKey().getPath(), prefix="spawn_pool_files/";
            if(!p.startsWith(prefix))continue;
            String rest=p.substring(prefix.length()); int slash=rest.indexOf('/');
            if(slash<=0||rest.substring(slash+1).startsWith("_"))continue;
            String pool=rest.substring(0,slash).toLowerCase(Locale.ROOT);
            try(InputStreamReader r=new InputStreamReader(file.getValue().open())){
                JsonArray a=JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("spawns");
                if(a==null)continue;
                for(JsonElement el:a){
                    if(!el.isJsonObject())continue; JsonObject o=el.getAsJsonObject();
                    String species=str(o,"species",""); if(species.isBlank())continue;
                    String bucket=str(o,"bucket","common").toLowerCase(Locale.ROOT);
                    int min=num(o,"minLevel",1), max=num(o,"maxLevel",min); if(max<min)max=min;
                    result.computeIfAbsent(pool,k->new ArrayList<>()).add(new Entry(species,bucket,dbl(o,"weight",1),Math.max(1,min),Math.max(1,max)));
                }
            }catch(Exception ignored){}
        }
        Map<String,List<Entry>> frozen=new HashMap<>(); result.forEach((k,v)->frozen.put(k,List.copyOf(v))); return Map.copyOf(frozen);
    }
    @Override protected void apply(Map<String,List<Entry>> data,ResourceManager manager,ProfilerFiller profiler){pools=data;}

    public static Entry roll(String pool,Random random){
        List<Entry> all=pools.getOrDefault(pool.toLowerCase(Locale.ROOT),List.of()); if(all.isEmpty())return null;
        Map<String,List<Entry>> groups=new LinkedHashMap<>(); for(Entry e:all)groups.computeIfAbsent(e.bucket(),k->new ArrayList<>()).add(e);
        int total=groups.keySet().stream().mapToInt(DatapackConfig::bucketWeight).sum();
        List<Entry> selected=all;
        if(total>0){int r=random.nextInt(total); for(var e:groups.entrySet()){r-=DatapackConfig.bucketWeight(e.getKey()); if(r<0){selected=e.getValue();break;}}}
        double w=selected.stream().mapToDouble(Entry::weight).sum(); if(w<=0)return selected.get(random.nextInt(selected.size()));
        double r=random.nextDouble()*w; for(Entry e:selected){r-=e.weight(); if(r<=0)return e;} return selected.get(selected.size()-1);
    }
    private static String str(JsonObject o,String k,String d){try{return o.has(k)?o.get(k).getAsString():d;}catch(Exception e){return d;}}
    private static int num(JsonObject o,String k,int d){try{return o.has(k)?o.get(k).getAsInt():d;}catch(Exception e){return d;}}
    private static double dbl(JsonObject o,String k,double d){try{return o.has(k)?o.get(k).getAsDouble():d;}catch(Exception e){return d;}}
}
