package com.aqu.mountvisibility.mixin;

import com.aqu.mountvisibility.client.MountedPlayerHider;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class SmartCameraMixin {

    @ModifyArg(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"
            ),
            index = 0
    )
    private double mountvisibility$smartMountDistance(double originalDistance) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();

        if (!(cameraEntity instanceof Player player)) {
            return originalDistance;
        }

        Entity vehicle = player.getVehicle();
        if (!MountedPlayerHider.isCobblemon(vehicle)) {
            return originalDistance;
        }

        return Math.max(originalDistance, MountedPlayerHider.desiredCameraDistance(vehicle));
    }
}
