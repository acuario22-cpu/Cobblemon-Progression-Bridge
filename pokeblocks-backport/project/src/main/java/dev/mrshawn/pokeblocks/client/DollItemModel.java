package dev.mrshawn.pokeblocks.client;

import dev.mrshawn.pokeblocks.GeneratedDolls;
import dev.mrshawn.pokeblocks.PokeblocksForge;
import dev.mrshawn.pokeblocks.item.DollBlockItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DollItemModel extends GeoModel<DollBlockItem> {
    private GeneratedDolls.Def def(DollBlockItem animatable) {
        return GeneratedDolls.BY_ID.getOrDefault(
            animatable.dollId(),
            GeneratedDolls.ALL.get(0)
        );
    }

    @Override
    public ResourceLocation getModelResource(DollBlockItem animatable) {
        return PokeblocksForge.id("geo/" + def(animatable).geo() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DollBlockItem animatable) {
        return PokeblocksForge.id("textures/block/" + def(animatable).texture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(DollBlockItem animatable) {
        return PokeblocksForge.id("animations/generic.animation.json");
    }

    @Override
    public RenderType getRenderType(DollBlockItem animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
