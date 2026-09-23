package dev.acuario22.cprog;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.util.List;

public final class ScoreboardProgress {
    private ScoreboardProgress() {}
    public static final List<String> OBJECTIVES = List.of(
        "cprog_outbreak_complete","cprog_outbreak_capture","cprog_outbreak_defeat","cprog_outbreak_shiny",
        "cprog_bloodmoon_seen","cprog_bloodmoon_complete","cprog_bloodmoon_capture","cprog_bloodmoon_defeat","cprog_bloodmoon_shiny",
        "cprog_breed_total","cprog_breed_ditto","cprog_breed_destiny","cprog_breed_everstone","cprog_breed_power",
        "cprog_breed_4iv","cprog_breed_5iv","cprog_breed_6iv","cprog_breed_hidden","cprog_breed_shiny",
        "cprog_spring_capture","cprog_summer_capture","cprog_autumn_capture","cprog_winter_capture",
        "cprog_bop_biomes","cprog_ru_biomes","cprog_legendary_unique"
    );
    public static void ensureAll(ServerPlayer player) { for (String id : OBJECTIVES) objective(player.getScoreboard(), id); }
    public static void add(ServerPlayer player, String objective, int amount) {
        if (player == null || amount == 0) return;
        Scoreboard sb = player.getScoreboard();
        Objective obj = objective(sb, objective);
        sb.getOrCreatePlayerScore(player.getScoreboardName(), obj).add(amount);
    }
    public static int get(ServerPlayer player, String objective) {
        Scoreboard sb = player.getScoreboard();
        Objective obj = objective(sb, objective);
        return sb.getOrCreatePlayerScore(player.getScoreboardName(), obj).getScore();
    }
    private static Objective objective(Scoreboard sb, String name) {
        Objective existing = sb.getObjective(name);
        if (existing != null) return existing;
        return sb.addObjective(name, ObjectiveCriteria.DUMMY, Component.literal(name), ObjectiveCriteria.RenderType.INTEGER);
    }
}
