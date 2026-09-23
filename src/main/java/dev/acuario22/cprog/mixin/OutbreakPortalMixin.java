package dev.acuario22.cprog.mixin;

import dev.acuario22.cprog.ScoreboardProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Pseudo
@Mixin(targets = "com.scouter.cobbleoutbreaks.entity.OutbreakPortalEntity", remap = false)
public abstract class OutbreakPortalMixin {
    @Shadow private UUID ownerUUID;
    @Shadow private Level level;
    @Inject(method = "completeOutBreak", at = @At("HEAD"), remap = false)
    private void cprog$complete(boolean rewards, CallbackInfo ci) {
        if (!rewards || ownerUUID == null || level == null || level.isClientSide() || level.getServer() == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerUUID);
        if (player != null) ScoreboardProgress.add(player, "cprog_outbreak_complete", 1);
    }
}
