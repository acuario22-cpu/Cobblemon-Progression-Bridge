package dev.acuario22.villagepokecenter;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Field;

@Mod(VillagePokeCenterGuarantee.MOD_ID)
public final class VillagePokeCenterGuarantee {
    public static final String MOD_ID = "village_pokecenter_guarantee";
    private static final Logger LOGGER = LogUtils.getLogger();

    public VillagePokeCenterGuarantee() {
        disableGenerationsRandomVillageInjection();
    }

    /**
     * Generations Structures 1.0.0 already has its own 50/50 village injector.
     * This addon takes over that job so we can guarantee exactly one PokéCenter
     * attempt per new plains/desert village and keep the PokéMart optional.
     */
    private static void disableGenerationsRandomVillageInjection() {
        try {
            Class<?> generationsStructures = Class.forName(
                    "generations.gg.generations.structures.generationsstructures.GenerationsStructures"
            );

            Field configField = generationsStructures.getField("CONFIG");
            Object config = configField.get(null);

            Field villageSectionField = config.getClass().getField("villageStructureGeneration");
            Object villageSection = villageSectionField.get(config);

            Field allowField = villageSection.getClass().getField("AllowStructuresInVillages");
            allowField.setBoolean(villageSection, false);

            LOGGER.info("[Village PokeCenter Guarantee] Disabled Generations Structures random village injector; deterministic injector enabled.");
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[Village PokeCenter Guarantee] Could not disable the original Generations Structures village injector. The guarantee mixin will still run, but duplicate special buildings may be possible.", e);
        }
    }
}
