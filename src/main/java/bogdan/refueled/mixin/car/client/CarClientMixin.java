package bogdan.refueled.mixin.car.client;

import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.common.sounds.RefueledHigh;
import bogdan.refueled.common.sounds.RefueledIdle;
import bogdan.refueled.common.sounds.RefueledStart;
import bogdan.refueled.common.sounds.RefueledStarting;
import bogdan.refueled.config.ClientConfig;
import bogdan.refueled.mixin.accessor.IBlockBehaviourAccess;
import bogdan.refueled.mixin.accessor.ICameraInvoke;
import com.dragn0007.dragnvehicles.vehicle.car.Car;
import com.dragn0007.dragnvehicles.vehicle.classic.Classic;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCar;
import com.dragn0007.dragnvehicles.vehicle.suv.SUV;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.function.Function;

import static bogdan.refueled.Utils.isSoundPlaying;

@Mixin(value = {Car.class, Classic.class, Truck.class, SUV.class, SportCar.class, Motorcycle.class})
public abstract class CarClientMixin extends Entity implements IVehicleAccess {
    public CarClientMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Shadow(remap = false)
    private float targetRotation, currentRotation;

    public void refuel$rotateWheels(float refuel$deltaRotation, float rotationSpeed, float speed){
        currentRotation = targetRotation;
        if (Math.abs(targetRotation + refuel$deltaRotation) <= refuel$getLowSpeedSteering() * 3f && this.getControllingPassenger() != null) {
            if (rotationSpeed != 0) targetRotation += refuel$deltaRotation;
            else if(Minecraft.getInstance().player != null) {
                if (!refuel$isLeft() && refuel$isRight())
                    targetRotation = refuel$getLowSpeedSteering();
                if (!refuel$isRight() && refuel$isLeft())
                    targetRotation = -refuel$getLowSpeedSteering();
            }
        }

        if (speed != 0 && refuel$deltaRotation == 0) {
            targetRotation += targetRotation < 0 ? (targetRotation + Math.abs(rotationSpeed) > 0 ? targetRotation * -1f : Math.abs(rotationSpeed)) : (targetRotation > 0 ? (targetRotation - Math.abs(rotationSpeed) < 0 ? targetRotation * -1f : -Math.abs(rotationSpeed)) : 0);
        }
    }

    public void refuel$displaySpeed(float speed){
        if(!ClientConfig.speedDisplay.get() || Minecraft.getInstance().player == null){
            return;
        }

        if(Minecraft.getInstance().player.getVehicle() != null){
            if(Minecraft.getInstance().player.getVehicle().equals(this) && refuel$isStarted()){
                String speedInfo = Mth.floor((speed * 20f * 60f * 60f) / 1000f) + " KM/H";
                if(ClientConfig.speedImperial.get())
                    speedInfo = Mth.floor(((speed * 20f * 60f * 60f) / 1000f) / 1.609f) + " MPH";
                Minecraft.getInstance().player.displayClientMessage(Component.literal(speedInfo), true);
            }
        }
    }

    public void refuel$playSoundLoop(AbstractTickableSoundInstance loop, Level level) {
        if (level.isClientSide) {
            Minecraft.getInstance().getSoundManager().play(loop);
        }
    }

    @Unique
    private RefueledStart refuel$startLoop;

    @Unique
    private RefueledIdle refuel$idleLoop;

    @Unique
    private RefueledHigh refuel$highLoop;

    @Unique
    private RefueledStarting refuel$startingLoop;

    @Unique
    public void refuel$checkIdleLoop() {
        if (!isSoundPlaying(refuel$idleLoop)) {
            refuel$idleLoop = new RefueledIdle(this, refuel$getEngineSound(), SoundSource.MASTER);
            refuel$playSoundLoop(refuel$idleLoop, level());
        }
    }

    @Unique
    public void refuel$checkHighLoop() {
        if (!isSoundPlaying(refuel$highLoop)) {
            refuel$highLoop = new RefueledHigh(this, refuel$getEngineSound(), SoundSource.MASTER);
            refuel$playSoundLoop(refuel$highLoop, level());
        }
    }

    @Unique
    public void refuel$checkStartLoop() {
        if (!isSoundPlaying(refuel$startLoop)) {
            refuel$startLoop = new RefueledStart(this, SoundEvents.FIRECHARGE_USE, SoundSource.MASTER);
            refuel$playSoundLoop(refuel$startLoop, level());
        }
    }

    @Unique
    public void refuel$checkStartingLoop() {
        if (!isSoundPlaying(refuel$startingLoop)) {
            refuel$startingLoop = new RefueledStarting(this, SoundEvents.TNT_PRIMED, SoundSource.MASTER);
            refuel$playSoundLoop(refuel$startingLoop, level());
        }
    }

    @Unique
    private boolean refuel$startedLast;

    public void refuel$updateSounds() {
        if (!refuel$isStarted() && refuel$isStarting())
            refuel$checkStartingLoop();

        if (refuel$getSpeed() == 0 && refuel$isStarted()) {
            if (!refuel$startedLast)
                refuel$checkStartLoop();
            else if (!isSoundPlaying(refuel$startLoop)) {
                if (refuel$startLoop != null) {
                    refuel$startLoop.setDonePlaying();
                    refuel$startLoop = null;
                }

                refuel$checkIdleLoop();
            }
        }
        if (refuel$getSpeed() != 0 && refuel$isStarted()) {
            refuel$checkHighLoop();
        }

        refuel$startedLast = refuel$isStarted();
    }

    @Unique
    public double[] refuel$angleOffsets = new double[2],
            refuel$lastAngleOffsets = new double[2];

    public float refuel$getVerticalOffset(float tickDelta) {
        return Mth.lerp(tickDelta, (float) refuel$lastAngleOffsets[0], (float) refuel$angleOffsets[0]);
    }

    public Quaternionf refuel$getRotation(float tickDelta) {
        var rot = new Quaternionf();
        Axis.XP.rotation((float) refuel$lastAngleOffsets[1]).slerp(Axis.XP.rotation((float) refuel$angleOffsets[1]), tickDelta, rot);
        return rot;
    }

    public Quaternionf refuel$getPlayerRotation(float partialTick){
        var rot = new Quaternionf();
        new Quaternionf(
                Math.sin(refuel$lastAngleOffsets[1] * 0.5) * Math.cos(Math.toRadians(refuel$lastYRot)),
                0,
                Math.sin(refuel$lastAngleOffsets[1] * 0.5) * Math.sin(Math.toRadians(refuel$lastYRot)),
                Math.cos(refuel$lastAngleOffsets[1] * 0.5)).slerp(new Quaternionf(
                    Math.sin(refuel$angleOffsets[1] * 0.5) * Math.cos(Math.toRadians(getYRot())),
                    0,
                    Math.sin(refuel$angleOffsets[1] * 0.5) * Math.sin(Math.toRadians(getYRot())),
                    Math.cos(refuel$angleOffsets[1] * 0.5)), partialTick, rot);
        return rot;
    }

    public Vec3 refuel$getPlayerOffsets(float partialTick, Player player){
        var oPos = refuel$getSeatPosition(player);
        Vec3 lastPos = oPos
                .add(0, -1.5 * IVehicleAccess.sizeFactor, 0)
                .xRot((float) -refuel$lastAngleOffsets[1])
                .add(0, 1.5 * IVehicleAccess.sizeFactor - oPos.y, -oPos.z),

            newPos = oPos
                    .add(0, -1.5 * IVehicleAccess.sizeFactor, 0)
                    .xRot((float) -refuel$angleOffsets[1])
                    .add(0, 1.5 * IVehicleAccess.sizeFactor - oPos.y, -oPos.z);

        lastPos = new Vec3(lastPos.z * -Math.sin(Math.toRadians(refuel$lastYRot)), lastPos.y + refuel$lastAngleOffsets[0] * IVehicleAccess.sizeFactor, lastPos.z * Math.cos(Math.toRadians(refuel$lastYRot)));
        newPos = new Vec3(newPos.z * -Math.sin(Math.toRadians(getYRot())), newPos.y + refuel$angleOffsets[0] * IVehicleAccess.sizeFactor, newPos.z * Math.cos(Math.toRadians(getYRot())));

        return lastPos.lerp(newPos, partialTick);
    }

    public void refuel$offsetCamera(float partialTick, Camera camera){
        var oPos = refuel$getSeatPosition(Minecraft.getInstance().player);
        if(oPos == null) return;

        Vec3 lastPos = oPos
                .add(0, -1.5 * IVehicleAccess.sizeFactor, 0)
                .xRot((float) -refuel$lastAngleOffsets[1])
                .add(0, 1.5 * IVehicleAccess.sizeFactor - oPos.y, -oPos.z),

                newPos = oPos
                        .add(0, -1.5 * IVehicleAccess.sizeFactor, 0)
                        .xRot((float) -refuel$angleOffsets[1])
                        .add(0, 1.5 * IVehicleAccess.sizeFactor - oPos.y, -oPos.z);

        //noinspection DataFlowIssue
        double hOffset = (1.875 * 0.85 * 0.66 * (Minecraft.getInstance().player.getVehicle() instanceof Motorcycle ? 1.33 : 1));
        lastPos = new Vec3(
                (lastPos.z - hOffset * Math.sin(-refuel$lastAngleOffsets[1])) * -Math.sin(Math.toRadians(refuel$lastYRot)),
                lastPos.y + refuel$lastAngleOffsets[0] * IVehicleAccess.sizeFactor - (hOffset - hOffset * Math.cos(-refuel$lastAngleOffsets[1])),
                (lastPos.z - hOffset * Math.sin(-refuel$lastAngleOffsets[1])) * Math.cos(Math.toRadians(refuel$lastYRot)));
        newPos = new Vec3(
                (newPos.z - hOffset * Math.sin(-refuel$angleOffsets[1])) * -Math.sin(Math.toRadians(getYRot())),
                newPos.y + refuel$angleOffsets[0] * IVehicleAccess.sizeFactor - (hOffset - hOffset * Math.cos(-refuel$angleOffsets[1])),
                (newPos.z - hOffset * Math.sin(-refuel$angleOffsets[1])) * Math.cos(Math.toRadians(getYRot())));
        ((ICameraInvoke) camera).cam$setPosition(camera.getPosition().add(lastPos).lerp(camera.getPosition().add(newPos), partialTick));
    }
    
    public float refuel$getAngle(float yawDeg){
        return (float) (refuel$angleOffsets[1] * Math.abs(Math.cos(yawDeg * 0.017453292F)));
    }

    public void refuel$angleTick(){
        refuel$lastAngleOffsets = refuel$angleOffsets;
        double modelToStackFactor = 1 / 16d;

        int leftRight = Math.round((float) -Math.sin(Math.toRadians(getYRot())));
        int frontBack = Math.round((float) Math.cos(Math.toRadians(getYRot())));
        //noinspection ConstantValue,EqualsBetweenInconvertibleTypes
        double wheelRadius = CarClientMixin.class.equals(Motorcycle.class) ? 38d * 0.5d : 53d * 0.5d;


        Function<BlockPos, Double> getMaxY = pos -> {
            var shape = level().getBlockState(pos).getCollisionShape(level(), pos);
            shape = shape.isEmpty() ? level().getBlockState(pos).getVisualShape(level(), pos, CollisionContext.empty()) : shape;
            return shape.isEmpty() ? 0 : shape.max(Direction.Axis.Y);
        };

        // BLOCK DETECTION
        Function<Vec3, BlockPos> find = pos -> {
            AABB area = new AABB(
                    pos.x - (frontBack * getBbWidth() * 0.49d), pos.y, pos.z - ((leftRight * getBbWidth() * 0.49d)),
                    pos.x + (frontBack * getBbWidth() * 0.49d), pos.y, pos.z + (leftRight * getBbWidth() * 0.49d));
            List<BlockPos> positions = BlockPos.betweenClosedStream(area).map(BlockPos::new).toList();
            BlockPos finalPos = null;
            double maxY = 0;

            for(BlockPos position : positions) {
                BlockState state = level().getBlockState(position);

                if (!state.isAir() && ((IBlockBehaviourAccess) state.getBlock()).hasCollision()) {
                    if (getMaxY.apply(position) > maxY) {
                        finalPos = position;
                        maxY = getMaxY.apply(position);
                    }
                }
            }

            return finalPos;
        };

        Function<Vec3, BlockPos> check = anchorPos -> {
            int searchIndex = 1;
            BlockPos finalPos = null;

            while(searchIndex < 4){
                var pos = anchorPos;

                switch(searchIndex){
                    case 1:
                        if(find.apply(pos) != null){
                            searchIndex += 2;
                            break;
                        }
                        searchIndex++;
                    case 2:
                        pos = anchorPos.subtract(0, 1, 0);
                        if(find.apply(pos) != null) finalPos = find.apply(pos);
                        searchIndex++;
                    case 3:
                        pos = anchorPos.subtract(0, 1, 0);
                        if(find.apply(pos) == null){
                            pos = pos.subtract(0, 1, 0);
                            if(find.apply(pos) != null) finalPos = find.apply(pos);
                        }
                        searchIndex++;
                }
            }

            return finalPos;
        };

        var opposite = 1;
        BlockPos foundPos = null;
        Vec3[] anchors = new Vec3[4];
        anchors[0] = new Vec3(getX() + (leftRight * wheelRadius * IVehicleAccess.sizeFactor * modelToStackFactor), getY() + 2, getZ() + (frontBack * wheelRadius * modelToStackFactor * IVehicleAccess.sizeFactor));
        anchors[1] = new Vec3(getX() - (leftRight * wheelRadius * IVehicleAccess.sizeFactor * modelToStackFactor), getY() + 2, getZ() - (frontBack * wheelRadius * modelToStackFactor * IVehicleAccess.sizeFactor));
        anchors[2] = new Vec3(anchors[0].x + leftRight, anchors[0].y, anchors[0].z + frontBack);
        anchors[3] = new Vec3(anchors[1].x - leftRight, anchors[1].y, anchors[1].z - frontBack);
        double globalMaxY = getY();

        // ITERATION
        for(int i = 0; i < 4; i++){
            var pos = check.apply(anchors[i]);
            if(check.apply(anchors[i]) != null){
                if(pos.getY() + getMaxY.apply(pos) > globalMaxY){
                    foundPos = pos;
                    globalMaxY = pos.getY() + getMaxY.apply(pos);
                    opposite = i % 2 == 1 ? -1 : 1;
                }
            }
        }

        if(foundPos == null){
            refuel$angleOffsets = new double[2];
            return;
        }

        // CALCULATION
        VoxelShape shape = level().getBlockState(foundPos).getCollisionShape(level(), foundPos);
        shape = shape.isEmpty() ? level().getBlockState(foundPos).getVisualShape(level(), foundPos, CollisionContext.empty()) : shape;
        double bodyRadius = wheelRadius * modelToStackFactor * IVehicleAccess.sizeFactor,
                blockHeight = Math.min(Math.max((foundPos.getY() + shape.max(Direction.Axis.Y)) - getY(), 0), 2 * bodyRadius),
                edgeLengthX = leftRight * opposite > 0 ? shape.min(Direction.Axis.X) : shape.max(Direction.Axis.X),
                edgeLengthZ = frontBack * opposite > 0 ? shape.min(Direction.Axis.Z) : shape.max(Direction.Axis.Z),
                blockDistance = frontBack == 0 ? Math.max(getX(), (foundPos.getX() + edgeLengthX)) - Math.min(getX(), (foundPos.getX() + edgeLengthX)) : Math.max(getZ(), (foundPos.getZ() + edgeLengthZ)) - Math.min(getZ(), (foundPos.getZ() + edgeLengthZ));
        blockDistance = Math.max(Math.abs(blockDistance), bodyRadius);

        double distance = Math.sqrt(Mth.square(2 * bodyRadius) - Mth.square((blockHeight / blockDistance) * bodyRadius)),
                radians = Math.acos(distance / (2 * bodyRadius)),
                offsetY = (modelToStackFactor * wheelRadius * Math.sin(radians)) + (modelToStackFactor * 17d * 0.5) - 1.5 + (1.5 - (modelToStackFactor * 17d * 0.5)) * Math.cos(radians);

        // APPLICATION
        this.refuel$angleOffsets = new double[]{offsetY, -radians * opposite};
    }

    @Unique
    private double refuel$lastYRot = 0;

    public void refuel$updateLastYRot(){
        refuel$lastYRot = getYRot();
    }

    @Unique
    public float refuel$clientPitch;

    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.targetYRot = yaw;
        this.refuel$clientPitch = pitch;
        this.lerpSteps = 10;
    }

    @Unique
    public float refuel$wheelRotation;

    @Unique
    public void refuel$updateWheelRotation() {
        refuel$wheelRotation += (8.5f * refuel$getSpeed());
    }

    public float refuel$getWheelRotation(float partialTicks) {
        return refuel$wheelRotation + (8.5f * refuel$getSpeed()) * partialTicks;
    }

    public void refuel$updateClientPos(){
        lastClientPos = position();
    }

    @Shadow(remap = false)
    public Vec3 lastClientPos;
    @Shadow(remap = false)
    private int lerpSteps;
    @Shadow(remap = false)
    private float targetYRot;
    @Shadow(remap = false)
    private double targetX, targetY, targetZ;

    @Unique
    public void refuel$tickLerp() {
        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }

        if (this.lerpSteps > 0) {
            double d0 = getX() + (targetX - getX()) / (double) lerpSteps;
            double d1 = getY() + (targetY - getY()) / (double) lerpSteps;
            double d2 = getZ() + (targetZ - getZ()) / (double) lerpSteps;
            double d3 = Mth.wrapDegrees(targetYRot - (double) getYRot());
            setYRot((float) ((double) getYRot() + d3 / (double) lerpSteps));
            --lerpSteps;
            setPos(d0, d1, d2);
            setRot(getYRot(), getXRot());
        }
    }
}
