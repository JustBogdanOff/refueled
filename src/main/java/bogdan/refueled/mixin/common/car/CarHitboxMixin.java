package bogdan.refueled.mixin.common.car;

import bogdan.refueled.common.accessors.IVehicleAccess;
import org.spongepowered.asm.mixin.Mixin;
import com.dragn0007.dragnvehicles.registry.VehicleRegistry;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = VehicleRegistry.class)
public abstract class CarHitboxMixin {
    @ModifyArg(
            method = {"lambda$static$0", "lambda$static$1", "lambda$static$2", "lambda$static$3", "lambda$static$4", "lambda$static$5"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType$Builder;sized(FF)Lnet/minecraft/world/entity/EntityType$Builder;"
            ),
            index = 0
    )
    private static float changeHitboxWidth(float original){
        return original * IVehicleAccess.sizeFactor.floatValue();
    }

    @ModifyArg(
            method = {"lambda$static$0", "lambda$static$1", "lambda$static$2", "lambda$static$3", "lambda$static$4", "lambda$static$5"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType$Builder;sized(FF)Lnet/minecraft/world/entity/EntityType$Builder;"
            ),
            index = 1
    )
    private static float changeHitboxHeight(float original){
        return original * IVehicleAccess.sizeFactor.floatValue();
    }
}
