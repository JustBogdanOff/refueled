package bogdan.refueled.client.events;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.client.accessors.IVehicleAccessClient;
import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.config.ClientConfig;
import bogdan.refueled.mixin.client.access.ICameraInvoke;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static bogdan.refueled.Utils.isCar;

@OnlyIn(Dist.CLIENT)
public class RenderEvent {
    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (getCar() != null) {
            if(MC.player == null){
                RefueledMain.LOGGER.error("null player when computing camera angles");
                return;
            }
            Camera camera = event.getCamera(); Entity car = getCar();
            ((IVehicleAccessClient) car).refuel$offsetCamera((float) event.getPartialTick(), camera);
            if(!MC.options.getCameraType().isFirstPerson()) {
                ((ICameraInvoke) camera).cam$move(
                        -((ICameraInvoke) camera).cam$getMaxZoom(ClientConfig.carZoom.get() - 4D), 0D, 0D
                );
            }
        }
    }

    @SubscribeEvent
    public void onMouseScroll(InputEvent.MouseScrollingEvent evt) {
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
        if (isCar(MC.player.getVehicle())) {
            return MC.player.getVehicle();
        }
        return null;
    }

    @SubscribeEvent
    public void renderPlayerPre(RenderPlayerEvent.Pre event) {
        if(isCar(event.getEntity().getVehicle())) {
            Entity car = event.getEntity().getVehicle();
            Player player = event.getEntity();
            float factor = car instanceof Motorcycle ? IVehicleAccess.sizeFactor.floatValue() * 1.33f : IVehicleAccess.sizeFactor.floatValue();
            var offsets = ((IVehicleAccessClient) car).refuel$getPlayerOffsets(event.getPartialTick(), player);

            if(player != MC.player)
                car.onPassengerTurned(player);
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offsets.x, offsets.y, offsets.z);
            event.getPoseStack().scale(factor, factor, factor);
            event.getPoseStack().mulPose(((IVehicleAccessClient) car).refuel$getPlayerRotation(event.getPartialTick()));
            //seatPos = seatPos.scale(1 / 0.9375);
        }
    }

    @SubscribeEvent
    public void renderPlayerPost(RenderPlayerEvent.Post event) {
        if(isCar(event.getEntity().getVehicle())) {
            event.getPoseStack().popPose();
        }
    }
}
