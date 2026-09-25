package com.hisroyalty.cobbledgacha.client;

import com.hisroyalty.cobbledgacha.CobbledGacha;
import com.hisroyalty.cobbledgacha.item.GeoMachineBlockItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;

public class MachineItemModel extends GeoModel<GeoMachineBlockItem> {
    private String path(GeoMachineBlockItem item) {
        var id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? "gacha_machine" : id.getPath();
    }

    @Override
    public ResourceLocation getModelResource(GeoMachineBlockItem item) {
        return CobbledGacha.id("geo/" + path(item) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoMachineBlockItem item) {
        return CobbledGacha.id("textures/block/" + path(item) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeoMachineBlockItem item) {
        return CobbledGacha.id("animations/" + path(item) + ".animation.json");
    }
}
