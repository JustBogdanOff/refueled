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
    void car$animate(ModelPart modelPart, Animation animation, float wheelRotation);

    boolean car$isStarted();
    boolean car$isStarting();
    boolean car$isForward();
    boolean car$isBackward();
    boolean car$isLeft();
    boolean car$isRight();

    void car$setBattery(int level);
    void car$centerCar();
    void car$setHealth(float health);
    void car$onCollision(float speed);
    void car$openCarGUI(Player player);
    void car$setStarting(boolean starting, boolean playFailSound);
    void car$updateControls(boolean forward, boolean backward, boolean left, boolean right, Player player);
    void car$rotateWheels(float deltaRot, float rotSpeed, float speed, float turnMod);
    void car$displaySpeed(float speed);
    void car$playSoundLoop(AbstractTickableSoundInstance loop, Level level);

    int car$getFuel();
    default int car$getMaxFuel(){
        return ServerConfig.modernMaxFuel.get();
    }
    int car$getBattery();
    int car$getMaxBattery();
    int car$getEfficiency(Fluid fluid);

    float car$getHealth();
    default float car$getMaxHealth(){
        return 100f;
    }
    float car$getTemperature();
    float car$getSpeed();
    float car$getMaxSpeed();
    float car$getMaxReverseSpeed();
    float car$getWheelRotation(float partialTick);
    float car$getBatterySoundPitchLevel();
    float car$getAcceleration();
    default float car$getPitch(){
        return 1f + Math.abs(car$getSpeed()) / (car$getMaxSpeed() * ServerConfig.onroadSpeed.get().floatValue());
    }
    default SoundEvent car$getEngineSound(){
        return RefueledRegistry.ENGINE.get();
    }
    default float car$getMaxRotationSpeed(){
        return 6f / 3f;
    }
    default float car$getMinRotationSpeed(){
        return ServerConfig.modernMaxRotation.get().floatValue();
    }
    default float car$getRamDamage(){
        return ServerConfig.modernRamDamage.get().floatValue();
    }

    default float car$getWheelRotationAmount() {
        return ((25.5f / 2f) * 0.6f) * car$getSpeed(); // 25.5 is the wheel diameter, 0.6 is scale factor
    }

    default double[] car$getExhaust(int rand){
        double radius = Math.sqrt((2.2D - 1D) * (2.2D - 1D) + (0.6D - 0) * (0.6D - 0));                             // calculates distance from entity center to exhaust point
        double pointDist = Math.sqrt((2.2D - (1D + radius)) * (2.2D - (1D + radius)) + (0.6D - 0) * (0.6D - 0));    // calculates distance from exhaust point to current entity viewing point
        double angle = 2 * Math.asin(0.5 * pointDist / radius);

        return new double[]{radius, angle, 0.06D};
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

        seatPos[0] = new Vec3(0.6 * 0.6, 0.3 * 0.6, -0.2 * 0.9);
        seatPos[1] = new Vec3(-0.6 * 0.6, 0.3 * 0.6, -0.2 * 0.9);
        seatPos[2] = new Vec3(0.6 * 0.6, 0.3 * 0.6, -2.0 * 0.9);
        seatPos[3] = new Vec3(-0.6 * 0.6, 0.3 * 0.6, -2.0 * 0.9);

        return seatPos;
    }

    Fluid car$getFluid();
}
