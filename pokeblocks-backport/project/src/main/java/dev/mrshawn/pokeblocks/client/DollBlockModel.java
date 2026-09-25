package dev.mrshawn.pokeblocks.client;

import dev.mrshawn.pokeblocks.GeneratedDolls;
import dev.mrshawn.pokeblocks.PokeblocksForge;
import dev.mrshawn.pokeblocks.block.DollBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DollBlockModel extends GeoModel<DollBlockEntity> {
    private GeneratedDolls.Def def(DollBlockEntity animatable) {
        return GeneratedDolls.BY_ID.getOrDefault(
            animatable.dollId(),
            GeneratedDolls.ALL.get(0)
        );
    }

    @Override
    public ResourceLocation getModelResource(DollBlockEntity animatable) {
        return PokeblocksForge.id("geo/" + def(animatable).geo() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DollBlockEntity animatable) {
        return PokeblocksForge.id("textures/block/" + def(animatable).texture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(DollBlockEntity animatable) {
        return PokeblocksForge.id("animations/generic.animation.json");
    }

    @Override
    public RenderType getRenderType(DollBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
