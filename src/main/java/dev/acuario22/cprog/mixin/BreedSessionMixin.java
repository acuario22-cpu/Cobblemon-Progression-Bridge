package dev.acuario22.cprog.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.acuario22.cprog.ScoreboardProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Pseudo
@Mixin(targets = "dev.thomasqtruong.veryscuffedcobblemonbreeding.commands.PokeBreed$BreedSession", remap = false)
public abstract class BreedSessionMixin {
    @Shadow public ServerPlayer breeder;
    @Shadow public Pokemon breederPokemon1;
    @Shadow public Pokemon breederPokemon2;
    @Unique private Set<UUID> cprog$before = Set.of();

    @Inject(method = "doBreed", at = @At("HEAD"), remap = false)
    private void cprog$before(CallbackInfo ci) {
        cprog$before = new HashSet<>();
        if (breeder == null) return;
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(breeder);
        for (Pokemon p : party) cprog$before.add(p.getUuid());
    }

    @Inject(method = "doBreed", at = @At("RETURN"), remap = false)
    private void cprog$after(CallbackInfo ci) {
        if (breeder == null || breederPokemon1 == null || breederPokemon2 == null) return;
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(breeder);
        Pokemon baby = null;
        for (Pokemon p : party) if (!cprog$before.contains(p.getUuid())) { baby = p; break; }
        if (baby == null) return;

        ScoreboardProgress.add(breeder, "cprog_breed_total", 1);
        String s1 = breederPokemon1.getSpecies().getResourceIdentifier().getPath();
        String s2 = breederPokemon2.getSpecies().getResourceIdentifier().getPath();
        if ("ditto".equals(s1) || "ditto".equals(s2)) ScoreboardProgress.add(breeder, "cprog_breed_ditto", 1);

        String i1 = itemPath(breederPokemon1.heldItem());
        String i2 = itemPath(breederPokemon2.heldItem());
        if ("destiny_knot".equals(i1) || "destiny_knot".equals(i2)) ScoreboardProgress.add(breeder, "cprog_breed_destiny", 1);
        if ("everstone".equals(i1) || "everstone".equals(i2)) ScoreboardProgress.add(breeder, "cprog_breed_everstone", 1);
        if (isPowerItem(i1) || isPowerItem(i2)) ScoreboardProgress.add(breeder, "cprog_breed_power", 1);

        int perfect = perfectIvCount(baby);
        if (perfect >= 4) ScoreboardProgress.add(breeder, "cprog_breed_4iv", 1);
        if (perfect >= 5) ScoreboardProgress.add(breeder, "cprog_breed_5iv", 1);
        if (perfect >= 6) ScoreboardProgress.add(breeder, "cprog_breed_6iv", 1);
        if (baby.getShiny()) ScoreboardProgress.add(breeder, "cprog_breed_shiny", 1);
        if (hasHiddenAbility(baby)) ScoreboardProgress.add(breeder, "cprog_breed_hidden", 1);
    }

    @Unique private static String itemPath(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }
    @Unique private static boolean isPowerItem(String id) {
        return id.equals("power_anklet") || id.equals("power_band") || id.equals("power_belt") ||
               id.equals("power_bracer") || id.equals("power_lens") || id.equals("power_weight");
    }
    @Unique private static int perfectIvCount(Pokemon p) {
        int n = 0;
        Stats[] stats = {Stats.HP,Stats.ATTACK,Stats.DEFENCE,Stats.SPECIAL_ATTACK,Stats.SPECIAL_DEFENCE,Stats.SPEED};
        for (Stats stat : stats) if (p.getIvs().getOrDefault(stat) == 31) n++;
        return n;
    }
    @Unique private static boolean hasHiddenAbility(Pokemon p) {
        for (PotentialAbility a : p.getForm().getAbilities()) {
            if (a.getPriority() == Priority.LOW && a.getTemplate().equals(p.getAbility().getTemplate())) return true;
        }
        return false;
    }
}
