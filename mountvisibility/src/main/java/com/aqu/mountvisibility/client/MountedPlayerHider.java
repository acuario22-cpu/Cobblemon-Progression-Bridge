package com.aqu.mountvisibility.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mountvisibility", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MountedPlayerHider {
    private static boolean wasRidingCobblemon;
    private static CameraType cameraBeforeMount;

    private MountedPlayerHider() {
    }

    @SubscribeEvent
    public static void beforePlayerRender(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        Entity vehicle = player.getVehicle();

        if (isCobblemon(vehicle)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void beforeLivingRender(RenderLivingEvent.Pre<?, ?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player localPlayer = minecraft.player;
        if (localPlayer == null) {
            return;
        }

        Entity vehicle = localPlayer.getVehicle();
        if (vehicle == null || event.getEntity() != vehicle || !isCobblemon(vehicle)) {
            return;
        }

        // Normally SmartCameraMixin keeps the camera far enough away.
        // If a wall or other collision forces it back inside/too close to the
        // mounted model, hide only the local mount for that frame instead of
        // letting the model cover the entire screen.
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 rider = localPlayer.position().add(0.0D, localPlayer.getEyeHeight() * 0.5D, 0.0D);
        double distance = camera.distanceTo(rider);

        double size = Math.max(vehicle.getBbWidth(), vehicle.getBbHeight());
        double hideDistance = Math.max(1.75D, Math.min(3.75D, 1.25D + size * 0.55D));

        if (minecraft.options.getCameraType() != CameraType.FIRST_PERSON && distance < hideDistance) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        boolean ridingCobblemon = isCobblemon(minecraft.player.getVehicle());

        if (ridingCobblemon && !wasRidingCobblemon) {
            cameraBeforeMount = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!ridingCobblemon && wasRidingCobblemon && cameraBeforeMount != null) {
            minecraft.options.setCameraType(cameraBeforeMount);
            cameraBeforeMount = null;
        }

        wasRidingCobblemon = ridingCobblemon;
    }

    public static boolean isCobblemon(Entity entity) {
        if (entity == null) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && "cobblemon".equals(id.getNamespace());
    }

    public static double desiredCameraDistance(Entity vehicle) {
        if (!isCobblemon(vehicle)) {
            return 4.0D;
        }

        // HD packs frequently make the visual model much larger than vanilla's
        // default third-person distance. Scale camera distance by the entity's
        // physical dimensions while keeping sane limits.
        double size = Math.max(vehicle.getBbWidth(), vehicle.getBbHeight());
        double distance = 4.0D + size * 1.35D;
        return Math.max(5.0D, Math.min(12.0D, distance));
    }
}
