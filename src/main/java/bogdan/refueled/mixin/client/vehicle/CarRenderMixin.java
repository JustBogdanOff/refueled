package bogdan.refueled.mixin.client.vehicle;

import bogdan.refueled.client.accessors.IVehicleAccessClient;
import bogdan.refueled.common.accessors.IVehicleAccess;
import com.dragn0007.dragnvehicles.vehicle.car.CarRender;
import com.dragn0007.dragnvehicles.vehicle.classic.ClassicRender;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.MotorcycleRender;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCarRender;
import com.dragn0007.dragnvehicles.vehicle.suv.SUVRender;
import com.dragn0007.dragnvehicles.vehicle.truck.TruckRender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {CarRender.class, ClassicRender.class, TruckRender.class, SUVRender.class, SportCarRender.class, MotorcycleRender.class})
public abstract class CarRenderMixin {
    @ModifyArg(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            ),
            index = 0
    )
    private float shrinkRenderX(float originalX){
        return -IVehicleAccess.sizeFactor.floatValue();
    }

    @ModifyArg(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            ),
            index = 1
    )
    private float shrinkRenderY(float originalY){
        return -IVehicleAccess.sizeFactor.floatValue();
    }

    @ModifyArg(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            ),
            index = 2
    )
    private float shrinkRenderZ(float originalZ){
        return IVehicleAccess.sizeFactor.floatValue();
    }

    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragn0007/dragnvehicles/Animation$KeyFrame;<init>(FFFF)V",
                    ordinal = 6
            ),
            index = 0
    )
    private static float modifyFrontKeyFrame(float original){
        return 1f;
    }

    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragn0007/dragnvehicles/Animation$KeyFrame;<init>(FFFF)V",
                    ordinal = 8
            ),
            index = 0
    )
    private static float modifyBackKeyFrame(float original){
        return 1f;
    }

    @Inject(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectSloping(@Coerce Object car, float rotation, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci){
        poseStack.translate(0, -((IVehicleAccessClient) car).refuel$getVerticalOffset(partialTick), 0);
        poseStack.mulPose(((IVehicleAccessClient) car).refuel$getRotation(partialTick));
    }
}
