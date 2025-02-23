package bogdan.refueled.common.events;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.config.ClientConfig;
import bogdan.refueled.mixin.accessor.ICameraInvoke;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Quaternionf;

import static bogdan.refueled.Utils.isCar;

@OnlyIn(Dist.CLIENT)
public class RenderEvent {
    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public void onRender(ViewportEvent.ComputeCameraAngles evt) {
        if (getCar() != null) {
            if(MC.player == null){
                RefueledMain.LOGGER.error("Somehow, the player is null");
                return;
            }
            Camera camera = evt.getCamera(); Entity car = getCar(); LocalPlayer player = MC.player;
            double rad = -((ICarInvoker) car).car$getOffsets()[1] * ((ICarInvoker) car).car$getOffsets()[2];

            ((ICameraInvoke) camera).invokeSetPosition(new Vec3(
                    camera.getPosition().x + ((((ICarInvoker) car).car$getSeatPositions()[car.getPassengers().indexOf(player)].z - player.getBbWidth() * 0.5d) * Math.sin(rad * Math.sin(Math.toRadians(car.getYRot())))),
                    camera.getPosition().y + ((ICarInvoker) car).car$getOffsets()[0] * 0.85d + ((player.getBbHeight() * 0.85d) * 0.5d - ((player.getBbHeight() * 0.85d) * 0.5d * Math.cos(-rad))),
                    camera.getPosition().z + ((((ICarInvoker) car).car$getSeatPositions()[car.getPassengers().indexOf(player)].z - player.getBbWidth() * 0.5d) * Math.sin(rad * -Math.cos(Math.toRadians(car.getYRot()))))
            ));
            if(!MC.options.getCameraType().isFirstPerson()) {
                ((ICameraInvoke) camera).invokeMove(
                        -((ICameraInvoke) camera).invokeGetMaxZoom(ClientConfig.carZoom.get() - 4D), 0D, 0D
                );
            }
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
        if (isCar(MC.player.getVehicle())) {
            return MC.player.getVehicle();
        }
        return null;
    }

    @SubscribeEvent
    public void renderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() != null) {
            if(isCar(event.getEntity().getVehicle())) {
                Entity car = event.getEntity().getVehicle();
                Player player = event.getEntity();
                float factor = event.getEntity().getVehicle() instanceof Motorcycle ? ICarInvoker.sizeFactor.floatValue() * 1.33f : ICarInvoker.sizeFactor.floatValue(),
                    rad =(float) -(((ICarInvoker) car).car$getOffsets()[1] * ((ICarInvoker) car).car$getOffsets()[2]);

                event.getPoseStack().pushPose();
                event.getPoseStack().scale(factor, factor, factor);
                event.getPoseStack().translate(
                        ((((ICarInvoker) car).car$getSeatPositions()[car.getPassengers().indexOf(player)].z + player.getBbHeight() * 0.5d + player.getBbWidth() * 0.5d) * Math.sin(rad * Math.sin(Math.toRadians(car.getYRot())))),
                        (((ICarInvoker) car).car$getOffsets()[0] / factor) + ((player.getBbHeight() + player.getBbWidth()) * 0.5d - ((player.getBbHeight() + player.getBbWidth()) * 0.5d * Math.cos(-rad))),
                        ((((ICarInvoker) car).car$getSeatPositions()[car.getPassengers().indexOf(player)].z + player.getBbHeight() * 0.5d + player.getBbWidth() * 0.5d) * Math.sin(rad * -Math.cos(Math.toRadians(car.getYRot()))))
                );

                event.getPoseStack().mulPose(new Quaternionf(
                        (float) Math.sin(rad * 0.5d) * Math.cos(Math.toRadians(car.getYRot())),
                        0,
                        (float) Math.sin(rad * 0.5d) * Math.sin(Math.toRadians(car.getYRot())),
                        (float) Math.cos(rad * 0.5d)
                ));
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
