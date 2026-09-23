package dev.acuario22.cprog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.Collections;
public final class ProgressionServerLevels {
    private ProgressionServerLevels() {}
    public static Iterable<ServerLevel> levels() {
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        return s == null ? Collections.emptyList() : s.getAllLevels();
    }
}
