package bogdan.refueled.common.accessors;

import bogdan.refueled.RefueledRegistry;
import bogdan.refueled.config.ServerConfig;
import com.dragn0007.dragnvehicles.Animation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface ICarInvoker{

    Double sizeFactor = 0.66d;

    // CLIENT
    void car$animate(ModelPart modelPart, Animation animation, float wheelRotation);
    void car$playSoundLoop(AbstractTickableSoundInstance loop, Level level);
    void car$displaySpeed(float speed);
    void car$setOffsets(double[] offsets);
    double[] car$getOffsets();
    default float car$getWheelRotationAmount() {
        return 8.5f * car$getSpeed();
    }

    Fluid car$getFluid();

    boolean car$isStarted();
    boolean car$isStarting();
    boolean car$isForward();
    boolean car$isBackward();
    boolean car$isLeft();
    boolean car$isRight();

    void car$updateSounds();
    void car$initTemperature();
    void car$setBattery(int level);
    void car$centerCar();
    void car$setHealth(float health);
    void car$onCollision(float speed);
    void car$openCarGUI(Player player);
    void car$setStarting(boolean starting, boolean playFailSound);
    void car$updateControls(boolean forward, boolean backward, boolean left, boolean right, Player player);
    void car$rotateWheels(float deltaRot, float rotSpeed, float speed, float turnMod);

    int car$getFuel();
    int car$getBattery();
    int car$getMaxBattery();
    int car$getEfficiency(Fluid fluid);
    default int car$getMaxFuel(){
        return ServerConfig.modernMaxFuel.get();
    }

    float car$getHealth();
    float car$getTemperature();
    float car$getSpeed();
    float car$getMaxSpeed();
    float car$getMaxReverseSpeed();
    float car$getWheelRotation(float partialTick);
    float car$getBatterySoundPitchLevel();
    float car$getAcceleration();
    default float car$getMaxHealth(){
        return 100f;
    }
    default float car$getPitch(){
        return 1f + Math.abs(car$getSpeed()) / (car$getMaxSpeed() * ServerConfig.onroadSpeed.get().floatValue());
    }

    default float car$getMaxRotationSpeed(){
        return 3f * sizeFactor.floatValue();
    }
    default float car$getMinRotationSpeed(){
        return ServerConfig.modernMaxRotation.get().floatValue();
    }
    default float car$getRamDamage(){
        return ServerConfig.modernRamDamage.get().floatValue();
    }

    default double[] car$getExhaust(int rand){
        double radius = Math.sqrt((1d + (1.8d * sizeFactor) - 1D) * (1d + (1.8d * sizeFactor) - 1d) + (sizeFactor - 0) * (sizeFactor - 0));                             // calculates distance from entity center to exhaust point
        double pointDist = Math.sqrt((1d + (1.8d * sizeFactor) - (1D + radius)) * (1d + (1.8d * sizeFactor) - (1d + radius)) + (sizeFactor - 0) * (sizeFactor - 0));    // calculates distance from exhaust point to current entity viewing point
        double angle = 2 * Math.asin(0.5 * pointDist / radius);

        return new double[]{radius, angle, sizeFactor * 0.1f};
    }

    default double[] car$getDismountLocations(int offset, AABB carBB, AABB playerBB){
        double[] dismountLocations = new double[4];

        dismountLocations[0] = (double) offset * (-carBB.getXsize() / 2D - playerBB.getXsize() / 2D - 1D / 16D);
        dismountLocations[1] = (double) offset * (carBB.getXsize() / 2D + playerBB.getXsize() / 2D + 1D / 16D);
        dismountLocations[2] = (offset == 0 ? offset - 1D : (double) offset * (-carBB.getXsize() / 2D - playerBB.getXsize() / 2D + 1D / 16D));
        dismountLocations[3] = (offset == 0 ? offset - 1D : (double) offset * (carBB.getXsize() / 2D + playerBB.getXsize() / 2D + 1D / 16D));

        return dismountLocations;
    }

    default Vec3[] car$getSeatPositions(){
        Vec3[] seatPos = new Vec3[4];

        seatPos[0] = new Vec3(0.65 * sizeFactor, 0.3 * sizeFactor, 0);
        seatPos[1] = new Vec3(-0.65 * sizeFactor, 0.3 * sizeFactor, 0);
        seatPos[2] = new Vec3(0.65 * sizeFactor, 0.3 * sizeFactor, -2.3 * sizeFactor);
        seatPos[3] = new Vec3(-0.65 * sizeFactor, 0.3 * sizeFactor, -2.3 * sizeFactor);

        return seatPos;
    }

    default SoundEvent car$getEngineSound(){
        return RefueledRegistry.ENGINE.get();
    }
}
