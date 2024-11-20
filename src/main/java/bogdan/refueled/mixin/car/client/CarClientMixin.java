package bogdan.refueled.mixin.car.client;

import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.config.ClientConfig;
import com.dragn0007.dragnvehicles.vehicle.car.Car;
import com.dragn0007.dragnvehicles.vehicle.classic.Classic;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCar;
import com.dragn0007.dragnvehicles.vehicle.suv.SUV;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static bogdan.refueled.Utils.isCar;

@Mixin({Car.class, Classic.class, Truck.class, SUV.class, SportCar.class, Motorcycle.class})
public abstract class CarClientMixin extends Entity implements ICarInvoker {
    public CarClientMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Shadow(remap = false)
    private float targetRotation, currentRotation;

    public void car$rotateWheels(float car$deltaRotation, float rotationSpeed, float speed, float turnMod){
        this.currentRotation = this.targetRotation;
        if (Math.abs(this.targetRotation + car$deltaRotation * turnMod) <= car$getMaxRotationSpeed() * 3f && this.getControllingPassenger() != null) {
            if (rotationSpeed != 0) this.targetRotation += car$deltaRotation * turnMod;
            else if(Minecraft.getInstance().player != null) {
                if (!car$isLeft() && car$isRight())
                    this.targetRotation += this.targetRotation == car$getMaxRotationSpeed() * 3f ? 0 : (this.targetRotation + car$getMaxRotationSpeed() > car$getMaxRotationSpeed() * 3f ? (-car$getMaxRotationSpeed() * 3f) - this.targetRotation : car$getMaxRotationSpeed());
                if (!car$isRight() && car$isLeft())
                    this.targetRotation += this.targetRotation == -car$getMaxRotationSpeed() * 3f ? 0 : (this.targetRotation - car$getMaxRotationSpeed() < -car$getMaxRotationSpeed() * 3f ? (-car$getMaxRotationSpeed() * 3f) + this.targetRotation : -car$getMaxRotationSpeed());
            }
        }

        if (speed != 0 && car$deltaRotation == 0) {
            this.targetRotation += this.targetRotation < 0 ? (this.targetRotation + rotationSpeed > 0 ? this.targetRotation * -1f : Math.abs(rotationSpeed)) : (this.targetRotation > 0 ? (this.targetRotation - Math.abs(rotationSpeed) < 0 ? this.targetRotation * -1f : -Math.abs(rotationSpeed)) : 0);
        }
    }

    public void car$displaySpeed(float speed){
        if(!ClientConfig.speedDisplay.get()){
            return;
        }

        if(Minecraft.getInstance().player.getVehicle() != null){
            if(Minecraft.getInstance().player.getVehicle().equals(this) && car$isStarted()){
                String speedInfo = Mth.floor((speed * 20f * 60f * 60f) / 1000f) + " KM/H";
                if(ClientConfig.speedImperial.get()){
                    speedInfo = Math.floor(((speed * 20f * 60f * 60f) / 1000f) / 1.609f) + " MPH";
                }
                Minecraft.getInstance().player.displayClientMessage(Component.literal(speedInfo), true);
            }
        }
    }
}
