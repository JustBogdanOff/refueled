package bogdan.refueled.common.events;

import bogdan.refueled.config.ClientConfig;
import bogdan.refueled.mixin.accessor.ICameraInvoke;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static bogdan.refueled.Utils.isCar;

@OnlyIn(Dist.CLIENT)
public class RenderEvent {
    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public void onRender(ViewportEvent.ComputeCameraAngles evt) {
        if (getCar() != null && !MC.options.getCameraType().isFirstPerson()) {
            Camera camera = evt.getCamera();
            ((ICameraInvoke) camera).invokeMove(
                -((ICameraInvoke) camera).invokeMaxZoom(ClientConfig.carZoom.get() - 4D), 0D, 0D
            );
        }
    }

    @SubscribeEvent
    public void onRender(InputEvent.MouseScrollingEvent evt) {
        if (getCar() != null && !MC.options.getCameraType().isFirstPerson()) {
            ClientConfig.carZoom.set(Mth.clamp(ClientConfig.carZoom.get() - evt.getScrollDelta(), 1D, 20D));
            ClientConfig.carZoom.save();
            evt.setCanceled(true);
        }
    }

    private static Entity getCar() {
        if (MC.player == null) {
            return null;
        }
        Entity entity = MC.player.getVehicle();
        if (isCar(entity)) {
            return entity;
        }
        return null;
    }

    @SubscribeEvent
    public void renderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() != null) {
            if(isCar(event.getEntity().getVehicle())) {
                event.getPoseStack().pushPose();
                event.getPoseStack().scale(0.8f, 0.8f, 0.8f);
            }
        }
    }

    @SubscribeEvent
    public void renderPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getEntity().getVehicle() != null) {
            if(isCar(event.getEntity().getVehicle())) {
                event.getPoseStack().popPose();
            }
        }
    }
}
