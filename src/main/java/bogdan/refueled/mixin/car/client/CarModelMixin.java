package bogdan.refueled.mixin.car.client;

import bogdan.refueled.common.accessors.ICarInvoker;
import com.dragn0007.dragnvehicles.Animation;
import com.dragn0007.dragnvehicles.vehicle.car.CarModel;
import com.dragn0007.dragnvehicles.vehicle.classic.ClassicModel;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.MotorcycleModel;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCarModel;
import com.dragn0007.dragnvehicles.vehicle.suv.SUVModel;
import com.dragn0007.dragnvehicles.vehicle.truck.TruckModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {CarModel.class, ClassicModel.class, TruckModel.class, SUVModel.class, SportCarModel.class, MotorcycleModel.class})
public abstract class CarModelMixin {

    @Redirect(
        method = {"prepareMobModel*", "setupAnim*"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/dragn0007/dragnvehicles/Animation;animate(Lnet/minecraft/client/model/geom/ModelPart;Lcom/dragn0007/dragnvehicles/Animation;FFF)V",
            ordinal = 0
        ),
        remap = false
    )
    private void refueled$nullifyBodyAnim(ModelPart modelPart, Animation animation, float irrelevantFloat, float irrelevantFloat2, float irrelevantFloat3){
        // Gone.
    }

    @Redirect(
        method = {"prepareMobModel*", "setupAnim*"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/dragn0007/dragnvehicles/Animation;animate(Lnet/minecraft/client/model/geom/ModelPart;Lcom/dragn0007/dragnvehicles/Animation;FFF)V",
            ordinal = 1
        ),
        remap = false
    )
    private void refueled$redirectFrontAnim(ModelPart modelPart, Animation animation, float irrelevantFloat, float irrelevantFloat2, float irrelevantFloat3, @Coerce Object car, float partialTick){
        ((ICarInvoker) (Object) animation).car$animate(modelPart, animation, ((ICarInvoker) car).car$getWheelRotation(partialTick));
    }

    @Redirect(
            method = {"prepareMobModel*", "setupAnim*"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragn0007/dragnvehicles/Animation;animate(Lnet/minecraft/client/model/geom/ModelPart;Lcom/dragn0007/dragnvehicles/Animation;FFF)V",
                    ordinal = 2
            ),
            remap = false
    )
    private void refueled$redirectBackAnim(ModelPart modelPart, Animation animation, float irrelevantFloat, float irrelevantFloat2, float irrelevantFloat3, @Coerce Object car, float partialTick){
        ((ICarInvoker) (Object) animation).car$animate(modelPart, animation, ((ICarInvoker) car).car$getWheelRotation(partialTick));
    }
}
