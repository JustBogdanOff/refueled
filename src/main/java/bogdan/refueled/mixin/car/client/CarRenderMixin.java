package bogdan.refueled.mixin.car.client;

import com.dragn0007.dragnvehicles.vehicle.car.CarRender;
import com.dragn0007.dragnvehicles.vehicle.classic.ClassicRender;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.MotorcycleRender;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCarRender;
import com.dragn0007.dragnvehicles.vehicle.suv.SUVRender;
import com.dragn0007.dragnvehicles.vehicle.truck.TruckRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

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
        return -0.6f;
    }

    @ModifyArg(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            ),
            index = 1
    )
    private float shrinkRenderZ(float originalZ){
        return -0.6f;
    }

    @ModifyArg(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            ),
            index = 2
    )
    private float shrinkRenderY(float originalY){
        return 0.6f;
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
}
