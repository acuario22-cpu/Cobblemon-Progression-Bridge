package dev.zanckor.cobblemonrider;

import com.google.common.collect.HashBasedTable;
import dev.zanckor.cobblemonrider.config.PokemonJsonObject;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;

public class MCUtil {
    /**
     * O(1) lookup table rebuilt only when the active PokemonJsonObject instance changes.
     * Stock 1.2.4 scans every configured Pokemon on first lookup of each species/form.
     */
    private static final HashBasedTable<String, String, PokemonJsonObject.PokemonConfigData> cachedConfig =
            HashBasedTable.create();

    private static PokemonJsonObject cachedConfigSource;

    public static Entity getEntityLookinAt(Entity rayTraceEntity, double distance) {
        float playerRotX = rayTraceEntity.getXRot();
        float playerRotY = rayTraceEntity.getYRot();
        Vec3 startPos = rayTraceEntity.getEyePosition();
        float f2 = Mth.cos(-playerRotY * ((float) Math.PI / 180F) - (float) Math.PI);
        float f3 = Mth.sin(-playerRotY * ((float) Math.PI / 180F) - (float) Math.PI);
        float f4 = -Mth.cos(-playerRotX * ((float) Math.PI / 180F));
        float additionY = Mth.sin(-playerRotX * ((float) Math.PI / 180F));
        float additionX = f3 * f4;
        float additionZ = f2 * f4;
        double d0 = distance;
        Vec3 endVec = startPos.add(((double) additionX * d0), ((double) additionY * d0), ((double) additionZ * d0));

        AABB startEndBox = new AABB(startPos, endVec);
        Entity entity = null;
        for (Entity entity1 : rayTraceEntity.level().getEntities(rayTraceEntity, startEndBox, (val) -> true)) {
            AABB aabb = entity1.getBoundingBox().inflate(entity1.getPickRadius());
            Optional<Vec3> optional = aabb.clip(startPos, endVec);
            if (aabb.contains(startPos)) {
                if (d0 >= 0.0D) {
                    entity = entity1;
                    startPos = optional.orElse(startPos);
                    d0 = 0.0D;
                }
            } else if (optional.isPresent()) {
                Vec3 vec31 = optional.get();
                double d1 = startPos.distanceToSqr(vec31);
                if (d1 < d0 || d0 == 0.0D) {
                    if (entity1.getRootVehicle() == rayTraceEntity.getRootVehicle() && !entity1.canRiderInteract()) {
                        if (d0 == 0.0D) {
                            entity = entity1;
                            startPos = vec31;
                        }
                    } else {
                        entity = entity1;
                        startPos = vec31;
                        d0 = d1;
                    }
                }
            }
        }

        return entity;
    }

    public static synchronized PokemonJsonObject.PokemonConfigData getPassengerObject(
            String pokemonType,
            String formName
    ) {
        PokemonJsonObject pokemonJsonObject = CobblemonRider.pokemonJsonObject;
        if (pokemonJsonObject == null || pokemonType == null) {
            return null;
        }

        ensureCache(pokemonJsonObject);

        String pokemonKey = normalize(pokemonType);
        String formKey = normalizeForm(formName);
        return cachedConfig.get(pokemonKey, formKey);
    }

    public static synchronized void clearCachedConfig() {
        cachedConfig.clear();
        cachedConfigSource = null;
    }

    private static void ensureCache(PokemonJsonObject pokemonJsonObject) {
        if (cachedConfigSource == pokemonJsonObject) {
            return;
        }

        cachedConfig.clear();

        for (String translationKey : pokemonJsonObject.getPokemonIDs()) {
            PokemonJsonObject.PokemonConfigData data = pokemonJsonObject.getPokemonData(translationKey);
            if (data == null) {
                continue;
            }

            cachedConfig.put(
                    normalize(translationKey),
                    normalizeForm(data.getFormName()),
                    data
            );
        }

        cachedConfigSource = pokemonJsonObject;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeForm(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()
                || normalized.equals("normal")
                || normalized.equals("base")
                || normalized.equals("default")) {
            return "none";
        }
        return normalized;
    }

    public static Vec3 clampVec3(Vec3 vec3, double min, double max) {
        double x = Mth.clamp(vec3.x, min, max);
        double z = Mth.clamp(vec3.z, min, max);

        return new Vec3(x, vec3.y, z);
    }
}
