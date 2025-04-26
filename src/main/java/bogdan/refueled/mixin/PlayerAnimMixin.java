package bogdan.refueled.mixin;

import bogdan.refueled.common.accessors.IVehicleAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static bogdan.refueled.Utils.isCar;

@Mixin(HumanoidModel.class)
public class PlayerAnimMixin {
    @Shadow @Final public ModelPart head;

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/model/HumanoidModel;body:Lnet/minecraft/client/model/geom/ModelPart;",
                    ordinal = 0
            )
    )
    private void injectVehiclePitch(LivingEntity pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch, CallbackInfo ci){
        if(!(pEntity.getFallFlyingTicks() > 4) && !pEntity.isVisuallySwimming() && isCar(pEntity.getVehicle())){
            this.head.xRot = pHeadPitch * 0.017453292F - (((IVehicleAccess) pEntity.getVehicle()).refuel$getAngle(pNetHeadYaw));
        }
    }
}
