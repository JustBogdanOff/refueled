package bogdan.refueled.mixin.car.client;

import bogdan.refueled.common.accessors.IVehicleAccess;
import com.dragn0007.dragnvehicles.Animation;
import com.dragn0007.dragnvehicles.vehicle.car.CarModel;
import com.dragn0007.dragnvehicles.vehicle.classic.ClassicModel;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.MotorcycleModel;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCarModel;
import com.dragn0007.dragnvehicles.vehicle.suv.SUVModel;
import com.dragn0007.dragnvehicles.vehicle.truck.TruckModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

import java.util.*;

@Mixin(value = {CarModel.class, ClassicModel.class, TruckModel.class, SUVModel.class, SportCarModel.class, MotorcycleModel.class})
public abstract class CarModelMixin{

    @Redirect(
            remap = false,
            method = {"prepareMobModel*", "setupAnim*"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragn0007/dragnvehicles/Animation;animate(Lnet/minecraft/client/model/geom/ModelPart;Lcom/dragn0007/dragnvehicles/Animation;FFF)V",
                    ordinal = 0
            )
    )
    private void refueled$nullifyBodyAnim(ModelPart modelPart, Animation animation, float irrelevantFloat, float irrelevantFloat2, float irrelevantFloat3){
        // Gone.
    }

    @Redirect(
            remap = false,
            method = {"prepareMobModel*", "setupAnim*"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragn0007/dragnvehicles/Animation;animate(Lnet/minecraft/client/model/geom/ModelPart;Lcom/dragn0007/dragnvehicles/Animation;FFF)V",
                    ordinal = 1
            )
    )
    private void refueled$redirectFrontAnim(ModelPart modelPart, Animation animation, float irrelevantFloat, float irrelevantFloat2, float irrelevantFloat3, @Coerce Object car, float partialTick){
        ((IVehicleAccess) (Object) animation).refuel$animate(modelPart, animation, ((IVehicleAccess) car).refuel$getWheelRotation(partialTick));
    }

    @Redirect(
            remap = false,
            method = {"prepareMobModel*", "setupAnim*"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragn0007/dragnvehicles/Animation;animate(Lnet/minecraft/client/model/geom/ModelPart;Lcom/dragn0007/dragnvehicles/Animation;FFF)V",
                    ordinal = 2
            )
    )
    private void refueled$redirectBackAnim(ModelPart modelPart, Animation animation, float irrelevantFloat, float irrelevantFloat2, float irrelevantFloat3, @Coerce Object car, float partialTick){
        ((IVehicleAccess) (Object) animation).refuel$animate(modelPart, animation, ((IVehicleAccess) car).refuel$getWheelRotation(partialTick));
    }

    @ModifyArg(
            method = "createBodyLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/PartPose;offset(FFF)Lnet/minecraft/client/model/geom/PartPose;"
            ),
            index = 2
    )
    private static float offsetParts(float pZ){
        Map<Object, Float> offsets = new HashMap<>();

        offsets.put(CarModel.class, 6f);
        offsets.put(ClassicModel.class, 6f);
        offsets.put(TruckModel.class, 6f);
        offsets.put(SUVModel.class, 6f);
        offsets.put(SportCarModel.class, 3f);
        offsets.put(MotorcycleModel.class, 2.5f);

        return pZ - offsets.get(CarModelMixin.class);
    }
}
