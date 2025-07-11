package bogdan.refueled.mixin.common;

import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static bogdan.refueled.Utils.isCar;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void refuel$redirectHurt(DamageSource pSource, float pAmount, CallbackInfoReturnable<Boolean> cir){
        if(!(getVehicle() instanceof Motorcycle) && isCar(getVehicle())){
            getVehicle().hurt(pSource, pAmount);
            cir.setReturnValue(false);
        }
    }
}
