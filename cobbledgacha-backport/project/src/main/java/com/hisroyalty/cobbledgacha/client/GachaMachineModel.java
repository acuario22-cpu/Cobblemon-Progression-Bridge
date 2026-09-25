package com.hisroyalty.cobbledgacha.client;
import com.hisroyalty.cobbledgacha.CobbledGacha;
import com.hisroyalty.cobbledgacha.block.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GachaMachineModel extends GeoModel<GachaMachineBlockEntity>{
    private String file(String pattern,GachaMachineBlockEntity a){
        String key=a.getBlockState().getBlock() instanceof GachaMachineBlock b?b.getLootKey():"gacha_machine";
        if(key.equals("gacha_machine"))return pattern.replace("_%d","");
        int n=1;try{n=Integer.parseInt(key.substring("gacha_machine_".length()));}catch(Exception ignored){}
        return pattern.formatted(n);
    }
    @Override public ResourceLocation getModelResource(GachaMachineBlockEntity a){return CobbledGacha.id(file("geo/gacha_machine_%d.geo.json",a));}
    @Override public ResourceLocation getTextureResource(GachaMachineBlockEntity a){return CobbledGacha.id(file("textures/block/gacha_machine_%d.png",a));}
    @Override public ResourceLocation getAnimationResource(GachaMachineBlockEntity a){return CobbledGacha.id(file("animations/gacha_machine_%d.animation.json",a));}
    @Override public RenderType getRenderType(GachaMachineBlockEntity a,ResourceLocation t){return RenderType.entityCutout(t);}
}
