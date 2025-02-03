package bogdan.refueled.mixin.car.client;

import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.common.sounds.RefueledHigh;
import bogdan.refueled.common.sounds.RefueledIdle;
import bogdan.refueled.common.sounds.RefueledStart;
import bogdan.refueled.common.sounds.RefueledStarting;
import bogdan.refueled.config.ClientConfig;
import com.dragn0007.dragnvehicles.vehicle.car.Car;
import com.dragn0007.dragnvehicles.vehicle.classic.Classic;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCar;
import com.dragn0007.dragnvehicles.vehicle.suv.SUV;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static bogdan.refueled.Utils.car$isSoundPlaying;

@Mixin(value = {Car.class, Classic.class, Truck.class, SUV.class, SportCar.class, Motorcycle.class})
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
                    this.targetRotation = car$getMaxRotationSpeed();
                if (!car$isRight() && car$isLeft())
                    this.targetRotation = -car$getMaxRotationSpeed();
            }
        }

        if (speed != 0 && car$deltaRotation == 0) {
            this.targetRotation += this.targetRotation < 0 ? (this.targetRotation + Math.abs(rotationSpeed) > 0 ? this.targetRotation * -1f : Math.abs(rotationSpeed)) : (this.targetRotation > 0 ? (this.targetRotation - Math.abs(rotationSpeed) < 0 ? this.targetRotation * -1f : -Math.abs(rotationSpeed)) : 0);
        }
    }

    public void car$displaySpeed(float speed){
        if(!ClientConfig.speedDisplay.get() || Minecraft.getInstance().player == null){
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

    public void car$playSoundLoop(AbstractTickableSoundInstance loop, Level level) {
        if (level.isClientSide) {
            Minecraft.getInstance().getSoundManager().play(loop);
        }
    }

    @Unique
    private RefueledStart car$startLoop;

    @Unique
    private RefueledIdle car$idleLoop;

    @Unique
    private RefueledHigh car$highLoop;

    @Unique
    private RefueledStarting car$startingLoop;

    @Unique
    public void car$checkIdleLoop() {
        if (!car$isSoundPlaying(car$idleLoop)) {
            car$idleLoop = new RefueledIdle(this, car$getEngineSound(), SoundSource.MASTER);
            car$playSoundLoop(car$idleLoop, level());
        }
    }

    @Unique
    public void car$checkHighLoop() {
        if (!car$isSoundPlaying(car$highLoop)) {
            car$highLoop = new RefueledHigh(this, car$getEngineSound(), SoundSource.MASTER);
            car$playSoundLoop(car$highLoop, level());
        }
    }

    @Unique
    public void car$checkStartLoop() {
        if (!car$isSoundPlaying(car$startLoop)) {
            car$startLoop = new RefueledStart(this, SoundEvents.FIRECHARGE_USE, SoundSource.MASTER);
            car$playSoundLoop(car$startLoop, level());
        }
    }

    @Unique
    public void car$checkStartingLoop() {
        if (!car$isSoundPlaying(car$startingLoop)) {
            car$startingLoop = new RefueledStarting(this, SoundEvents.TNT_PRIMED, SoundSource.MASTER);
            car$playSoundLoop(car$startingLoop, level());
        }
    }

    @Unique
    private boolean car$startedLast;

    public void car$updateSounds() {
        if (!car$isStarted() && car$isStarting()) {
            car$checkStartingLoop();
        }

        if (car$getSpeed() == 0 && car$isStarted()) {

            if (!car$startedLast) {
                car$checkStartLoop();
            } else if (!car$isSoundPlaying(car$startLoop)) {
                if (car$startLoop != null) {
                    car$startLoop.setDonePlaying();
                    car$startLoop = null;
                }

                car$checkIdleLoop();
            }
        }
        if (car$getSpeed() != 0 && car$isStarted()) {
            car$checkHighLoop();
        }

        car$startedLast = car$isStarted();
    }
}
