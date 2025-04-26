package bogdan.refueled.common.accessors;

import bogdan.refueled.RefueledRegistry;
import com.dragn0007.dragnvehicles.Animation;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public interface IVehicleAccess {

    Double sizeFactor = 0.66d;

    // CLIENT
    void refuel$displaySpeed(float speed);
    // -> WHEELS
    void refuel$animate(ModelPart modelPart, Animation animation, float wheelRotation);
    void refuel$rotateWheels(float deltaRot, float rotSpeed, float speed);
    void refuel$updateWheelRotation();
    float refuel$getWheelRotation(float partialTick);
    // -> SLOPING
    void refuel$angleTick();
    void refuel$offsetCamera(float partialTick, Camera camera);
    float refuel$getAngle(float yawDeg);
    float refuel$getVerticalOffset(float partialTick);
    Vec3 refuel$getPlayerOffsets(float partialTick, Player player);
    Quaternionf refuel$getPlayerRotation(float partialTick);
    Quaternionf refuel$getRotation(float partialTick);
    // -> POSITION & ROTATION
    void refuel$updateClientPos();
    void refuel$tickLerp();
    void refuel$updateLastYRot();
    // -> SOUND
    void refuel$updateSounds();
    void refuel$playSoundLoop(AbstractTickableSoundInstance loop, Level level);
    float refuel$getBatterySoundPitchLevel();
    default float refuel$getPitch(){
        return 1f + (Math.abs(refuel$getSpeed()) / (refuel$getMaxSpeed() * refuel$getModifier()));
    }
    default SoundEvent refuel$getEngineSound(){
        return RefueledRegistry.ENGINE.get();
    }

    Fluid refuel$getFluid();
    void refuel$initTemperature();
    void refuel$centerCar();
    void refuel$onCollision(double speed);
    void refuel$openGUI(Player player);
    void refuel$setStarting(boolean starting, boolean playFailSound);
    void refuel$updateControls(boolean forward, boolean backward, boolean left, boolean right, Player player);
    float refuel$getModifier();
    float refuel$getLowSpeedSteering();

    default float[] refuel$getExhaust(int rand){
        final var factor = sizeFactor;
        double radius = Math.sqrt((1 + (1.8 * factor) - 1) * (1 + (1.8 * factor) - 1) + factor * factor);// calculates distance from entity center to exhaust point
        double pointDist = Math.sqrt((1 + (1.8 * factor) - (1 + radius)) * (1 + (1.8 * factor) - (1 + radius)) + factor * factor);    // calculates distance from exhaust point to current entity viewing point
        double angle = 2f * Math.asin(0.5 * pointDist / radius);

        return new float[]{(float) radius, (float) angle, factor.floatValue() * 0.1f};
    }

    default double[] refuel$getDismountLocations(int offset, double carWidth, double playerWidth){
        double[] dismountLocations = new double[4];

        // 1/16 inlined -> 0.0625
        var actualOffset = offset * (carWidth * 0.5 + playerWidth * 0.5 + 0.0625);
        dismountLocations[0] = -actualOffset;
        dismountLocations[1] = actualOffset;
        dismountLocations[2] = offset == 0 ? -carWidth * 0.5 : -actualOffset;
        dismountLocations[3] = offset == 0 ? -carWidth * 0.5 : actualOffset;

        return dismountLocations;
    }

    default Vec3[] refuel$getSeatPositions(){
        Vec3[] seatPos = new Vec3[4];

        seatPos[0] = new Vec3(0.65 * sizeFactor, 0.3 * sizeFactor, 0);
        seatPos[1] = new Vec3(-0.65 * sizeFactor, 0.3 * sizeFactor, 0);
        seatPos[2] = new Vec3(0.65 * sizeFactor, 0.3 * sizeFactor, -2.3 * sizeFactor);
        seatPos[3] = new Vec3(-0.65 * sizeFactor, 0.3 * sizeFactor, -2.3 * sizeFactor);

        return seatPos;
    }
    Vec3 refuel$getSeatPosition(Entity player);


    Container refuel$getContainer();

    // ENTITY DATA

    int refuel$getFuel();
    int refuel$getBattery();
    float refuel$getHealth();
    float refuel$getTemperature();
    float refuel$getSpeed();
    void refuel$setBattery(int level);
    void refuel$setHealth(float health);

    boolean refuel$isStarted();
    boolean refuel$isStarting();
    boolean refuel$isForward();
    boolean refuel$isBackward();
    boolean refuel$isLeft();
    boolean refuel$isRight();

    // CONFIGURED VEHICLE DATA

    int refuel$getMaxBattery();
    int refuel$getMaxFuel();
    float refuel$getMaxSpeed();
    float refuel$getAcceleration();
    float refuel$getMaxReverseSpeed();
    float refuel$getMaxHealth();

}
