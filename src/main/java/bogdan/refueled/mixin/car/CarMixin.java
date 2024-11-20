package bogdan.refueled.mixin.car;

import bogdan.refueled.RefueledRegistry;
import bogdan.refueled.common.network.*;
import bogdan.refueled.common.sounds.RefueledSounds;
import bogdan.refueled.common.sounds.RefueledHigh;
import bogdan.refueled.common.sounds.RefueledIdle;
import bogdan.refueled.common.sounds.RefueledStart;
import bogdan.refueled.common.sounds.RefueledStarting;
import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.common.gui.CarGUI;
import bogdan.refueled.config.ServerConfig;
import bogdan.refueled.mixin.accessor.IBiomeTempInvoke;
import bogdan.refueled.mixin.accessor.IDmgSourceInvoke;
import bogdan.refueled.mixin.accessor.IHeightAccess;
import com.dragn0007.dragnvehicles.vehicle.car.Car;
import com.dragn0007.dragnvehicles.vehicle.classic.Classic;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCar;
import com.dragn0007.dragnvehicles.vehicle.suv.SUV;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import de.maxhenkel.car.fluids.ModFluids;
import de.maxhenkel.car.items.ItemCanister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static bogdan.refueled.Utils.*;

@Mixin(value = {Car.class, Classic.class, Truck.class, SUV.class, SportCar.class, Motorcycle.class})
public abstract class CarMixin extends Entity implements ICarInvoker, Container, IFluidHandler {
    public CarMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Shadow(remap = false)
    protected abstract Vec3 calcOffset(double x, double y, double z);

    @Override
    public void positionRider(Entity entity, MoveFunction moveFunction) {
        int i = this.getPassengers().indexOf(entity);
        entity.setPos(this.calcOffset(car$getSeatPositions()[i].x, car$getSeatPositions()[i].y, car$getSeatPositions()[i].z));

        entity.setBoundingBox(AABB.ofSize(entity.getBoundingBox().getCenter(), entity.getBoundingBox().getXsize() * 0.8, entity.getEyeHeight(Pose.SITTING) * 0.8, entity.getBoundingBox().getZsize() * 0.8));
        ((IHeightAccess) entity).setEyeHeight(entity.getEyeHeight(Pose.SITTING) * 0.8f);


        entity.setYRot(entity.getYRot() + car$deltaRotation);
        entity.setYHeadRot(entity.getYHeadRot() + this.car$deltaRotation);
        car$applyYawToEntity(entity);
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entityToUpdate) {
        car$applyYawToEntity(entityToUpdate);
    }

    @Unique
    public void car$applyYawToEntity(Entity entityToUpdate) {
        entityToUpdate.setYBodyRot(getYRot());
        float f = Mth.wrapDegrees(entityToUpdate.getYRot() - getYRot());
        float f1 = Mth.clamp(f, -130.0F, 130.0F);
        entityToUpdate.yRotO += f1 - f;
        entityToUpdate.setYRot(entityToUpdate.getYRot() + f1 - f);
        entityToUpdate.setYHeadRot(entityToUpdate.getYRot());
    }

    @Unique
    public Container car$internalInventory;

    @Unique
    public FluidStack car$fluidInventory;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void car$addInit(EntityType<?> entityType, Level level, CallbackInfo ci) {
        this.car$internalInventory = new SimpleContainer(27);
        this.car$fluidInventory = FluidStack.EMPTY;
    }

    // ENTITYDATA DEFINERS

    @Unique
    private static final EntityDataAccessor<String>
            car$FUEL_TYPE = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.STRING);
    @Unique
    private static final EntityDataAccessor<Float>
            car$TEMPERATURE = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.FLOAT),
            car$SPEED = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Integer>
            car$FUEL_AMOUNT = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.INT),
            car$BATTERY_LEVEL = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.INT),
            car$STARTING_TIME = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Boolean>
            car$STARTING = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            car$STARTED = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            car$FORWARD = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            car$BACKWARD = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            car$LEFT = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            car$RIGHT = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN);

    @Inject(
            method = "addAdditionalSaveData",
            at = @At("TAIL")
    )
    private void car$saveDataToNBT(CompoundTag compoundTag, CallbackInfo ci) {
        compoundTag.putInt("fuel", car$getFuel());
        compoundTag.putString("fuel_type", car$getFuelType());
        compoundTag.putFloat("engine_temp", this.entityData.get(car$TEMPERATURE));
        compoundTag.putInt("battery_level", this.entityData.get(car$BATTERY_LEVEL));
        saveInventory(compoundTag, "int_inventory", car$internalInventory);
        if (!car$fluidInventory.isEmpty()) {
            compoundTag.put("fluid_inventory", car$fluidInventory.writeToNBT(new CompoundTag()));
        }
    }

    @Inject(
            method = "readAdditionalSaveData",
            at = @At("TAIL")
    )
    private void car$readDataFromNBT(CompoundTag compoundTag, CallbackInfo ci) {
        car$setFuel(compoundTag.getInt("fuel"));
        if (compoundTag.contains("fuel_type")) {
            car$setFuelType(compoundTag.getString("fuel_type"));
        }
        this.entityData.set(car$TEMPERATURE, compoundTag.getFloat("engine_temp"));
        this.entityData.set(car$BATTERY_LEVEL, compoundTag.getInt("battery_level"));
        readInventory(compoundTag, "int_inventory", car$internalInventory);
        if (compoundTag.contains("fluid_inventory")) {
            car$fluidInventory = FluidStack.EMPTY;
        }

        if (compoundTag.getAllKeys().stream().allMatch(s -> s.equals("id"))) {
            car$setFuel(100);
            car$setBattery(500);
            car$initTemperature();
        }
    }

    @Inject(
            method = "defineSynchedData",
            at = @At("TAIL")
    )
    private void car$syncData(CallbackInfo ci) {
        this.entityData.define(car$FUEL_AMOUNT, 0);
        this.entityData.define(car$FUEL_TYPE, "");
        this.entityData.define(car$TEMPERATURE, 0f);
        this.entityData.define(car$BATTERY_LEVEL, 0);
        this.entityData.define(car$STARTING_TIME, 0);
        this.entityData.define(car$STARTING, false);
        this.entityData.define(car$STARTED, false);
        this.entityData.define(car$SPEED, 0f);
        this.entityData.define(car$FORWARD, false);
        this.entityData.define(car$BACKWARD, false);
        this.entityData.define(car$LEFT, false);
        this.entityData.define(car$RIGHT, false);
    }

    @SuppressWarnings("unchecked")
    @Inject(
            method = "getCapability",
            at = @At(
                    value = "RETURN",
                    ordinal = 1
            ),
            cancellable = true,
            remap = false
    )
    private <T> void car$addFluidCapability(@NotNull Capability<T> cap, @Nullable Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (this.isAlive() && cap == ForgeCapabilities.FLUID_HANDLER) {
            cir.setReturnValue(LazyOptional.of(() -> (T) this));
        }
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return car$getDriver();
    }

    @Shadow(remap = false)
    @Final
    private static EntityDataAccessor<Float> HEALTH;

    @Unique
    public float car$getSpeed() {
        return this.entityData.get(car$SPEED);
    }

    @Unique
    public void car$setSpeed(float speed) {
        this.entityData.set(car$SPEED, speed);
    }


    // HEALTH

    @Unique
    public float car$getHealth() {
        return this.entityData.get(HEALTH);
    }

    @Unique
    public void car$setHealth(float health) {
        if (health > 100F) {
            health = car$getMaxHealth();
        } else if (health < 0) {
            this.kill();
        }
        this.entityData.set(HEALTH, health);
    }

    @Unique
    public void car$addDamage(float damage) {
        car$setHealth(car$getHealth() - damage);
    }


    // FUEL

    @Unique
    public int car$getFuel() {
        return this.entityData.get(car$FUEL_AMOUNT);
    }

    @Unique
    public void car$setFuel(int fuel) {
        this.entityData.set(car$FUEL_AMOUNT, fuel);
    }

    @Unique
    public String car$getFuelType() {
        return this.entityData.get(car$FUEL_TYPE);
    }

    @Unique
    public void car$setFuelType(String fluid) {
        if (fluid == null) {
            fluid = "";
        }
        this.entityData.set(car$FUEL_TYPE, fluid);
    }

    @Unique
    public void car$setFuelType(Fluid fluid) {
        car$setFuelType(ForgeRegistries.FLUIDS.getKey(fluid).toString());
    }

    @Unique
    public int car$getMaxFuel() {
        return 1000;
    }


    @Nullable
    public Fluid car$getFluid() {
        String fuelType = car$getFuelType();
        if (fuelType == null || fuelType.isEmpty()) {
            return null;
        }

        return ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fuelType));
    }


    // BATTERY

    @Unique
    public int car$getBattery() {
        return this.entityData.get(car$BATTERY_LEVEL);
    }

    @Unique
    public int car$getMaxBattery() {
        return 1000;
    }

    @Unique
    public void car$setBattery(int level) {
        if (level < 0) {
            level = 0;
        } else if (level > car$getMaxBattery()) {
            level = car$getMaxBattery();
        }
        this.entityData.set(car$BATTERY_LEVEL, level);
    }


    // ENGINE TEMPERATURE

    @Unique
    public float car$getTemperature() {
        return this.entityData.get(car$TEMPERATURE);
    }

    @Unique
    public void car$setTemperature(float temp) {
        this.entityData.set(car$TEMPERATURE, temp);
    }


    @Override
    public int getTanks() {
        return 1;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        Fluid fluid = car$getFluid();
        if (fluid == null) {
            return new FluidStack(ModFluids.BIO_DIESEL.get(), car$getFuel());
        } else {
            return new FluidStack(fluid, car$getFuel());
        }
    }

    @Override
    public int getTankCapacity(int tank) {
        return car$getMaxFuel();
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource == null || !car$isValidFuel(resource.getFluid())) {
            return 0;
        }

        if (car$getFluid() != null && car$getFuel() > 0 && !resource.getFluid().equals(car$getFluid())) {
            return 0;
        }

        int amount = Math.min(resource.getAmount(), car$getMaxFuel() - car$getFuel());

        if (action.execute()) {
            int i = car$getFuel() + amount;
            if (i > car$getMaxFuel()) {
                i = car$getMaxFuel();
            }
            car$setFuel(i);
            car$setFuelType(resource.getFluid());
        }

        return amount;
    }

    @Inject(
            method = "interact",
            at = @At("HEAD"),
            cancellable = true
    )
    private void car$gasSiphon(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir){
        if(player.isShiftKeyDown()) {
            if (!player.getItemInHand(hand).isEmpty()) {
                if (player.getItemInHand(hand).getItem() instanceof ItemCanister) {
                    boolean success = ItemCanister.fillCanister(player.getItemInHand(hand), this);

                    if (success) {
                        RefueledSounds.playSound(SoundEvents.BREWING_STAND_BREW, level(), blockPosition(), null, SoundSource.BLOCKS);
                    }
                    cir.setReturnValue(InteractionResult.CONSUME);
                }
            }
        }
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource == null) {
            return FluidStack.EMPTY;
        }

        if (resource.getFluid() == null || !resource.getFluid().equals(car$getFluid())) {
            return FluidStack.EMPTY;
        }

        return drain(resource.getAmount(), action);
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        Fluid fluid = car$getFluid();
        int totalAmount = car$getFuel();

        if (fluid == null) {
            return FluidStack.EMPTY;
        }

        int amount = Math.min(maxDrain, totalAmount);


        if (action.execute()) {
            int newAmount = totalAmount - amount;


            if (newAmount <= 0) {
                car$setFuelType((String) null);
                car$setFuel(0);
            } else {
                car$setFuel(newAmount);
            }
        }

        return new FluidStack(fluid, amount);
    }

    @Override
    public int getContainerSize() {
        return car$internalInventory.getContainerSize();
    }

    @Override
    public ItemStack getItem(int index) {
        return car$internalInventory.getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return car$internalInventory.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return car$internalInventory.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        car$internalInventory.setItem(index, stack);
    }

    @Override
    public int getMaxStackSize() {
        return car$internalInventory.getMaxStackSize();
    }

    @Override
    public void setChanged() {
        car$internalInventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return car$internalInventory.stillValid(player);
    }

    @Override
    public boolean isEmpty() {
        return car$internalInventory.isEmpty();
    }

    @Override
    public void startOpen(Player player) {
        car$internalInventory.startOpen(player);
    }

    @Override
    public void stopOpen(Player player) {
        car$internalInventory.stopOpen(player);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return car$internalInventory.canPlaceItem(index, stack);
    }

    @Override
    public void clearContent() {
        car$internalInventory.clearContent();
    }

    @Unique
    private void car$fuelTick() {
        int fuel = car$getFuel();
        int tickFuel = car$getEfficiency(car$getFluid());
        if (tickFuel <= 0) {
            return;
        }
        if (fuel > 0 && car$isAccelerating()) {
            if (tickCount % tickFuel == 0) {
                car$removeFuel(1);
            }
        } else if (fuel > 0 && car$isStarted()) {
            if (tickCount % (tickFuel * 100) == 0) {
                car$removeFuel(1);
            }
        }
    }

    @Unique
    private void car$removeFuel(int amount) {
        int fuel = car$getFuel();
        int newFuel = fuel - amount;
        car$setFuel(Math.max(newFuel, 0));
    }

    @Unique
    public boolean car$isAccelerating() {
        boolean b = (car$isForward() || car$isBackward()) && !horizontalCollision;
        return b && car$isStarted();
    }

    @Unique
    public boolean car$isStarted() {
        return entityData.get(car$STARTED);
    }

    @Unique
    private static boolean car$carStopped = false, car$carStarted = false;

    /**
     * Fail sound is only played when stopping the starting process
     */
    @Override
    public void car$setStarting(boolean starting, boolean playFailSound) {
        if (starting) {
            if (car$getBattery() <= 0) {
                return;
            }
            if (car$isStarted()) {
                car$setStarted(false, true, false);
                car$carStopped = true;
                return;
            }
        } else {
            if (car$carStarted || car$carStopped) {
                // TO prevent car from making stop start sound after releasing the starter key
                car$carStopped = false;
                car$carStarted = false;
                return;
            }
            if (playFailSound) {
                if (car$getBattery() > 0) {
                    car$playFailSound();
                }
            }
        }
        this.entityData.set(car$STARTING, starting);
    }

    @Unique
    public void car$setStarted(boolean started, boolean playStopSound, boolean playFailSound) {
        if (!started && playStopSound) {
            car$playStopSound();
        } else if (!started && playFailSound) {
            car$playFailSound();
        }
        this.entityData.set(car$STARTED, started);
    }

    @Unique
    public void car$playStopSound() {
        RefueledSounds.playSound(car$getStopSound(), level(), blockPosition(), null, SoundSource.MASTER, 1F);
    }

    @Unique
    public void car$playFailSound() {
        RefueledSounds.playSound(car$getFailSound(), level(), blockPosition(), null, SoundSource.MASTER, 1F, car$getBatterySoundPitchLevel());
    }

    @Unique
    public float car$getModifier() {
        BlockState state = getBlockStateOn();

        if (state.isAir() || isRoadBlock(state)) {
            return ServerConfig.onroadSpeed.get().floatValue();
        } else {
            return ServerConfig.offroadSpeed.get().floatValue();
        }
    }

    @Unique
    public float car$getRollResistance() {
        return 0.02F;
    }

    @Shadow(remap = false)
    @Final
    private static float SPEED;

    @Unique
    public float car$deltaRotation = 0;

    @Unique
    public void car$setForward(boolean forward) {
        this.entityData.set(car$FORWARD, forward);
    }

    @Unique
    public void car$setBackward(boolean backward) {
        this.entityData.set(car$BACKWARD, backward);
    }

    @Unique
    public void car$setLeft(boolean left) {
        this.entityData.set(car$LEFT, left);
    }

    @Unique
    public void car$setRight(boolean right) {
        this.entityData.set(car$RIGHT, right);
    }

    @Unique
    public Player car$getDriver() {
        List<Entity> passengers = getPassengers();
        if (passengers.size() <= 0) {
            return null;
        }

        if (passengers.get(0) instanceof Player) {
            return (Player) passengers.get(0);
        }

        return null;
    }

    @Unique
    public boolean car$isForward() {
        if (car$getDriver() == null || !car$canPlayerDriveCar(car$getDriver())) {
            return false;
        }
        return entityData.get(car$FORWARD);
    }

    @Unique
    public boolean car$isBackward() {
        if (car$getDriver() == null || !car$canPlayerDriveCar(car$getDriver())) {
            return false;
        }
        return entityData.get(car$BACKWARD);
    }

    @Unique
    public boolean car$isLeft() {
        if (car$getDriver() == null || !car$canPlayerDriveCar(car$getDriver())) {
            return false;
        }
        return entityData.get(car$LEFT);
    }

    @Unique
    public boolean car$isRight() {
        if (car$getDriver() == null || !car$canPlayerDriveCar(car$getDriver())) {
            return false;
        }
        return entityData.get(car$RIGHT);
    }

    @Unique
    private boolean car$collidedLastTick, car$startedLast;

    @Unique
    private void car$handleInput() {
        if (!isVehicle()) {
            car$setForward(false);
            car$setBackward(false);
            car$setLeft(false);
            car$setRight(false);
        }
        float turnMod = 1;
        float maxSp = car$getMaxSpeed() * car$getModifier();
        float maxBackSp = car$getMaxReverseSpeed() * car$getModifier();

        float speed = subtractToZero(car$getSpeed(), car$getRollResistance());

        if (car$isForward()) {
            if (speed <= maxSp) {
                speed = Math.min(speed + car$getAcceleration(), maxSp);
            }
        }

        if (car$isBackward()) {
            if (level().isClientSide) {
                turnMod = -1;
            }
            if (speed >= -maxBackSp) {
                speed = Math.max(speed - car$getAcceleration(), -maxBackSp);
            }
        }

        car$setSpeed(speed);

        float rotationSpeed = 0;
        if (Math.abs(speed) > 0.02F) {
            rotationSpeed = Mth.abs(car$getRotationModifier() / (float) Math.pow(speed, 2));

            rotationSpeed = Mth.clamp(rotationSpeed, car$getMinRotationSpeed(), car$getMaxRotationSpeed());
        }

        car$deltaRotation = 0;

        if (speed < 0) {
            rotationSpeed = -rotationSpeed;
        }

        if (car$isLeft()) {
            car$deltaRotation -= rotationSpeed;
        }
        if (car$isRight()) {
            car$deltaRotation += rotationSpeed;
        }

        if(level().isClientSide){
            car$rotateWheels(car$deltaRotation, rotationSpeed, speed, turnMod);
        }

        setYRot(getYRot() + car$deltaRotation);
        float delta = Math.abs(getYRot() - yRotO);
        while (getYRot() > 180F) {
            setYRot(getYRot() - 360F);
            yRotO = getYRot() - delta;
        }
        while (getYRot() <= -180F) {
            setYRot(getYRot() + 360F);
            yRotO = delta + getYRot();
        }

        if (horizontalCollision) {
            if (level().isClientSide && !car$collidedLastTick) {
                car$onCollision(speed);
                car$collidedLastTick = true;
            }
        } else {
            setDeltaMovement(car$calculateMotionX(car$getSpeed(), getYRot()), getDeltaMovement().y, car$calculateMotionZ(car$getSpeed(), getYRot()));
            if (level().isClientSide) {
                car$collidedLastTick = false;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Unique
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

    @OnlyIn(Dist.CLIENT)
    @Unique
    public void car$checkStartingLoop() {
        if (!car$isSoundPlaying(car$startingLoop)) {
            car$startingLoop = new RefueledStarting(this, car$getStartingSound(), SoundSource.MASTER);
            RefueledSounds.playSoundLoop(car$startingLoop, level());
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

    @OnlyIn(Dist.CLIENT)
    @Unique
    public void car$checkIdleLoop() {
        if (!car$isSoundPlaying(car$idleLoop)) {
            car$idleLoop = new RefueledIdle(this, car$getIdleSound(), SoundSource.MASTER);
            RefueledSounds.playSoundLoop(car$idleLoop, level());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Unique
    public void car$checkHighLoop() {
        if (!car$isSoundPlaying(car$highLoop)) {
            car$highLoop = new RefueledHigh(this, car$getHighSound(), SoundSource.MASTER);
            RefueledSounds.playSoundLoop(car$highLoop, level());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Unique
    public void car$checkStartLoop() {
        if (!car$isSoundPlaying(car$startLoop)) {
            car$startLoop = new RefueledStart(this, car$getStartSound(), SoundSource.MASTER);
            RefueledSounds.playSoundLoop(car$startLoop, level());
        }
    }

    @Unique
    public SoundEvent car$getCrashSound() {
        return RefueledSounds.CAR_CRASH.get();
    }

    @Shadow(remap = false)
    public Vec3 lastClientPos;
    @Shadow(remap = false)
    private int lerpSteps;
    @Unique
    private int car$timeToStart, car$timeSinceStarted;
    @Shadow(remap = false)
    private float targetYRot;
    @Shadow(remap = false)
    private double targetX, targetY, targetZ;

    @Override
    public void tick() {
        this.lastClientPos = this.position();

        super.tick();
        car$tickLerp();

        Runnable task;
        while ((task = car$tasks.poll()) != null) {
            task.run();
        }

        if (car$isStarted() && !car$canEngineStayOn()) {
            car$setStarted(false);
        }

        car$updateGravity();
        car$handleInput();

        move(MoverType.SELF, getDeltaMovement());

        car$updateWheelRotation();

        if (isInLava()) {
            car$addDamage(1);
        }

        if (car$isStarted() || (car$getHealth() / car$getMaxHealth()) * 100f < 1f) {
            car$particles();
        }

        car$fuelTick();

        if (level().isClientSide) {
            car$displaySpeed(car$getSpeed());
            car$updateSounds();
            if (car$isStarted()) {
                car$timeSinceStarted++;
                if (tickCount % 2 == 0) { //How often particles will spawn
                    car$spawnParticles(car$getSpeed() > 0.1F);
                    car$spawnParticles(car$getSpeed() > 0.1F);
                    if((Entity) this instanceof Motorcycle){
                        car$spawnParticles(car$getSpeed() > 0.1F);
                        car$spawnParticles(car$getSpeed() > 0.1F);
                    }
                    if((Entity) this instanceof SportCar){
                        car$spawnParticles(car$getSpeed() > 0.1F);
                        car$spawnParticles(car$getSpeed() > 0.1F);
                    }
                }
            } else {
                car$timeSinceStarted = 0;
            }
            return; //Important because car not going off bug
        }

        // SERVER SIDE
        if (car$isStarting()) {
            if (tickCount % 2 == 0) {
                car$setBattery(car$getBattery() - car$getBatteryUsage());
            }

            car$setStartingTime(car$getStartingTime() + 1);
            if (car$getBattery() <= 0) {
                car$setStarting(false, true);
            }
        } else {
            car$setStartingTime(0);
        }

        int time = car$getStartingTime();

        if (time > 0) { // prevent always calling gettimetostart
            if (car$timeToStart <= 0) {
                car$timeToStart = car$getTimeToStart();
            }

            if (time > car$getTimeToStart()) {
                car$startCarEngine();
                car$timeToStart = 0;
            }
        }

        if (car$isStarted()) {
            car$setStartingTime(0);
            car$carStarted = true;
            float speedPerc = car$getSpeed() / car$getMaxSpeed();

            int chargingRate = (int) (speedPerc * 7F);
            if (chargingRate < 5) {
                chargingRate = 1;
            }

            if (tickCount % 20 == 0) {
                car$setBattery(car$getBattery() + chargingRate);
            }
        }

        if (tickCount % 20 != 0) {
            return;
        }

        float speedPerc = car$getSpeed() / car$getMaxSpeed();

        int tempRate = (int) (speedPerc * 10F) + 1;

        if (tempRate > 5) {
            tempRate = 5;
        }

        float rate = tempRate * 0.2F + (random.nextFloat() - 0.5F) * 0.1F;

        float temp = car$getTemperature();

        float tempToReach = car$getTemperatureToReach();

        if (isInBounds(temp, tempToReach, rate)) {
            car$setTemperature(tempToReach);
        } else {
            if (tempToReach < temp) {
                rate = -rate;
            }
            car$setTemperature(temp + rate);
        }
    }

    @Unique
    private static double car$calculateMotionX(float speed, float rotationYaw) {
        return Mth.sin(-rotationYaw * 0.017453292F) * speed;
    }

    @Unique
    private static double car$calculateMotionZ(float speed, float rotationYaw) {
        return Mth.cos(rotationYaw * 0.017453292F) * speed;
    }

    @Unique
    public void car$onCollision(float speed) {
        if (level().isClientSide) {
            RefueledChannel.sendToServer(new VehicleCrash(this, speed));
        }
        car$setSpeed(0.01F);
        setDeltaMovement(0D, getDeltaMovement().y, 0D);

        float percSpeed = speed / car$getMaxSpeed();

        if (percSpeed > 0.8F) {
            car$addDamage(percSpeed * 5);
            car$playCrashSound();

            if (percSpeed > 0.9F) {
                car$setStarted(false);
                car$playStopSound();
            }
        }
    }

    @Unique
    public void car$setStarted(boolean started) {
        car$setStarting(false, false);

        car$setStarted(started, true, false);
    }

    @Unique
    public void car$playCrashSound() {
        RefueledSounds.playSound(car$getCrashSound(), level(), blockPosition(), null, SoundSource.MASTER, 1F);
    }

    @Unique
    public float car$getRotationModifier() {
        return 0.5F * 3f;
    }

    @Unique
    public boolean car$canPlayerDriveCar(Player player) {
        if (car$getFuel() <= 0) {
            return false;
        }

        if (player.equals(getControllingPassenger()) && car$isStarted()) {
            return true;
        } else if (isInWater() || isInLava()) {
            return false;
        } else {
            return false;
        }
    }

    @Unique
    public int car$getStartingTime() {
        return this.entityData.get(car$STARTING_TIME);
    }

    @Unique
    public void car$setStartingTime(int time) {
        this.entityData.set(car$STARTING_TIME, time);
    }

    @Unique
    public boolean car$isStarting() {
        return this.entityData.get(car$STARTING);
    }

    @Unique
    public int car$getTimeToStart() {
        int time = random.nextInt(10) + 5;

        float temp = car$getTemperature();
        if (temp < 0F) {
            time += 40;
        } else if (temp < 10F) {
            time += 35;
        } else if (temp < 30F) {
            time += 10;
        } else if (temp < 60F) {
            time += 5;
        }

        float batteryPerc = ((float) car$getBattery() / (float) car$getMaxBattery());

        if (batteryPerc < 0.5F) {
            time += 20 + random.nextInt(10);
        } else if (batteryPerc < 0.75F) {
            time += 10 + random.nextInt(10);
        }

        if (car$getHealth() < 5) {
            time += random.nextInt(25) + 50;
        } else if (car$getHealth() <= 10) {
            time += random.nextInt(15) + 30;
        } else if (car$getHealth() <= 20) {
            time += random.nextInt(15) + 10;
        } else if (car$getHealth() <= 50) {
            time += random.nextInt(10) + 5;
        }

        return time;
    }

    @Unique
    public float car$getTemperatureToReach() {
        float biomeTemp = car$getBiomeTemperatureCelsius();

        if (!car$isStarted()) {
            return biomeTemp;
        }
        float optimalTemp = car$getOptimalTemperature();

        if (biomeTemp > 45F) {
            optimalTemp = 100F;
        } else if (biomeTemp <= 0F) {
            optimalTemp = 80F;
        }
        return Math.max(biomeTemp, optimalTemp);
    }

    @Unique
    public float car$getBiomeTemperatureCelsius() {
        Biome biome = level().getBiome(blockPosition()).value();
        return (((IBiomeTempInvoke) (Object) biome).invokeGetTemperature(blockPosition()) - 0.3f) * 30f;
    }

    @Unique
    public float car$getOptimalTemperature() {
        return 90F;
    }

    @Unique
    public int car$getBatteryUsage() {
        if (!ServerConfig.useBattery.get()) {
            return 0;
        }

        float temp = car$getBiomeTemperatureCelsius();
        int baseUsage = 2;
        if (temp < 0F) {
            baseUsage += 2;
        } else if (temp < 15F) {
            baseUsage += 1;
        }
        return baseUsage;
    }

    @Unique
    public void car$startCarEngine() {
        Player player = (Player) getControllingPassenger();
        if (player != null && car$canStartCarEngine()) {
            car$setStarted(true);
        }
    }

    @Unique
    public boolean car$canStartCarEngine() {
        if (car$getFuel() <= 0) {
            return false;
        }

        if (car$getHealth() <= 0) {
            return false;
        }

        if (isInWater() || isInLava()) {
            return false;
        }

        return true;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity entity) {
        Direction direction = getMotionDirection();
        if (direction.getAxis() == Direction.Axis.Y) {
            return super.getDismountLocationForPassenger(entity);
        }
        int[][] offsets = DismountHelper.offsetsForDirection(direction);
        AABB bb = entity.getLocalBoundsForPose(Pose.STANDING);
        AABB carBB = getBoundingBox();
        for (int[] offset : offsets) {
            int i = this.getPassengers().size();
            Vec3 dismountPos = new Vec3(
                getX() + car$getDismountLocations(offset[0], carBB, bb)[i],
                getY(),
                getZ() + car$getDismountLocations(offset[1], carBB, bb)[i]);

            double y = level().getBlockFloorHeight(new BlockPos((int) dismountPos.x, (int) dismountPos.y, (int) dismountPos.z));
            if (DismountHelper.isBlockFloorValid(y)) {
                if (DismountHelper.canDismountTo(level(), entity, bb.move(dismountPos))) {
                    return dismountPos;
                }
            }
        }
        ((IHeightAccess) entity).setEyeHeight(getEyeHeight(Pose.STANDING));
        return super.getDismountLocationForPassenger(entity);
    }

    @Shadow(remap = false)
    public SimpleContainer inventory;

    @Override
    public boolean canBeHitByProjectile(){
        return this.isAlive();
    }

    @Override
    public boolean hurt(DamageSource damageSource, float damage) {
        if (this.isInvulnerable() || level().isClientSide || !isAlive()) {
            return false;
        }

        if (!(damageSource.getEntity() instanceof Player)) {
            return false;
        }
        Player player = (Player) damageSource.getEntity();

        if (player == null) {
            return false;
        }

        if (getPassengers().stream().anyMatch(player::equals)) {
            return false;
        }

        if (!this.level().isClientSide && !this.isRemoved()) {
            this.markHurt();
            this.gameEvent(GameEvent.ENTITY_DAMAGE);
            float health = this.entityData.get(HEALTH) - damage;
            this.entityData.set(HEALTH, health);

            if (health < 0) {
                if(ServerConfig.explodeOnDeath.get()) {
                    level().explode(null, getX(), getY(), getZ(), 2f + 4f * ((float) car$getFuel() / (float) car$getMaxFuel()), Level.ExplosionInteraction.BLOCK);
                }
                Containers.dropContents(this.level(), this, this.inventory);
                Containers.dropContents(this.level(), this, car$internalInventory);
                this.kill();
            }
        }
        return true;
    }

    // Engine sound related stuff

    @Unique
    public float car$getBatterySoundPitchLevel() {

        int batteryLevel = car$getBattery();

        int startLevel = car$getMaxBattery() / 3;

        float basePitch = 1F - 0.002F * ((float) car$getStartingTime());

        if (batteryLevel > startLevel) {
            return basePitch;
        }

        int levelUnder = startLevel - batteryLevel;

        float perc = (float) levelUnder / (float) startLevel;

        return basePitch - (perc / 2.3F);
    }

    @Unique
    private final BlockingQueue<Runnable> car$tasks = new LinkedBlockingQueue<>();

    @Override
    public boolean canCollideWith(Entity entity) {
        if (!level().isClientSide && ServerConfig.damageEntities.get() && entity instanceof LivingEntity && !getPassengers().contains(entity)) {
            if (entity.getBoundingBox().intersects(getBoundingBox())) {
                float altSpeed = car$getSpeed();

                if (altSpeed > 0.35F) {
                    float damage = altSpeed * car$getRamDamage();
                    car$tasks.add(() -> {
                        entity.hurt(((IDmgSourceInvoke) level().damageSources()).invokeSource(RefueledRegistry.CAR_DAMAGE_TYPE), damage);
                    });
                }
            }
        }

        if (!ServerConfig.collideWithEntities.get()) {
            if (!isCar(entity)) {
                return false;
            }
        }

        return (entity.canBeCollidedWith() || entity.isPushable()) && !isPassengerOfSameVehicle(entity);
    }


    @Unique
    public float car$clientPitch;

    @Unique
    private void car$tickLerp() {
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
            setXRot((float) ((double) getXRot() + (car$clientPitch - (double) getXRot()) / (double) lerpSteps));
            --lerpSteps;
            setPos(d0, d1, d2);
            setRot(getYRot(), getXRot());
        }
    }

    @Unique
    public boolean car$canEngineStayOn() {
        if (car$getFuel() <= 0 || isInLava() || car$getHealth() <= 0) {
            return false;
        }

        if (isInWater()) {
            car$addDamage(25);
            return false;
        }

        return true;
    }

    @Unique
    private void car$updateGravity() {
        if (isNoGravity()) {
            setDeltaMovement(getDeltaMovement().x, 0D, getDeltaMovement().z);
            return;
        }
        setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y - 0.2D, getDeltaMovement().z);
    }

    @Unique
    public void car$spawnParticles(boolean driving) {
        if (!level().isClientSide) {
            return;
        }
        Vec3 lookVec = getLookAngle().normalize();
        double lookAngle = getYRot() < 0 ? 360 + getYRot() : getYRot();

        int rand = random.nextInt(4);
        double radius = car$getExhaust(rand)[0], angle = car$getExhaust(rand)[1];

        double offX = Math.sin(Math.toRadians(lookAngle)) + radius * Math.sin(angle + Math.toRadians(lookAngle)); // offX is equal to the vertical vector on a 2d plane
        double offY = car$getExhaust(rand)[2]; // slightly elevate the exhaust
        double offZ = Math.cos(Math.toRadians(lookAngle)) * -1D - radius * Math.cos(angle + Math.toRadians(lookAngle)); // and for offZ, it's equal to the horizontal vector


        // Engine started smoke should only come 1 second after start and only if the
        // engine is colder than 50°C
        if (car$timeSinceStarted > 0 && car$timeSinceStarted < 20 && car$getTemperature() < 50F) {
            double speedX = lookVec.x * -0.1D;
            double speedZ = lookVec.z * -0.1D;

            int health = (int) ((car$getHealth() / car$getMaxHealth()) * 100f);
            int count = 1;
            double r = 0.1;

            if (health < 10) {
                count = 6;
                r = 0.7;
            } else if (health < 25) {
                count = 3;
                r = 0.7;
            } else if (health < 50) {
                count = 2;
                r = 0.3;
            }
            for (int i = 0; i <= count; i++) {
                car$spawnParticle(ParticleTypes.LARGE_SMOKE, offX, offY, offZ, speedX, speedZ, r);
            }
        } else if (driving) {
            double speedX = lookVec.x * -0.2D;
            double speedZ = lookVec.z * -0.2D;
            car$spawnParticle(ParticleTypes.SMOKE, offX, offY, offZ, speedX, speedZ);
        } else {
            double speedX = lookVec.x * -0.05D;
            double speedZ = lookVec.z * -0.05D;
            car$spawnParticle(ParticleTypes.SMOKE, offX, offY, offZ, speedX, speedZ);
        }

    }

    @Unique
    private void car$spawnParticle(ParticleOptions particleTypes, double offX, double offY, double offZ, double speedX, double speedZ, double r) {
        level().addParticle(particleTypes,
                getX() + offX + (random.nextDouble() * r - r / 2D),
                getY() + offY + (random.nextDouble() * r - r / 2D) + getBbHeight() / 8F,
                getZ() + offZ + (random.nextDouble() * r - r / 2D),
                speedX, 0.0D, speedZ);
    }

    @Unique
    private void car$spawnParticle(ParticleOptions particleTypes, double offX, double offY, double offZ, double speedX, double speedZ) {
        car$spawnParticle(particleTypes, offX, offY, offZ, speedX, speedZ, 0.1D);
    }

    @Unique
    public void car$particles() {
        if (!level().isClientSide) {
            return;
        }
        int health = (int) (car$getHealth() / car$getMaxHealth() * 100f);

        if (health > 50) {
            return; // Don't render damage particles if car is above 50% health
        }

        int amount;

        if (health > 30) {
            if (random.nextInt(10) != 0) {
                return; // If between 50% and 30%, render a particle 10% of the time
            }
            amount = 1;
        } else if (health > 20) {
            if (random.nextInt(5) != 0) {
                return; // If between 30% and 20%, render a particle 20% of the time
            }
            amount = 1;
        } else if (health > 10) {
            amount = 2; // If between 20% and 10%, render 2 particles every tick
        } else {
            amount = 3; // If below 10%, render 3 particles every tick
        }

        for (int i = 0; i < amount; i++) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    0.0D, 0.0D, 0.0D);
        }

    }

    @Unique
    public float car$wheelRotation;

    @Unique
    public float car$getWheelRotationAmount() {
        return ((25.5f / 2f) * 0.8f) * car$getSpeed(); // 25.5 is the wheel height, 0.8 is scale factor
    }

    @Unique
    public void car$updateWheelRotation() {
        car$wheelRotation += car$getWheelRotationAmount();
    }

    @Override
    public float car$getWheelRotation(float partialTicks) {
        return car$wheelRotation + car$getWheelRotationAmount() * partialTicks;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Unique
    public boolean car$isValidFuel(Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        return car$getEfficiency(fluid) > 0;
    }

    @Unique
    public void car$initTemperature() {
        car$setTemperature(car$getBiomeTemperatureCelsius());
    }

    @Unique
    public void car$updateControls(boolean forward, boolean backward, boolean left, boolean right, Player player) {
        boolean needsUpdate = false;

        if (car$isForward() != forward) {
            car$setForward(forward);
            needsUpdate = true;
        }

        if (car$isBackward() != backward) {
            car$setBackward(backward);
            needsUpdate = true;
        }

        if (car$isLeft() != left) {
            car$setLeft(left);
            needsUpdate = true;
        }

        if (car$isRight() != right) {
            car$setRight(right);
            needsUpdate = true;
        }
        if (level().isClientSide && needsUpdate) {
            RefueledChannel.sendToServer(new ControlVehicle(forward, backward, left, right, player));
        }
    }

    @Unique
    public void car$centerCar() {
        Direction facing = getDirection();
        switch (facing) {
            case SOUTH:
                setYRot(0F);
                break;
            case NORTH:
                setYRot(180F);
                break;
            case EAST:
                setYRot(-90F);
                break;
            case WEST:
                setYRot(90F);
                break;
        }
    }

    @Unique
    public void car$openCarGUI(Player player) {
        if (level().isClientSide) {
            RefueledChannel.sendToServer(new VehicleGUI(player));
        }

        if (!level().isClientSide && player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return CarMixin.this.getDisplayName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new CarGUI(i, playerInventory, CarMixin.this);
                }
            }, packetBuffer -> packetBuffer.writeUUID(getUUID()));
        }
    }

    @Unique
    private static float car$speedMod = (1 + SPEED);

    /**
     * maxSpeed{
     * body:
     *     bodyBigWood      = 0.85
     *     bodyWooden       = 0.9
     *     bodySport        = 1.2
     *     bodySUV          = 0.8
     *     bodyTransporter  = 0.8
     *     ----------------------
     *     engineV3     = 0.75
     *     engineV6     = 0.9
     *     engineTruck  = 0.65
     * }
     * engineMaxReverseSpeed{
     *     v3       = 0.2
     *     v6       = 0.25
     *     truck    = 0.15
     * }
     * acceleration{
     *     bodyBigWood      = 0.95
     *     bodyWooden       = 1
     *     bodySport        = 1
     *     bodySUV          = 0.8
     *     bodyTransporter  = 0.8
     *     -----------------------
     *     engineV3     = 0.04
     *     engineV6     = 0.03
     *     engineTruck  = 0.035
     * }
     * fuelEfficiency{
     *     bodyBigWood      = 0.7
     *     bodyWooden       = 0.8
     *     bodySport        = 0.9
     *     bodySUV          = 0.6
     *     bodyTransporter  = 0.6
     *     ----------------------
     *     engineV3     = 0.5
     *     engineV6     = 0.25
     *     engineTruck  = 0.8
     * fuel:
     *     bioDiesel    = 100[%]
     * }
     * Modern: Big Wood body, V3 engine
     * Classic: Wooden body, V3 engine
     * Truck: Transporter body, Truck engine
     * SUV: SUV body, Truck engine
     * Sport: Sport body, V6 engine
     * Bike: Sport body, V3 engine
     */
    @Mixin(Car.class)
    private abstract static class ModernMixin implements ICarInvoker {

        @ModifyConstant(
                method = "defineSynchedData",
                constant = @Constant(floatValue = 20f)
        )
        private float car$increaseHealth(float original) {
            return original * 5f;
        }

        public float car$getMaxSpeed() {
            // bodyMaxSpeed * engineMaxSpeed * DragN's maxSpeed
            return 0.85f * 0.75f * car$speedMod;
        }

        public float car$getMaxReverseSpeed() {
            // engineMaxReverseSpeed * DragN's maxSpeed
            return 0.2f * car$speedMod;
        }

        public float car$getAcceleration() {
            // bodyAcceleration * engineAcceleration * DragN's maxSpeed
            return 0.04f * 0.95f * car$speedMod;
        }

        public int car$getEfficiency(@Nullable Fluid fluid) {
            // bodyEfficiency * engineEfficiency * fuelEfficiency
            return (int) Math.ceil(ServerConfig.modernFuelEff.get().floatValue() * getFuelEfficiency(fluid));
        }

        @Inject(
            method = "getStepHeight",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
        )
        private void car$modifyStepHeight(CallbackInfoReturnable<Float> cir){
            cir.setReturnValue(ServerConfig.modernStepHeight.get().floatValue());
        }
    }

    @Mixin(Classic.class)
    private abstract static class ClassicMixin implements ICarInvoker {
        @ModifyConstant(
                method = "defineSynchedData",
                constant = @Constant(floatValue = 20f)
        )
        private float car$increaseHealth(float original) {
            return original * 5f;
        }

        public float car$getMaxSpeed() {
            return 0.9f * 0.75f * car$speedMod;
        }

        public float car$getMaxReverseSpeed() {
            return 0.2f * car$speedMod;
        }

        public float car$getAcceleration() {
            return 0.04f * 1f * car$speedMod;
        }

        public int car$getEfficiency(@Nullable Fluid fluid) {
            return (int) Math.ceil(ServerConfig.classicFuelEff.get().floatValue() * getFuelEfficiency(fluid));
        }

        @Inject(
            method = "getStepHeight",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
        )
        private void car$modifyStepHeight(CallbackInfoReturnable<Float> cir){
            cir.setReturnValue(ServerConfig.classicStepHeight.get().floatValue());
        }

        public float car$getRamDamage(){
            return ServerConfig.classicRamDamage.get().floatValue();
        }
    }

    @Mixin(Truck.class)
    private abstract static class TruckMixin implements ICarInvoker{
        @ModifyConstant(
                method = "defineSynchedData",
                constant = @Constant(floatValue = 25f)
        )
        private float car$increaseHealth(float original) {
            return original * 5f;
        }

        public float car$getMaxSpeed() {
            return 0.8f * 0.65f * car$speedMod;
        }

        public float car$getMaxReverseSpeed() {
            return 0.15f * car$speedMod;
        }

        public float car$getAcceleration() {
            return 0.035f * 0.8f * car$speedMod;
        }

        public int car$getEfficiency(@Nullable Fluid fluid) {
            return (int) Math.ceil(ServerConfig.truckFuelEff.get().floatValue() * getFuelEfficiency(fluid));
        }

        @Inject(
            method = "getStepHeight",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
        )
        private void car$modifyStepHeight(CallbackInfoReturnable<Float> cir){
            cir.setReturnValue(ServerConfig.truckStepHeight.get().floatValue());
        }

        public float car$getPitch() {
            return 1f + 0.35f * Math.abs(car$getSpeed()) / car$getMaxSpeed();
        }

        public SoundEvent car$getStopSound() {
            return RefueledSounds.TRUCK_ENGINE_STOP.get();
        }

        public SoundEvent car$getFailSound() {
            return RefueledSounds.TRUCK_ENGINE_FAIL.get();
        }

        public SoundEvent car$getStartSound() {
            return RefueledSounds.TRUCK_ENGINE_START.get();
        }

        public SoundEvent car$getStartingSound() {
            return RefueledSounds.TRUCK_ENGINE_STARTING.get();
        }

        public SoundEvent car$getIdleSound() {
            return RefueledSounds.TRUCK_ENGINE_IDLE.get();
        }

        public SoundEvent car$getHighSound() {
            return RefueledSounds.TRUCK_ENGINE_HIGH.get();
        }

        public float car$getRamDamage(){
            return ServerConfig.truckRamDamage.get().floatValue();
        }

        public float car$getMaxHealth(){
            return 125f;
        }
    }

    @Mixin(SUV.class)
    private abstract static class SUVMixin implements ICarInvoker{
        @ModifyConstant(
                method = "defineSynchedData",
                constant = @Constant(floatValue = 25f)
        )
        private float car$increaseHealth(float original) {
            return original * 5f;
        }

        public float car$getMaxSpeed() {
            return 0.8f * 0.65f * car$speedMod;
        }

        public float car$getMaxReverseSpeed() {
            return 0.15f * car$speedMod;
        }

        public float car$getAcceleration() {
            return 0.035f * 0.8f * car$speedMod;
        }

        public int car$getEfficiency(@Nullable Fluid fluid) {
            return (int) Math.ceil(ServerConfig.suvFuelEff.get().floatValue() * getFuelEfficiency(fluid));
        }

        @Inject(
            method = "getStepHeight",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
        )
        private void car$modifyStepHeight(CallbackInfoReturnable<Float> cir){
            cir.setReturnValue(ServerConfig.suvStepHeight.get().floatValue());
        }

        public float car$getPitch() {
            return 1f + 0.35f * Math.abs(car$getSpeed()) / car$getMaxSpeed();
        }

        public SoundEvent car$getStopSound() {
            return RefueledSounds.TRUCK_ENGINE_STOP.get();
        }

        public SoundEvent car$getFailSound() {
            return RefueledSounds.TRUCK_ENGINE_FAIL.get();
        }

        public SoundEvent car$getStartSound() {
            return RefueledSounds.TRUCK_ENGINE_START.get();
        }

        public SoundEvent car$getStartingSound() {
            return RefueledSounds.TRUCK_ENGINE_STARTING.get();
        }

        public SoundEvent car$getIdleSound() {
            return RefueledSounds.TRUCK_ENGINE_IDLE.get();
        }

        public SoundEvent car$getHighSound() {
            return RefueledSounds.TRUCK_ENGINE_HIGH.get();
        }

        public float car$getRamDamage(){
            return ServerConfig.suvRamDamage.get().floatValue();
        }

        public float car$getMaxHealth(){
            return 125f;
        }
    }

    @Mixin(SportCar.class)
    private abstract static class SportMixin implements ICarInvoker {
        @ModifyConstant(
                method = "defineSynchedData",
                constant = @Constant(floatValue = 20f)
        )
        private float car$increaseHealth(float original) {
            return original * 4f;
        }

        public float car$getMaxSpeed() {
            return 1.2f * 0.9f * car$speedMod;
        }

        public float car$getMaxReverseSpeed() {
            return 0.25f * car$speedMod;
        }

        public float car$getAcceleration() {
            return 0.03f * 1f * car$speedMod;
        }

        public int car$getEfficiency(@Nullable Fluid fluid) {
            return (int) Math.ceil(ServerConfig.sportFuelEff.get().floatValue() * getFuelEfficiency(fluid));
        }

        @Inject(
            method = "getStepHeight",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
        )
        private void car$modifyStepHeight(CallbackInfoReturnable<Float> cir){
            cir.setReturnValue(ServerConfig.sportStepHeight.get().floatValue());
        }

        public Vec3[] car$getSeatPositions(){
            Vec3[] seatPos = new Vec3[4];

            seatPos[0] = new Vec3(0.6 * 0.9, 0.1 * 0.8, -0.2 * 0.9);
            seatPos[1] = new Vec3(-0.6 * 0.9, 0.1 * 0.8, -0.2 * 0.9);
            seatPos[2] = new Vec3(0.6 * 0.9, 0.1 * 0.8, -2.0 * 0.9);
            seatPos[3] = new Vec3(-0.6 * 0.9, 0.1 * 0.8, -2.0 * 0.9);

            return seatPos;
        }

        public SoundEvent car$getStopSound() {
            return RefueledSounds.SPORT_ENGINE_STOP.get();
        }

        public SoundEvent car$getFailSound() {
            return RefueledSounds.SPORT_ENGINE_FAIL.get();
        }

        public SoundEvent car$getStartSound() {
            return RefueledSounds.SPORT_ENGINE_START.get();
        }

        public SoundEvent car$getStartingSound() {
            return RefueledSounds.SPORT_ENGINE_STARTING.get();
        }

        public SoundEvent car$getIdleSound() {
            return RefueledSounds.SPORT_ENGINE_IDLE.get();
        }

        public SoundEvent car$getHighSound() {
            return RefueledSounds.SPORT_ENGINE_HIGH.get();
        }

        public float car$getRamDamage(){
            return ServerConfig.sportRamDamage.get().floatValue();
        }

        public float car$getMaxHealth(){
            return 80f;
        }

        public double[] car$getExhaust(int rand){
            double[] modX = new double[]{1D, -1D, 1D, -1D};
            double radius = Math.sqrt((2.65D - 1D) * (2.65D - 1D) + (0.8D - 0) * (0.8D - 0));                             // calculates distance from entity center to exhaust point
            double pointDist = Math.sqrt((2.65D - (1D + radius)) * (2.65D - (1D + radius)) + (0.8D - 0) * (0.8D - 0));    // calculates distance from exhaust point to current entity viewing point
            double angle = 2 * Math.asin(0.5 * pointDist / radius) * modX[rand];

            return new double[]{radius, angle, 0.05D};
        }
    }

    @Mixin(Motorcycle.class)
    private abstract static class BikeMixin implements ICarInvoker{
        @ModifyConstant(
                method = "defineSynchedData",
                constant = @Constant(floatValue = 20f)
        )
        private float car$increaseHealth(float original) {
            return original * 3f;
        }

        @Override
        public float car$getMaxSpeed() {
            return 1.2f * 0.75f * car$speedMod;
        }

        @Override
        public float car$getMaxReverseSpeed() {
            return 0.2f * car$speedMod;
        }

        @Override
        public float car$getAcceleration() {
            return 0.04f * 1f * car$speedMod;
        }

        public int car$getEfficiency(Fluid fluid) {
            return (int) Math.ceil(ServerConfig.bikeFuelEff.get().floatValue() * getFuelEfficiency(fluid));
        }

        @Inject(
            method = "getStepHeight",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
        )
        private void car$modifyStepHeight(CallbackInfoReturnable<Float> cir){
            cir.setReturnValue(ServerConfig.bikeStepHeight.get().floatValue());
        }

        @Override
        public double[] car$getExhaust(int rand){
            double[] modX = new double[]{1d, -1d, 1d, -1d};
            double[] randomOffY = new double[]{0.7d, 0.7d, 0.7D - 0.275D, 0.7D - 0.275D};
            double[] modY = new double[]{1.5d, 1.5d, 1.5D - 0.225D, 1.5D - 0.225D};

            double radius = Math.sqrt((modY[rand] - 1D) * (modY[rand] - 1D) + (0.3D - 0) * (0.3D - 0));
            double pointDist = Math.sqrt((modY[rand] - (1D + radius)) * (modY[rand] - (1D + radius)) + (0.3D - 0) * (0.3D - 0));
            double angle = (2 * Math.asin(0.5 * pointDist / radius)) * modX[rand];

            return new double[]{radius, angle, randomOffY[rand]};
        }

        public Vec3[] car$getSeatPositions(){
            Vec3[] seatPos = new Vec3[1];

            seatPos[0] = new Vec3(0 * 0.8, 0.8 * 0.7, -0.5 * 0.8);

            return seatPos;
        }

        public float car$getRamDamage(){
            return ServerConfig.bikeRamDamage.get().floatValue();
        }

        public float car$getMaxRotationSpeed(){
            return 5f;
        }

        public float car$getMaxHealth(){
            return 60f;
        }
    }

}