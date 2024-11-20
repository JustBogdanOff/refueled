package bogdan.refueled.mixin.car;

import org.spongepowered.asm.mixin.Mixin;
import com.dragn0007.dragnvehicles.registry.VehicleRegistry;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(VehicleRegistry.class)
public abstract class CarHitboxMixin {
    /* lambda$static:
        0: Modern Car
        1: Classic Car
        2: Truck
        3: SUV
        4: Sport Car
        5: Motorcycle
    */

    @ModifyArg(
        method = {"lambda$static$0", "lambda$static$1", "lambda$static$2", "lambda$static$3", "lambda$static$4", "lambda$static$5"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType$Builder;sized(FF)Lnet/minecraft/world/entity/EntityType$Builder;"
        ),
        index = 0,
        remap = false
    )
    private static float changeModernHitboxWidth(float original){
        return original * 0.8f;
    }

    @ModifyArg(
        method = {"lambda$static$0", "lambda$static$1", "lambda$static$2", "lambda$static$3", "lambda$static$4", "lambda$static$5"},
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/EntityType$Builder;sized(FF)Lnet/minecraft/world/entity/EntityType$Builder;"
        ),
        index = 1,
        remap = false
    )
    private static float changeModernHitboxHeight(float original){
        return original * 0.8f;
    }
}
