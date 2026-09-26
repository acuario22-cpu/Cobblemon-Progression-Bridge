package dev.acuario22.villagepokecenter.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * New plains/desert villages:
 * - First eligible street pool is replaced with the Generations PokéCenter street pool.
 * - Once the PokéCenter building pool is actually requested, the center is considered confirmed.
 * - PokéMart gets one independent 50% decision per village and is never forced twice.
 *
 * If the PokéCenter street cannot progress far enough to request its building pool,
 * subsequent eligible streets are retried, which makes this more robust than a single
 * one-shot random replacement.
 */
@Mixin(value = JigsawPlacement.Placer.class, priority = 900)
public abstract class VillagePokeCenterMixin {
    @Shadow @Final
    private RandomSource random;

    @Unique
    private boolean vpcg$centerConfirmed = false;

    @Unique
    private boolean vpcg$martDecisionMade = false;

    @Unique
    private boolean vpcg$wantMart = false;

    @Unique
    private boolean vpcg$martAttempted = false;

    @Unique
    private String vpcg$villageKind = null;

    @ModifyArg(
            method = "tryPlacingChildren",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/Registry;getHolder(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;"
            ),
            index = 0
    )
    private ResourceKey<StructureTemplatePool> vpcg$forceVillageSpecialPool(
            ResourceKey<StructureTemplatePool> key
    ) {
        ResourceLocation id = key.location();
        String namespace = id.getNamespace();
        String path = id.getPath();

        // The Generations special street successfully reached the PokéCenter building pool.
        if ("generations_structures".equals(namespace)) {
            if ("village/plains/pokecenter".equals(path)) {
                vpcg$confirmCenter("plains");
                return key;
            }
            if ("village/desert/pokecenter".equals(path)) {
                vpcg$confirmCenter("desert");
                return key;
            }

            // If a PokéMart pool is already being requested, do not inject another one.
            if ("village/plains/pokemart".equals(path) || "village/desert/pokemart".equals(path)) {
                vpcg$martAttempted = true;
                return key;
            }
        }

        String kind = vpcg$vanillaVillageKind(namespace, path);
        if (kind == null) {
            return key;
        }

        if (vpcg$villageKind == null) {
            vpcg$villageKind = kind;
        }

        // Keep retrying eligible street branches until the PokéCenter pool is actually reached.
        if (!vpcg$centerConfirmed) {
            return vpcg$specialStreetKey(kind, "pokecenter");
        }

        // PokéMart is optional: exactly one 50% decision for this village.
        if (!vpcg$martDecisionMade) {
            vpcg$martDecisionMade = true;
            vpcg$wantMart = random.nextBoolean();
        }

        if (vpcg$wantMart && !vpcg$martAttempted) {
            vpcg$martAttempted = true;
            return vpcg$specialStreetKey(kind, "pokemart");
        }

        return key;
    }

    @Unique
    private void vpcg$confirmCenter(String kind) {
        vpcg$centerConfirmed = true;
        vpcg$villageKind = kind;
        if (!vpcg$martDecisionMade) {
            vpcg$martDecisionMade = true;
            vpcg$wantMart = random.nextBoolean();
        }
    }

    @Unique
    private static String vpcg$vanillaVillageKind(String namespace, String path) {
        if (!"minecraft".equals(namespace)) {
            return null;
        }
        if ("village/plains/streets".equals(path)) {
            return "plains";
        }
        if ("village/desert/streets".equals(path)) {
            return "desert";
        }
        return null;
    }

    @Unique
    private static ResourceKey<StructureTemplatePool> vpcg$specialStreetKey(String kind, String building) {
        return ResourceKey.create(
                Registries.TEMPLATE_POOL,
                new ResourceLocation(
                        "generations_structures",
                        "village/" + kind + "/streets/" + building
                )
        );
    }
}
