package bogdan.refueled.mixin.common.car;

import bogdan.refueled.RefueledRegistry;
import bogdan.refueled.common.gui.TruckGUI;
import bogdan.refueled.common.network.*;
import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.common.gui.CarGUI;
import bogdan.refueled.config.ServerConfig;
import bogdan.refueled.mixin.common.accessor.IBiomeAccess;
import bogdan.refueled.mixin.common.accessor.IEntityAccess;
import bogdan.refueled.mixin.common.accessor.IHeightAccess;
import com.dragn0007.dragnvehicles.registry.ItemRegistry;
import com.dragn0007.dragnvehicles.vehicle.car.Car;
import com.dragn0007.dragnvehicles.vehicle.classic.Classic;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCar;
import com.dragn0007.dragnvehicles.vehicle.suv.SUV;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
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
import static bogdan.refueled.server.PlayerEvents.REFUELED_KEY;

@Mixin(value = {Car.class, Classic.class, Truck.class, SUV.class, SportCar.class, Motorcycle.class})
public abstract class CarMixin extends Entity implements IVehicleAccess, MenuProvider {
    public CarMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /*
     * Any references to this abstract CarMixin class gets replaced with a class reference to the currently mix-ined class
     * i.e: Current mixined class is 'Truck.class'
     * then CarMixin.this, CarMixin.class -> Truck.this, Truck.class
     */

    /**
     * todo:
     *     - !!! Add paint markings and item
     *     - ! add lighting to vehicles
     *     - ! make the recipes conditional
     *     - ! add a crafting recipe allowing for energy/fluid transfer using the battery/canister
     *     - Add 2 collision PartEntity hitboxes to front and back of the vehicles
     *     - update the sloping system block collection
     *     - Implement a way to keep the vehicles locked
     *     - model custom slot and 2 states, one with key and other without
     *     - render the cars glovebox locked (disabled slots)
     **/

    @Unique
    private final static int refuel$type = List.of(Car.class, Classic.class, Truck.class, SUV.class, SportCar.class, Motorcycle.class).indexOf(CarMixin.class);

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void refuel$addInit(EntityType<?> entityType, Level level, CallbackInfo ci) {
        refuel$internalInventory = new SimpleContainer(27);
        refuel$lazyFluid = LazyOptional.of(() -> new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Nonnull
            @Override
            public FluidStack getFluidInTank(int tank) {
                return new FluidStack(refuel$getFluid(), refuel$getFuel());
            }

            @Override
            public int getTankCapacity(int tank) {
                return refuel$getMaxFuel();
            }

            @Override
            public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
                return ServerConfig.getFuelEfficiency(stack.getFluid()) > 0;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                Fluid fluid = refuel$getFluid();
                if (resource == null || (fluid != Fluids.EMPTY && !resource.getFluid().equals(refuel$getFluid())) || ServerConfig.getFuelEfficiency(resource.getFluid()) < 0) {
                    return 0;
                }
                var fluidKey = ForgeRegistries.FLUIDS.getKey(resource.getFluid());
                if (fluidKey == null) return 0;

                int maxFuel = refuel$getMaxFuel();
                int amount = Math.min(resource.getAmount(), maxFuel - refuel$getFuel());

                if (action.execute()) {
                    int i = refuel$getFuel() + amount;
                    if (i > maxFuel) i = maxFuel;
                    if (fluid == Fluids.EMPTY) entityData.set(refuel$FUEL_TYPE, fluidKey.toString());
                    refuel$setFuel(i);
                }

                return amount;
            }

            @Nonnull
            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                Fluid fluid = refuel$getFluid();
                if (fluid == Fluids.EMPTY || resource == null || resource.getFluid() == null || !resource.getFluid().equals(fluid)) {
                    return FluidStack.EMPTY;
                }

                return drain(resource.getAmount(), action);
            }

            @Nonnull
            @Override
            public FluidStack drain(int toDrain, FluidAction action) {
                Fluid fluid = refuel$getFluid();
                if (fluid == Fluids.EMPTY) return FluidStack.EMPTY;

                int actuallyDrained = Math.min(toDrain, refuel$getFuel());
                if (action.execute())
                    refuel$setFuel(refuel$getFuel() - actuallyDrained);

                return new FluidStack(fluid, actuallyDrained);
            }
        });
        refuel$lazyEnergy = LazyOptional.of(() -> new IEnergyStorage() {
            @Override
            public int receiveEnergy(int amount, boolean simulate) {
                if (amount < 0) {
                    return 0;
                }
                int input = Math.min(refuel$getMaxBattery() - refuel$getBattery(), amount);
                if (!simulate) {
                    refuel$setBattery(refuel$getBattery() + input);
                }

                return input;
            }

            @Override
            public int extractEnergy(int amount, boolean simulate) {
                if (amount < 0) {
                    return 0;
                }
                int extracted = Math.min(refuel$getBattery(), amount);
                if (!simulate) {
                    refuel$setBattery(refuel$getBattery() - extracted);
                }
                return extracted;
            }

            @Override
            public int getEnergyStored() {
                return refuel$getBattery();
            }

            @Override
            public int getMaxEnergyStored() {
                return refuel$getMaxBattery();
            }

            @Override
            public boolean canExtract() {
                return true;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        });
    }

    @Inject(
            method = "interact*",
            at = @At("HEAD"),
            cancellable = true
    )
    private void refuel$injectMoreInteraction(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack item = player.getItemInHand(hand);
        if (isCar(item.getItem()) || item.getItem() instanceof BucketItem) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        if(player.isShiftKeyDown() && (item.getItem() == Items.SHEARS || item.getTags().anyMatch(tag -> tag.location().toString().equals("forge:tools/wrench")))){
            refuel$convertIntoItems();
            ItemStack egg = refuel$getEgg();
            if(player.getInventory().add(egg))
                player.drop(egg, true);

            this.remove(RemovalReason.DISCARDED);
            cir.setReturnValue(InteractionResult.sidedSuccess(level().isClientSide));
            return;
        }

        if(item.getItem() == Items.NAME_TAG){
            this.setCustomName(item.getHoverName());
            item.shrink(1);
            cir.setReturnValue(InteractionResult.sidedSuccess(level().isClientSide));
            return;
        }

        if (player.isShiftKeyDown() && item.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
            IFluidHandler otherHandler = item.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().get(),
                    handler = getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().get();
            FluidStack fluidStack = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE),
                    otherFluidStack = otherHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);

            // If there's anything in the otherHandler
            if (!otherFluidStack.isEmpty()) {
                // and if there's any space left in our handler
                if (handler.fill(new FluidStack(fluidStack, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE) > 0) {
                    otherHandler.drain(handler.fill(otherFluidStack, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    cir.setReturnValue(InteractionResult.sidedSuccess(level().isClientSide));
                }
                // otherwise assume it's full and drain
                else {
                    handler.drain(otherHandler.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    cir.setReturnValue(InteractionResult.sidedSuccess(level().isClientSide));
                }
            }
            // If the otherHandler is empty and our handler contains anything
            else if (!fluidStack.isEmpty()) {
                handler.drain(otherHandler.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                cir.setReturnValue(InteractionResult.sidedSuccess(level().isClientSide));
            } else cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Shadow(remap = false)
    protected abstract Vec3 calcOffset(double x, double y, double z);

    @Override
    public void positionRider(@NotNull Entity entity, @NotNull MoveFunction moveFunction) {
        int i = this.getPassengers().indexOf(entity);
        if(i == -1) return;
        entity.setPos(this.calcOffset(refuel$getSeatPositions()[i].x, refuel$getSeatPositions()[i].y, refuel$getSeatPositions()[i].z));

        float sizeMod = (Entity) this instanceof Motorcycle ? sizeFactor.floatValue() * 1.33f : sizeFactor.floatValue(),
                offset = entity.getDimensions(Pose.SITTING).width * 0.5f * (1f - sizeMod);
        AABB oldBB = entity.getBoundingBox();
        // Legs take up 37.5% of the model, however the bounding box is only 96% of the actual model scaled down to 93.75%
        entity.setBoundingBox(new AABB(
                        oldBB.minX + offset, oldBB.minY + entity.getBbHeight() * (0.375 * 0.9375) * sizeMod, oldBB.minZ + offset,
                        oldBB.maxX - offset, oldBB.maxY - entity.getBbHeight() * (1d - sizeMod), oldBB.maxZ - offset
                )
        );
        ((IHeightAccess) entity).setEyeHeight(entity.getEyeHeight(Pose.SITTING) * sizeMod);

        entity.setYRot(entity.getYRot() + refuel$deltaRotation);
        entity.setYHeadRot(entity.getYHeadRot() + this.refuel$deltaRotation);
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entityToUpdate) {
        if(ServerConfig.correctYaw.get())
            refuel$applyYawToEntity(entityToUpdate);
    }

    @Unique
    public void refuel$applyYawToEntity(Entity entityToUpdate) {
        entityToUpdate.setYBodyRot(getYRot());
        float f = Mth.wrapDegrees(entityToUpdate.getYRot() - getYRot());
        float f1;
        if(ServerConfig.restrictYaw.get())
            f1 = Mth.clamp(f, -135.0F, 135.0F);
        else f1 = f;
        entityToUpdate.yRotO += f1 - f;
        entityToUpdate.setYRot(entityToUpdate.getYRot() + f1 - f);
        entityToUpdate.setYHeadRot(entityToUpdate.getYRot());
    }


    // ENTITYDATA DEFINERS
    // Server -> Client synced data
    @Unique
    private static final EntityDataAccessor<String>
            refuel$FUEL_TYPE = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.STRING);

    @Unique
    private static final EntityDataAccessor<Float>
            refuel$TEMPERATURE = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.FLOAT),
            refuel$SPEED = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.FLOAT);

    @Unique
    private static final EntityDataAccessor<Integer>
            refuel$FUEL_AMOUNT = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.INT),
            refuel$BATTERY_LEVEL = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.INT),
            refuel$STARTING_TIME = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.INT);

    @Unique
    private static final EntityDataAccessor<Boolean>
            refuel$STARTING = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            refuel$STARTED = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            refuel$FORWARD = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            refuel$BACKWARD = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            refuel$LEFT = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN),
            refuel$RIGHT = SynchedEntityData.defineId(CarMixin.class, EntityDataSerializers.BOOLEAN);

    @Inject(
            method = "addAdditionalSaveData",
            at = @At("TAIL")
    )
    private void refuel$saveDataToNBT(CompoundTag tag, CallbackInfo ci) {
        tag.putFloat("temperature", entityData.get(refuel$TEMPERATURE));
        tag.putInt("energy", entityData.get(refuel$BATTERY_LEVEL));
        saveInventory(tag, "internalInventory", refuel$internalInventory);
        tag.put("Fluid", new FluidStack(refuel$getFluid(), refuel$getFuel()).writeToNBT(new CompoundTag()));
    }

    @Inject(
            method = "readAdditionalSaveData",
            at = @At("TAIL")
    )
    private void refuel$readDataFromNBT(CompoundTag tag, CallbackInfo ci) {
        if(tag.contains("temperature"))
            entityData.set(refuel$TEMPERATURE, tag.getFloat("temperature"));
        if(tag.contains("energy"))
            entityData.set(refuel$BATTERY_LEVEL, tag.getInt("energy"));
        if(tag.contains("internalInventory"))
            readInventory(tag, "internalInventory", refuel$internalInventory);

        if(tag.contains("Fluid")){
            var temp = FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid"));
            var efficiency = ServerConfig.getFuelEfficiency(temp.getFluid());
            if(efficiency > 0){
                entityData.set(refuel$FUEL_AMOUNT, temp.getAmount());
                entityData.set(refuel$FUEL_TYPE, tag.getCompound("Fluid").getString("FluidName"));
            }
        }
    }

    @Inject(
            method = "defineSynchedData",
            at = @At("TAIL")
    )
    private void refuel$syncData(CallbackInfo ci) {
        entityData.define(refuel$FUEL_TYPE, "");
        entityData.define(refuel$SPEED, 0f);
        entityData.define(refuel$TEMPERATURE, 0f);
        entityData.define(refuel$FUEL_AMOUNT, 0);
        entityData.define(refuel$BATTERY_LEVEL, 0);
        entityData.define(refuel$STARTING_TIME, 0);
        entityData.define(refuel$STARTING, false);
        entityData.define(refuel$STARTED, false);
        entityData.define(refuel$FORWARD, false);
        entityData.define(refuel$BACKWARD, false);
        entityData.define(refuel$LEFT, false);
        entityData.define(refuel$RIGHT, false);
    }

    @Unique
    public LazyOptional<IFluidHandler> refuel$lazyFluid;

    @Unique
    public LazyOptional<IEnergyStorage> refuel$lazyEnergy;

    @Inject(
            remap = false,
            method = "getCapability",
            at = @At(
                    value = "RETURN",
                    ordinal = 1
            ),
            cancellable = true
    )
    private void refuel$addFluidCapability(@NotNull Capability<?> cap, @Nullable Direction side, CallbackInfoReturnable<LazyOptional<?>> cir) {
        if (this.isAlive() && cap == ForgeCapabilities.FLUID_HANDLER) {
            cir.setReturnValue(refuel$lazyFluid.cast());
        }
        if (this.isAlive() && cap == ForgeCapabilities.ENERGY) {
            cir.setReturnValue(refuel$lazyEnergy.cast());
        }
    }

    @Inject(
            remap = false,
            method = "invalidateCaps",
            at = @At("TAIL")
    )
    private void refuel$removeCap(CallbackInfo ci) {
        if (refuel$lazyFluid != null) {
            refuel$lazyFluid.invalidate();
        }
        if (refuel$lazyEnergy != null) {
            refuel$lazyEnergy.invalidate();
        }
    }

    @Unique
    public Container refuel$internalInventory;

    @Shadow(remap = false)
    public SimpleContainer inventory;

    public Container refuel$getContainer() {
        return refuel$internalInventory;
    }

    @Shadow(remap = false)
    @Final
    private static EntityDataAccessor<Float> HEALTH;

    @Shadow(remap = false)
    @Final
    private static float GRAVITY;

    public float refuel$getSpeed() {
        return entityData.get(refuel$SPEED);
    }

    @Unique
    public void refuel$setSpeed(float speed) {
        entityData.set(refuel$SPEED, speed);
    }


    // HEALTH

    public float refuel$getHealth() {
        return entityData.get(HEALTH);
    }

    public void refuel$setHealth(float health) {
        if (health > refuel$getMaxHealth()) {
            health = refuel$getMaxHealth();
        } else if (health <= 0) {
            health = 0;
            refuel$kill();
        }
        entityData.set(HEALTH, health);
    }

    @Unique
    public void refuel$addDamage(float damage) {
        refuel$setHealth(refuel$getHealth() - damage);
    }


    // FUEL

    @Unique
    public int refuel$getFuel() {
        return this.entityData.get(refuel$FUEL_AMOUNT);
    }

    public void refuel$setFuel(int fuel) {
        if (fuel < 0) {
            fuel = 0;
            entityData.set(refuel$FUEL_TYPE, "");
        }
        fuel = Math.min(fuel, refuel$getMaxFuel());
        entityData.set(refuel$FUEL_AMOUNT, fuel);
    }

    public Fluid refuel$getFluid() {
        String fuelType = entityData.get(refuel$FUEL_TYPE);
        if (fuelType.isEmpty()) return Fluids.EMPTY;

        //noinspection removal
        return ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fuelType));
    }

    public void refuel$setFuelType(String type){
        entityData.set(refuel$FUEL_TYPE, type);
    }


    // BATTERY

    public int refuel$getBattery() {
        return entityData.get(refuel$BATTERY_LEVEL);
    }

    public void refuel$setBattery(int level) {
        entityData.set(refuel$BATTERY_LEVEL, Mth.clamp(level, 0, refuel$getMaxBattery()));
    }


    // ENGINE TEMPERATURE

    public float refuel$getTemperature() {
        return entityData.get(refuel$TEMPERATURE);
    }

    public void refuel$setTemperature(float heat) {
        entityData.set(refuel$TEMPERATURE, heat);
    }

    public boolean refuel$isStarted() {
        return this.entityData.get(refuel$STARTED);
    }

    @Unique
    private static boolean refuel$carStopped = false, refuel$carStarted = false;

    public void refuel$setStarting(boolean starting, boolean playFailSound) {
        if (starting) {
            if (refuel$getBattery() <= 0) {
                return;
            }
            if (refuel$isStarted()) {
                refuel$setStarted(false, true, false);
                refuel$carStopped = true;
                return;
            }
        } else {
            if (refuel$carStarted || refuel$carStopped) {
                // TO prevent car from making stop start sound after releasing the starter key
                refuel$carStopped = false;
                refuel$carStarted = false;
                return;
            }
            if (playFailSound) {
                if (refuel$getBattery() > 0) {
                    refuel$playFailSound();
                }
            }
        }
        this.entityData.set(refuel$STARTING, starting);
    }

    @SuppressWarnings("SameParameterValue")
    @Unique
    private void refuel$setStarted(boolean started, boolean playStopSound, boolean playFailSound) {
        if (!started && playStopSound) {
            refuel$playStopSound();
        } else if (!started && playFailSound) {
            refuel$playFailSound();
        }
        this.entityData.set(refuel$STARTED, started);
    }

    @Unique
    public void refuel$playStopSound() {
        if (!(level().isClientSide)) {
            level().playSound(
                    null,
                    blockPosition().getX() + 0.5d,
                    blockPosition().getY() + 0.5d,
                    blockPosition().getZ() + 0.5d,
                    SoundEvents.CHAIN_HIT,
                    SoundSource.MASTER,
                    1f,
                    0f
            );
        }
    }

    @Unique
    public void refuel$playFailSound() {
        if (!(level().isClientSide)) {
            level().playSound(
                    null,
                    blockPosition().getX() + 0.5d,
                    blockPosition().getY() + 0.5d,
                    blockPosition().getZ() + 0.5d,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.MASTER,
                    1f,
                    1f + refuel$getBatterySoundPitchLevel()
            );
        }
    }

    @Unique
    public void refuel$playCrashSound() {
        if (!level().isClientSide) {
            level().playSound(
                    null,
                    (double) blockPosition().getX() + 0.5D,
                    (double) blockPosition().getY() + 0.5D,
                    (double) blockPosition().getZ() + 0.5D,
                    SoundEvents.ANVIL_LAND,
                    SoundSource.MASTER,
                    1f,
                    1f);
        }
    }

    @Unique
    public float refuel$getModifier() {
        var multiplier = ServerConfig.getRoadBlockMultiplier(getBlockStateOn());
        if (multiplier > 0) return multiplier;

        return ServerConfig.offroadSpeed.get().floatValue();
    }

    @Unique
    public float refuel$getRollResistance() {
        return ServerConfig.rollResistance.get().floatValue();
    }

    @Unique
    public float refuel$deltaRotation = 0;

    @Unique
    public void refuel$setForward(boolean forward) {
        entityData.set(refuel$FORWARD, forward);
    }

    @Unique
    public void refuel$setBackward(boolean backward) {
        entityData.set(refuel$BACKWARD, backward);
    }

    @Unique
    public void refuel$setLeft(boolean left) {
        entityData.set(refuel$LEFT, left);
    }

    @Unique
    public void refuel$setRight(boolean right) {
        entityData.set(refuel$RIGHT, right);
    }

    @Unique
    public boolean refuel$isForward() {
        return refuel$getDriver() != null && refuel$canPlayerDriveCar(refuel$getDriver()) && entityData.get(refuel$FORWARD);
    }

    @Unique
    public boolean refuel$isBackward() {
        return refuel$getDriver() != null && refuel$canPlayerDriveCar(refuel$getDriver()) && entityData.get(refuel$BACKWARD);
    }

    @Unique
    public boolean refuel$isLeft() {
        return refuel$getDriver() != null && refuel$canPlayerDriveCar(refuel$getDriver()) && entityData.get(refuel$LEFT);
    }

    @Unique
    public boolean refuel$isRight() {
        return refuel$getDriver() != null && refuel$canPlayerDriveCar(refuel$getDriver()) && entityData.get(refuel$RIGHT);
    }

    @Unique
    private Player refuel$getDriver(){
        Entity firstPassenger = getFirstPassenger();
        return firstPassenger instanceof Player player ? player : null;
    }

    @Unique
    private final boolean[] refuel$lastInputs = new boolean[4], refuel$lastTickInputs = new boolean[4];
    @Unique
    private boolean[] refuel$randomInputs = new boolean[2];

    @Unique
    private int[] refuel$drunkTicks = new int[4];

    @Unique
    private void refuel$handleInput() {
        if (!isVehicle()) {
            refuel$setForward(false);
            refuel$setBackward(false);
            refuel$setLeft(false);
            refuel$setRight(false);
        }
        Player driver = refuel$getDriver();
        var nausea = driver == null || !refuel$canPlayerDriveCar(driver) ? null : driver.getEffect(MobEffects.CONFUSION);
        int amp = -1;
        if (nausea != null) {
            amp = nausea.getAmplifier();
            if (tickCount % (random.nextInt(amp) + 1) == 0) refuel$randomInputs = new boolean[]{
                    random.nextInt(10 + Mth.floor(driver.getMaxHealth() / 10) - Math.min(amp, 9 + Mth.floor(driver.getMaxHealth() / 10))) == 0 ? random.nextBoolean() : refuel$isLeft(),
                    random.nextInt(10 + Mth.floor(driver.getMaxHealth() / 10) - Math.min(amp, 9 + Mth.floor(driver.getMaxHealth() / 10))) == 0 ? random.nextBoolean() : refuel$isRight()
            };
        } else refuel$drunkTicks = new int[4];

        float turnMod = 1;
        float maxSp = refuel$getMaxSpeed() * refuel$getModifier(),
                maxBackSp = refuel$getMaxReverseSpeed() * refuel$getModifier(),
                speed = subtractToZero(refuel$getSpeed(), refuel$getRollResistance());

        if (amp == -1) {
            if (refuel$isForward() && speed <= maxSp) speed = Math.min(speed + refuel$getAcceleration(), maxSp);
            // if sober, proceed as normal
            if (refuel$isBackward()) {
                if (level().isClientSide) turnMod = -1;
                if (speed >= -maxBackSp) speed = Math.max(speed - refuel$getAcceleration(), -maxBackSp);
            }
        } else {
            // FORWARD
            if (refuel$isForward() != refuel$lastTickInputs[0]) refuel$drunkTicks[0] = 0;
            if (refuel$drunkTicks[0] > amp) refuel$lastInputs[0] = refuel$isForward();
            else refuel$drunkTicks[0]++;
            if (refuel$lastInputs[0] && speed <= maxSp) speed = Math.min(speed + refuel$getAcceleration(), maxSp);
            refuel$lastTickInputs[0] = refuel$isForward();

            // BACKWARD
            if (refuel$isBackward() != refuel$lastTickInputs[1]) refuel$drunkTicks[1] = 0;
            if (refuel$drunkTicks[1] > amp) refuel$lastInputs[1] = refuel$isBackward();
            else refuel$drunkTicks[1]++;
            if (refuel$lastInputs[1]) {
                if (level().isClientSide) turnMod = -1;
                if (speed >= -maxBackSp) speed = Math.max(speed - refuel$getAcceleration(), -maxBackSp);
            }
            refuel$lastTickInputs[1] = refuel$isBackward();
        }

        refuel$setSpeed(speed);

        float rotationSpeed = 0;
        if (Math.abs(speed) > 0.02F) {
            rotationSpeed = Mth.abs(refuel$getRotationModifier() / (float) Math.pow(speed, 2));

            rotationSpeed = Mth.clamp(rotationSpeed, refuel$getHighSpeedSteering(), refuel$getLowSpeedSteering());
        }

        refuel$deltaRotation = 0;

        if (speed < 0) rotationSpeed = -rotationSpeed;

        if (amp == -1) {
            if (refuel$isLeft()) refuel$deltaRotation -= rotationSpeed;
            if (refuel$isRight()) refuel$deltaRotation += rotationSpeed;
        } else {
            // LEFT
            if (refuel$isLeft() != refuel$lastTickInputs[2]) refuel$drunkTicks[2] = 0;
            if (refuel$drunkTicks[2] > amp) refuel$lastInputs[2] = refuel$randomInputs[0];
            else refuel$drunkTicks[2]++;
            if (refuel$lastInputs[2]) refuel$deltaRotation -= rotationSpeed;
            refuel$lastTickInputs[2] = refuel$isLeft();

            // RIGHT
            if (refuel$isRight() != refuel$lastTickInputs[3]) refuel$drunkTicks[3] = 0;
            if (refuel$drunkTicks[3] > amp) refuel$lastInputs[3] = refuel$randomInputs[1];
            else refuel$drunkTicks[3]++;
            if (refuel$lastInputs[3]) refuel$deltaRotation += rotationSpeed;
            refuel$lastTickInputs[3] = refuel$isRight();
        }

        if (level().isClientSide) refuel$rotateWheels(refuel$deltaRotation * turnMod, rotationSpeed, speed);

        setYRot(getYRot() + refuel$deltaRotation);
        float delta = Math.abs(getYRot() - yRotO);
        while (getYRot() > 180F) {
            setYRot(getYRot() - 360F);
            yRotO = getYRot() - delta;
        }
        while (getYRot() <= -180F) {
            setYRot(getYRot() + 360F);
            yRotO = delta + getYRot();
        }

        setDeltaMovement(refuel$calculateMotionX(refuel$getSpeed(), getYRot()), getDeltaMovement().y, refuel$calculateMotionZ(refuel$getSpeed(), getYRot()));

        move(MoverType.SELF, getDeltaMovement());
    }

    @Unique
    private int refuel$timeToStart, refuel$timeSinceStarted;

    @Override
    public void tick() {
        if (level().isClientSide) {
            refuel$updateLastYRot();
            refuel$updateClientPos();
            refuel$tickLerp();
        }
        super.tick();

        Runnable task;
        while ((task = refuel$tasks.poll()) != null) {
            task.run();
        }

        if (refuel$isStarted() && !refuel$canEngineStayOn()) {
            refuel$setStarted(false);
        }

        refuel$updateGravity();
        refuel$handleInput();

        if (isInLava() && tickCount % 2 == 0) refuel$addDamage(1);
        if (refuel$isStarted() || getHP(this) <= 1) refuel$particles();

        refuel$fuelTick();
        refuel$checkSlots();

        if (level().isClientSide) {
            refuel$updateWheelRotation();
            refuel$displaySpeed(refuel$getSpeed());
            refuel$updateSounds();
            if (refuel$isStarted()) {
                refuel$timeSinceStarted++;
                if (tickCount % 2 == 0) {
                    refuel$spawnParticles(refuel$getSpeed() > 0.1F);
                    refuel$spawnParticles(refuel$getSpeed() > 0.1F);
                    //noinspection ConstantValue,EqualsBetweenInconvertibleTypes
                    if (CarMixin.class.equals(Motorcycle.class) || CarMixin.class.equals(SportCar.class)) {
                        refuel$spawnParticles(refuel$getSpeed() > 0.1F);
                        refuel$spawnParticles(refuel$getSpeed() > 0.1F);
                    }
                }
            } else
                refuel$timeSinceStarted = 0;
            refuel$angleTick();
            return;
        }

        // SERVER SIDE

        if (refuel$isStarting()) {
            refuel$setBattery(refuel$getBattery() - refuel$getBatteryUsage());

            refuel$setStartingTime(refuel$getStartingTime() + 1);
            if (refuel$getBattery() <= 0)
                refuel$setStarting(false, true);
        } else
            refuel$setStartingTime(0);


        int time = refuel$getStartingTime();
        if (time > 0) {
            if (refuel$timeToStart <= 0)
                refuel$timeToStart = refuel$getTimeToStart();

            if (time > refuel$getTimeToStart()) {
                refuel$startCarEngine();
                refuel$timeToStart = 0;
            }
        }

        if (refuel$isStarted()) {
            refuel$setStartingTime(0);
            refuel$carStarted = true;
        }

        if (tickCount % 20 != 0) return;

        float speedPerc = refuel$getSpeed() / refuel$getMaxSpeed();
        int tempRate = Math.min(Mth.floor(speedPerc) * 10 + 1, 5);

        if(refuel$isStarted())
            refuel$setBattery(refuel$getBattery() + Mth.floor(speedPerc * 4 - (1 - speedPerc) * 2));

        float rate = tempRate * 0.2F + (random.nextFloat() - 0.5F) * 0.1F;

        float temp = refuel$getTemperature(),
                tempToReach = refuel$getTemperatureToReach();

        if (isInBounds(temp, tempToReach, rate))
            refuel$setTemperature(tempToReach);
        else {
            if (tempToReach < temp)
                rate = -rate;
            refuel$setTemperature(temp + rate);
        }

    }

    public Vec3 refuel$getSeatPosition(Entity player) {
        if (!this.getPassengers().contains(player)) return null;

        return refuel$getSeatPositions()[this.getPassengers().indexOf(player)];
    }

    @Unique
    private static double refuel$calculateMotionX(float speed, float rotationYaw) {
        return Mth.sin(-rotationYaw * 0.017453292F) * speed;
    }

    @Unique
    private static double refuel$calculateMotionZ(float speed, float rotationYaw) {
        return Mth.cos(rotationYaw * 0.017453292F) * speed;
    }

    public void refuel$onCollision(float speed) {
        if (level().isClientSide){
            RefueledChannel.sendToServer(new VehicleCrash(this, speed));
        }
        refuel$setSpeed(0);
        setDeltaMovement(0, getDeltaMovement().y, 0);

        float percSpeed = speed / refuel$getMaxSpeed(),
                totalDamage;
        totalDamage = percSpeed * 5;
        refuel$playCrashSound();
        refuel$setStarted(false);

        if (percSpeed > 1)
            totalDamage += percSpeed * 5;

        refuel$addDamage(totalDamage);
        //noinspection EqualsBetweenInconvertibleTypes,ConstantExpression,ConstantValue
        if(CarMixin.class.equals(Motorcycle.class) && refuel$getDriver() != null){
            refuel$getDriver().hurt(new DamageSource(level().registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(RefueledRegistry.crashedVehicle), this, refuel$getDriver()), totalDamage / 4);
        }
    }

    @Unique
    public void refuel$setStarted(boolean started) {
        refuel$setStarting(false, false);

        refuel$setStarted(started, true, false);
    }


    @Unique
    public float refuel$getRotationModifier() {
        return 0.5F * 3f;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @Unique
    public boolean refuel$canPlayerDriveCar(Player player) {
        if (refuel$getFuel() <= 0) {
            return false;
        }

        if (player.equals(refuel$getDriver()) && refuel$isStarted()) {
            return true;
        } else if (isInWater() || isInLava()) {
            return false;
        } else {
            return false;
        }
    }

    @Unique
    public int refuel$getStartingTime() {
        return this.entityData.get(refuel$STARTING_TIME);
    }

    @Unique
    public void refuel$setStartingTime(int time) {
        this.entityData.set(refuel$STARTING_TIME, time);
    }

    @Unique
    public boolean refuel$isStarting() {
        return this.entityData.get(refuel$STARTING);
    }

    @Unique
    public int refuel$getTimeToStart() {
        int time = random.nextInt(10) + 5;

        float temp = refuel$getTemperature();
        if (temp < 0F) {
            time += 40;
        } else if (temp < 10F) {
            time += 35;
        } else if (temp < 30F) {
            time += 10;
        } else if (temp < 60F) {
            time += 5;
        }

        float batteryPerc = ((float) refuel$getBattery() / (float) refuel$getMaxBattery());

        if (batteryPerc < 0.5F) {
            time += 20 + random.nextInt(10);
        } else if (batteryPerc < 0.75F) {
            time += 10 + random.nextInt(10);
        }

        if (refuel$getHealth() < 5) {
            time += random.nextInt(25) + 50;
        } else if (refuel$getHealth() <= 10) {
            time += random.nextInt(15) + 30;
        } else if (refuel$getHealth() <= 20) {
            time += random.nextInt(15) + 10;
        } else if (refuel$getHealth() <= 50) {
            time += random.nextInt(10) + 5;
        }

        return time;
    }

    @Unique
    public float refuel$getTemperatureToReach() {
        float biomeTemp = refuel$getBiomeTemperatureCelsius();

        if (!refuel$isStarted()) {
            return biomeTemp;
        }
        float optimalTemp = refuel$getOptimalTemperature();

        if (biomeTemp > 45F) {
            optimalTemp = 100F;
        } else if (biomeTemp <= 0F) {
            optimalTemp = 80F;
        }
        return Math.max(biomeTemp, optimalTemp);
    }

    @Unique
    public float refuel$getBiomeTemperatureCelsius() {
        Biome biome = level().getBiome(blockPosition()).value();
        return (((IBiomeAccess) (Object) biome).invokeGetTemperature(blockPosition()) - 0.3f) * 30f;
    }

    @Unique
    public float refuel$getOptimalTemperature() {
        return 90F;
    }

    @Unique
    public int refuel$getBatteryUsage() {
        if (!ServerConfig.useBattery.get()) {
            return 0;
        }

        float temp = refuel$getBiomeTemperatureCelsius();
        int baseUsage = 2;
        if (temp < 0F) {
            baseUsage += 2;
        } else if (temp < 15F) {
            baseUsage++;
        }
        return baseUsage;
    }

    @Unique
    public void refuel$startCarEngine() {
        Player player = refuel$getDriver();
        if (player != null && refuel$canStartCarEngine()) {
            refuel$setStarted(true);
        }
    }

    @Unique
    public boolean refuel$canStartCarEngine() {
        if (refuel$getFuel() <= 0) {
            return false;
        }

        if (refuel$getHealth() <= 0) {
            return false;
        }

        return !isInWater() && !isInLava();
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity entity) {
        Direction direction = getMotionDirection();
        if (direction.getAxis() == Direction.Axis.Y) {
            return super.getDismountLocationForPassenger(entity);
        }

        int[][] offsets = DismountHelper.offsetsForDirection(direction);
        AABB bb = entity.getLocalBoundsForPose(Pose.STANDING);
        for (int[] offset : offsets) {
            int i = this.getPassengers().size();
            Vec3 dismountPos = new Vec3(
                    getX() + refuel$getDismountLocations(offset[0], getBbWidth(), entity.getBbWidth())[i],
                    getY(),
                    getZ() + refuel$getDismountLocations(offset[1], getBbWidth(), entity.getBbWidth())[i]);

            double y = level().getBlockFloorHeight(new BlockPos((int) dismountPos.x, (int) dismountPos.y, (int) dismountPos.z));
            if (DismountHelper.isBlockFloorValid(y)) {
                if (DismountHelper.canDismountTo(level(), entity, bb.move(dismountPos))) {
                    return dismountPos;
                }
            }
        }
        return super.getDismountLocationForPassenger(entity);
    }

    @Override
    public boolean canBeHitByProjectile() {
        return this.isAlive();
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float damage) {
        if (level().isClientSide || isInvulnerable() || !isAlive()) return false;

        if (damageSource.getEntity() != null && getPassengers().stream().anyMatch(damageSource.getEntity()::equals)) return false;

        if (!this.isRemoved()) {
            this.markHurt();
            this.gameEvent(GameEvent.ENTITY_DAMAGE);

            var actualDamage = refuel$getHealth() - damage;
            if (actualDamage <= 0)
                refuel$kill();
            else refuel$setHealth(refuel$getHealth() - damage);

            return true;
        }

        return false;
    }

    @Unique
    private void refuel$kill(){
        refuel$tasks.add(() -> {
            var fuelPerc = (float) refuel$getFuel() / (float) refuel$getMaxFuel();
            if (ServerConfig.explodeOnDeath.get())
                level().explode(null, new DamageSource(level().registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(RefueledRegistry.vehicleExplosion), this, refuel$getDriver(), position()), null, getX(), getY(), getZ(), 2f + 4f * fuelPerc, fuelPerc >= 0.75, Level.ExplosionInteraction.MOB);
            Containers.dropContents(this.level(), this, inventory);
            Containers.dropContents(this.level(), this, refuel$internalInventory);
            refuel$convertIntoItems();
            this.kill();
        });
    }

    @Unique
    private void refuel$convertIntoItems(){
        Containers.dropContents(this.level(), this, inventory);
        Containers.dropContents(this.level(), this, refuel$internalInventory);
    }

    @Unique
    private ItemStack refuel$getEgg(){
        ItemStack egg = List.of(ItemRegistry.CAR_SPAWN_EGG, ItemRegistry.CLASSIC_SPAWN_EGG, ItemRegistry.TRUCK_SPAWN_EGG, ItemRegistry.SUV_SPAWN_EGG, ItemRegistry.SPORT_CAR_SPAWN_EGG, ItemRegistry.MOTORCYCLE_SPAWN_EGG).get(refuel$type).get().getDefaultInstance();
        CompoundTag tag = new CompoundTag();
        tag.putFloat("temperature", refuel$getTemperature());

        if(refuel$getBattery() > 0)
            tag.putInt("energy", refuel$getBattery());

        if(refuel$getFuel() > 0){
            tag.put("Fluid", new FluidStack(refuel$getFluid(), refuel$getFuel()).writeToNBT(new CompoundTag()));
        }

        if(refuel$getHealth() > 0)
            tag.putFloat("Health", refuel$getHealth());

        if(hasCustomName()){
            //noinspection DataFlowIssue
            tag.putString("custom", getCustomName().getString());
            egg.setHoverName(Component.literal(getCustomName().getString()));
        }

        egg.getOrCreateTag().put("refueled", tag);
        return egg;
    }

    @Unique
    public float refuel$getBatterySoundPitchLevel() {
        int startLevel = refuel$getMaxBattery() / 3;
        float basePitch = 1F - 0.002F * (float) refuel$getStartingTime();
        if (refuel$getBattery() > startLevel) return basePitch;

        float perc = (float) (startLevel - refuel$getBattery()) / (float) startLevel;
        return basePitch - (perc / 2.3F);
    }

    @Unique
    private final BlockingQueue<Runnable> refuel$tasks = new LinkedBlockingQueue<>();

    @Unique
    private void refuel$addTask(LivingEntity mob, DamageSource source, double amount){
        refuel$tasks.add(() -> {
            if(!ServerConfig.isEntityOnList(mob))
                mob.knockback((refuel$getSpeed() / refuel$getMaxSpeed()) * Math.max(amount - mob.getHealth(), 1), getX() - mob.getX(), getZ() - mob.getZ());
            mob.hurt(source, (float) amount);
        });
    }

    @Override
    public boolean canCollideWith(@NotNull Entity entity) {
        if (!level().isClientSide && ServerConfig.damageEntities.get() && entity instanceof LivingEntity mob && !getPassengers().contains(entity)) {
            if (entity.getBoundingBox().intersects(getBoundingBox()) && refuel$getSpeed() > 0.3) {
                double damage = refuel$getSpeed() * refuel$getRamDamage();
                DamageSource source = new DamageSource(level().registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(RefueledRegistry.vehicleCollision), this, refuel$getDriver());
                //noinspection ConstantValue,EqualsBetweenInconvertibleTypes
                if(CarMixin.class.equals(Truck.class) && this.hasCustomName() && this.getCustomName().getString().equalsIgnoreCase("bergentrück") && refuel$getDriver() != null && refuel$getDriver().getItemInHand(InteractionHand.MAIN_HAND).getItem() == Items.TRIDENT){
                    damage *= 3.4;
                    source = new DamageSource(level().registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(RefueledRegistry.bergentrucked), this, refuel$getDriver());
                }
                refuel$addTask(mob, source, damage);
            }
        }

        boolean onList = ServerConfig.isEntityOnList(entity);
        if (ServerConfig.collideWithEntities.get()) {
            // If all entities are walls, then consider it a whitelist
            if(!isCar(entity) && onList)
                return true;
        }
        else{
            // If all can be ran over, consider it a blacklist
            if(!isCar(entity) || onList)
                return false;
        }

        return (entity.canBeCollidedWith() || entity.isPushable()) && !isPassengerOfSameVehicle(entity);
    }

    @Unique
    public boolean refuel$canEngineStayOn() {
        if (isInWater()) {
            if(tickCount % 20 == 0)
                refuel$addDamage(25);
            return false;
        }

        return !isInLava() && refuel$getFuel() > 0 && refuel$getHealth() > 0 && refuel$getBattery() > 0;
    }

    @Unique
    private void refuel$updateGravity() {
        Vec3 delta = getDeltaMovement();
        if (isNoGravity()) {
            setDeltaMovement(delta.x, 0, delta.z);
            return;
        }
        setDeltaMovement(delta.x, delta.y - (isInWater() ? 0.01 : 0.08), delta.z);
    }

    @Unique
    public void refuel$spawnParticles(boolean driving) {
        if (!level().isClientSide) return;
        Vec3 lookVec = getLookAngle().normalize();
        double lookAngle = getYRot() < 0 ? 360 + getYRot() : getYRot();

        int rand = random.nextInt(4);
        double radius = refuel$getExhaust(rand)[0], angle = refuel$getExhaust(rand)[1];

        double offX = Math.sin(Math.toRadians(lookAngle)) + radius * Math.sin(angle + Math.toRadians(lookAngle)); // offX is equal to the vertical vector on a 2d plane
        double offY = refuel$getExhaust(rand)[2]; // slightly elevate the exhaust
        double offZ = Math.cos(Math.toRadians(lookAngle)) * -1D - radius * Math.cos(angle + Math.toRadians(lookAngle)); // and for offZ, it's equal to the horizontal vector


        // Engine started smoke should only come 1 second after start and only if the
        // engine is colder than 50°C
        if (refuel$timeSinceStarted > 0 && refuel$timeSinceStarted < 20 && refuel$getTemperature() < 50F) {
            double speedX = lookVec.x * -0.1D;
            double speedZ = lookVec.z * -0.1D;

            int health = (int) ((refuel$getHealth() / refuel$getMaxHealth()) * 100f);
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
                refuel$spawnParticle(ParticleTypes.LARGE_SMOKE, offX, offY, offZ, speedX, speedZ, r);
            }
        } else if (driving) {
            double speedX = lookVec.x * -0.2D;
            double speedZ = lookVec.z * -0.2D;
            refuel$spawnParticle(ParticleTypes.SMOKE, offX, offY, offZ, speedX, speedZ);
        } else {
            double speedX = lookVec.x * -0.05D;
            double speedZ = lookVec.z * -0.05D;
            refuel$spawnParticle(ParticleTypes.SMOKE, offX, offY, offZ, speedX, speedZ);
        }

    }

    @Unique
    private void refuel$spawnParticle(ParticleOptions particleTypes, double offX, double offY, double offZ, double speedX, double speedZ, double r) {
        level().addParticle(particleTypes,
                getX() + offX + (random.nextDouble() * r - r / 2D),
                getY() + offY + (random.nextDouble() * r - r / 2D) + getBbHeight() / 8F,
                getZ() + offZ + (random.nextDouble() * r - r / 2D),
                speedX, 0.0D, speedZ);
    }

    @Unique
    private void refuel$spawnParticle(@SuppressWarnings("SameParameterValue") ParticleOptions particleTypes, double offX, double offY, double offZ, double speedX, double speedZ) {
        refuel$spawnParticle(particleTypes, offX, offY, offZ, speedX, speedZ, 0.1D);
    }

    @Unique
    public void refuel$particles() {
        if (!level().isClientSide) {
            return;
        }
        int health = (int) ((refuel$getHealth() / refuel$getMaxHealth()) * 100);

        if (health > 50) return;

        int amount = 3;
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
        }

        for (int i = 0; i < amount; i++) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    0.0D, 0.0D, 0.0D);
        }

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
    public void refuel$initTemperature() {
        refuel$setTemperature(refuel$getBiomeTemperatureCelsius());
    }

    @Unique
    public void refuel$updateControls(boolean forward, boolean backward, boolean left, boolean right, Player player) {
        boolean needsUpdate = false;

        if (refuel$isForward() != forward) {
            refuel$setForward(forward);
            needsUpdate = true;
        }

        if (refuel$isBackward() != backward) {
            refuel$setBackward(backward);
            needsUpdate = true;
        }

        if (refuel$isLeft() != left) {
            refuel$setLeft(left);
            needsUpdate = true;
        }

        if (refuel$isRight() != right) {
            refuel$setRight(right);
            needsUpdate = true;
        }

        if (level().isClientSide && needsUpdate) {
            RefueledChannel.sendToServer(new ControlVehicle(forward, backward, left, right, player));
        }
    }

    @Unique
    public void refuel$centerCar() {
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
    public void refuel$openGUI(Player player) {
        if (level().isClientSide)
            RefueledChannel.sendToServer(new VehicleGUI(player));
        else if (player instanceof ServerPlayer srvrplyr)
            NetworkHooks.openScreen(srvrplyr, this, packetBuffer -> packetBuffer.writeUUID(getUUID()));

    }

    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
        return new CarGUI(i, playerInventory, this);
    }

    @Mixin(Classic.class)
    private abstract static class ClassicMixin implements IVehicleAccess {
        @Override
        public Vec3[] refuel$getSeatPositions() {
            return new Vec3[]{
                    new Vec3(0.65 * sizeFactor, 0.1 * sizeFactor, 0.1 * sizeFactor),
                    new Vec3(-0.65 * sizeFactor, 0.1 * sizeFactor, 0.1 * sizeFactor),
                    new Vec3(0.65 * sizeFactor, 0.1 * sizeFactor, -2.2 * sizeFactor),
                    new Vec3(-0.65 * sizeFactor, 0.1 * sizeFactor, -2.2 * sizeFactor)
            };
        }
    }

    @Mixin(Truck.class)
    private abstract static class TruckMixin extends Entity implements IVehicleAccess {
        public TruckMixin(EntityType<?> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
        }

        @Shadow(remap = false)
        public SimpleContainer inventory;

        protected boolean canAddPassenger(@NotNull Entity pPassenger) {
            int emptyCount = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).isEmpty()) emptyCount++;
            }
            emptyCount = Math.min(48, emptyCount);
            return this.getPassengers().size() < 2 + emptyCount / 24;
        }

        @ModifyArg(
                method = "createInventory",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/SimpleContainer;<init>(I)V"
                ),
                index = 0
        )
        private int refuel$increaseSize(int pSize) {
            return 72;
        }

        @Redirect(
                method = "interact*",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraftforge/network/NetworkHooks;openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;)V"
                )
        )
        private void refuel$redirectIntoLargerGUI(ServerPlayer player, MenuProvider containerSupplier) {
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return TruckMixin.this.getDisplayName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new TruckGUI(i, playerInventory, (Truck) ((Entity) TruckMixin.this));
                }
            }, packetBuffer -> packetBuffer.writeUUID(getUUID()));
        }

        public float refuel$getPitch() {
            return 1f + 0.34f * Math.abs(refuel$getSpeed()) / refuel$getMaxSpeed();
        }

        public SoundEvent refuel$getEngineSound() {
            return RefueledRegistry.TRUCK_ENGINE.get();
        }
    }

    @Mixin(SUV.class)
    private abstract static class SUVMixin implements IVehicleAccess {

        public float refuel$getPitch() {
            return 1f + 0.34f * Math.abs(refuel$getSpeed()) / refuel$getMaxSpeed();
        }

        public SoundEvent refuel$getEngineSound() {
            return RefueledRegistry.TRUCK_ENGINE.get();
        }
    }

    @Mixin(SportCar.class)
    private abstract static class SportMixin implements IVehicleAccess {

        public Vec3[] refuel$getSeatPositions() {
            return new Vec3[]{
                    new Vec3(0.7 * sizeFactor, 0, 0.1 * sizeFactor),
                    new Vec3(-0.7 * sizeFactor, 0, 0.1 * sizeFactor),
                    new Vec3(0.7 * sizeFactor, 0, -2.2 * sizeFactor),
                    new Vec3(-0.7 * sizeFactor, 0, -2.2 * sizeFactor)
            };
        }

        public float[] refuel$getExhaust(int rand) {
            final var factor = sizeFactor;
            double[] modX = new double[]{1D, -1D, 1D, -1D};
            double radius = Math.sqrt(1.8 * factor * 1.8 * factor + factor * factor);                             // calculates distance from entity center to exhaust point
            double pointDist = Math.sqrt((1 + 1.8 * factor - (1 + radius)) * (1 + 1.8 * factor - (1 + radius)) + factor * factor);    // calculates distance from exhaust point to current entity viewing point
            double angle = 2 * Math.asin(0.5 * pointDist / radius) * modX[rand];

            return new float[]{(float) radius, (float) angle, 0.08f * factor.floatValue()};
        }

        public SoundEvent refuel$getEngineSound() {
            return RefueledRegistry.SPORT_ENGINE.get();
        }
    }

    @Mixin(Motorcycle.class)
    private abstract static class BikeMixin implements IVehicleAccess {
        @Override
        public float[] refuel$getExhaust(int rand) {
            // todo sizeFactor
            double[] modX = new double[]{1d, -1d, 1d, -1d};
            double[] randomOffY = new double[]{0.5d, 0.5d, 0.525D - 0.2D, 0.525D - 0.2D};
            double[] modY = new double[]{1.125d, 1.125d, 1.125D - 0.15D, 1.125D - 0.15D};

            double radius = Math.sqrt((modY[rand] - 1D) * (modY[rand] - 1D) + (0.2D - 0) * (0.2D - 0));
            double pointDist = Math.sqrt((modY[rand] - (1D + radius)) * (modY[rand] - (1D + radius)) + (0.2D - 0) * (0.2D - 0));
            double angle = (2 * Math.asin(0.5 * pointDist / radius)) * modX[rand];

            return new float[]{(float) radius, (float) angle, (float) randomOffY[rand]};
        }

        public Vec3[] refuel$getSeatPositions() {
            return new Vec3[]{
                    new Vec3(0, 0.56 * sizeFactor, -0.5 * sizeFactor)
            };
        }

        public SoundEvent refuel$getEngineSound() {
            return RefueledRegistry.SPORT_ENGINE.get();
        }

        @Redirect(
                method = "interact",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraftforge/network/NetworkHooks;openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;)V"
                )
        )
        private void refuel$mergeInventories(ServerPlayer player, MenuProvider containerSupplier) {
            refuel$openGUI(player);
        }
    }

    @Override
    protected void removePassenger(Entity removedPassenger) {
        if (removedPassenger.getVehicle() == this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        } else {
            if (((IEntityAccess) this).getPassengers().size() == 1 && ((IEntityAccess) this).getPassengers().get(0) == removedPassenger) {
                ((IEntityAccess) this).setPassengers(ImmutableList.of());
            } else {
                ((IEntityAccess) this).setPassengers(((IEntityAccess) this).getPassengers().stream().filter(passenger -> passenger != removedPassenger).collect(ImmutableList.toImmutableList()));
            }

            ((IEntityAccess) removedPassenger).setBoardingCooldown(60);
            removedPassenger.refreshDimensions();
            this.gameEvent(GameEvent.ENTITY_DISMOUNT, removedPassenger);
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (ServerConfig.vehiclePersist.get()) {
            if (getPersistentData().contains(REFUELED_KEY)) {
                CompoundTag tag = getPersistentData().getCompound(REFUELED_KEY);
                for (var key : tag.getAllKeys()) {
                    ServerPlayer player = (ServerPlayer) level().getPlayerByUUID(tag.getUUID(key));
                    if (player != null)
                        player.startRiding(this);
                }

                getPersistentData().remove(REFUELED_KEY);
            }
        }
    }

    public float refuel$getMaxHealth() {
        return ServerConfig.vehicleHealth.get().get(refuel$type).floatValue();
    }

    public int refuel$getMaxBattery(){
        return ServerConfig.vehicleBattery.get().get(refuel$type).intValue();
    }

    public float refuel$getMaxSpeed() {
        return ServerConfig.vehicleMaxSpeed.get().get(refuel$type).floatValue();
    }

    public float refuel$getMaxReverseSpeed() {
        return ServerConfig.vehicleMaxRevSpeed.get().get(refuel$type).floatValue();
    }

    public float refuel$getAcceleration() {
        return ServerConfig.vehicleAcc.get().get(refuel$type).floatValue();
    }

    @Unique
    public float refuel$getRamDamage() {
        return ServerConfig.vehicleRamDamage.get().get(refuel$type).floatValue();
    }

    @Unique
    public float refuel$getEfficiency(Fluid fluid){
        var eff = ServerConfig.getFuelEfficiency(fluid);
        if(eff <= 0){
            eff = 0;
            refuel$setFuel(0);
        }
        return ServerConfig.vehicleFuelEff.get().get(refuel$type).floatValue() * eff;
    }

    @Unique
    public float refuel$getHighSpeedSteering(){
        return ServerConfig.vehicleSteering.get().get(refuel$type).get(0).floatValue();
    }

    @Unique
    public float refuel$getLowSpeedSteering(){
        return ServerConfig.vehicleSteering.get().get(refuel$type).get(1).floatValue();
    }

    public int refuel$getMaxFuel(){
        return ServerConfig.vehicleFuel.get().get(refuel$type).intValue();
    }

    public float getStepHeight(){
        return ServerConfig.vehicleStepHeight.get().get(refuel$type).floatValue();
    }

    @Unique
    private void refuel$fuelTick() {
        int fuel = refuel$getFuel();
        if (fuel == 0) return;

        // Fuel efficiency represented in a decimal percentage on a scale of 0.0 -> 1.0
        double efficiency = refuel$getEfficiency(refuel$getFluid());
        if (efficiency <= 0) return;

        int frequency = Math.max(Mth.floor(20 * efficiency), 5),
            density = 4 + (10 - Math.min(Mth.floor(20 * efficiency), 5) * 2);

        // 4mb every 20 ticks -> 6m:15s @ 1.5B of 100% fuel
        if (fuel > 0 && ((refuel$isForward() || refuel$isBackward()) && !horizontalCollision && refuel$isStarted())) {
            if (tickCount % frequency == 0){
                refuel$drainFuel(density);
            }
        } else if (fuel > 0 && refuel$isStarted()) {
            if (tickCount % (frequency * 100) == 0){
                refuel$drainFuel(density);
            }
        }
    }

    @Unique
    private void refuel$drainFuel(int amount){
        if(!refuel$internalInventory.getItem(0).isEmpty()){
            refuel$internalInventory.getItem(0).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent( otherHandler -> {
                if(otherHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).getAmount() > 0){
                    int remainder = amount - otherHandler.drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount();
                    if(remainder > 0)
                        refuel$setFuel(refuel$getFuel() - remainder);
                }
            });
        }
        else
            refuel$setFuel(refuel$getFuel() - amount);
    }

    @Unique
    private void refuel$checkSlots(){
        if(!refuel$internalInventory.getItem(0).isEmpty()){
            refuel$internalInventory.getItem(0).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(
                    otherHandler -> {
                if(otherHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).getAmount() > 0){
                    int toDrain = Math.min(refuel$getMaxFuel() - refuel$getFuel(), 0);
                    refuel$setFuel(otherHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE).getAmount());
                }
            });
        }

        if(!refuel$internalInventory.getItem(1).isEmpty()){
            ItemStack stack = refuel$internalInventory.getItem(1);

            IEnergyStorage energy = getCapability(ForgeCapabilities.ENERGY).resolve().get();
            LazyOptional<IEnergyStorage> lazyOtherEnergy = stack.getCapability(ForgeCapabilities.ENERGY);
            lazyOtherEnergy.ifPresent(otherEnergy -> {
                if (energy.receiveEnergy(otherEnergy.getEnergyStored(), true) > 0) {
                    otherEnergy.extractEnergy(energy.receiveEnergy(otherEnergy.getEnergyStored(), false), false);
                }
            });

            if(stack.getItem().equals(Items.REDSTONE) && energy.receiveEnergy(360, true) > 0) {
                if(energy.receiveEnergy(360, true) > 0){
                    stack.shrink(1);
                    refuel$internalInventory.setChanged();
                    energy.receiveEnergy(360, false);
                }
            }
        }

        if(!refuel$internalInventory.getItem(2).isEmpty()){
            ItemStack stack = refuel$internalInventory.getItem(2);
            var data = ServerConfig.getRepairItemData(stack);

            //noinspection DataFlowIssue
            if (getHP(this) <= 25 && stack.getCount() > Integer.parseInt(data.get(0))) {
                float health = refuel$getHealth() + Float.parseFloat(data.get(1));
                stack.shrink(Integer.parseInt(data.get(0)));
                refuel$internalInventory.setChanged();
                refuel$setHealth(health);
            }

        }
    }

    public void move(@NotNull MoverType pType, @NotNull Vec3 pPos) {
        if (this.noPhysics) {
            this.setPos(this.getX() + pPos.x, this.getY() + pPos.y, this.getZ() + pPos.z);
        } else {
            this.wasOnFire = this.isOnFire();
            if (pType == MoverType.PISTON) {
                pPos = this.limitPistonMovement(pPos);
                if (pPos.equals(Vec3.ZERO)) {
                    return;
                }
            }

            this.level().getProfiler().push("move");
            if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7) {
                pPos = pPos.multiply(this.stuckSpeedMultiplier);
                this.stuckSpeedMultiplier = Vec3.ZERO;
                this.setDeltaMovement(Vec3.ZERO);
            }

            pPos = this.maybeBackOffFromEdge(pPos, pType);
            Vec3 vec3 = refuel$vehicleCollide(pPos);
            double d0 = vec3.lengthSqr();
            if (d0 > 1.0E-7) {
                if (this.fallDistance != 0.0F && d0 >= (double)1.0F) {
                    BlockHitResult blockhitresult = this.level().clip(new ClipContext(this.position(), this.position().add(vec3), ClipContext.Block.FALLDAMAGE_RESETTING, net.minecraft.world.level.ClipContext.Fluid.WATER, this));
                    if (blockhitresult.getType() != HitResult.Type.MISS) {
                        this.resetFallDistance();
                    }
                }

                this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
            }

            this.level().getProfiler().pop();
            this.level().getProfiler().push("rest");
            //noinspection SuspiciousNameCombination
            boolean flag4 = !Mth.equal(pPos.x, vec3.x);
            boolean flag = !Mth.equal(pPos.z, vec3.z);
            this.horizontalCollision = flag4 || flag;
            this.verticalCollision = pPos.y != vec3.y;
            this.verticalCollisionBelow = this.verticalCollision && pPos.y < (double)0.0F;
            if (this.horizontalCollision) {
                this.minorHorizontalCollision = this.isHorizontalCollisionMinor(vec3);
            } else {
                this.minorHorizontalCollision = false;
            }

            this.setOnGroundWithKnownMovement(this.verticalCollisionBelow, vec3);
            BlockPos blockpos = this.getOnPos(0.2F);
            BlockState blockstate = this.level().getBlockState(blockpos);
            this.checkFallDamage(vec3.y, this.onGround(), blockstate, blockpos);
            if (this.isRemoved()) {
                this.level().getProfiler().pop();
            } else {
                if (this.horizontalCollision) {
                    Vec3 vec31 = this.getDeltaMovement();
                    this.setDeltaMovement(flag4 ? (double)0.0F : vec31.x, vec31.y, flag ? (double)0.0F : vec31.z);
                }

                net.minecraft.world.level.block.Block block = blockstate.getBlock();
                if (pPos.y != vec3.y) {
                    block.updateEntityAfterFallOn(this.level(), this);
                }

                if (this.onGround()) {
                    block.stepOn(this.level(), blockpos, blockstate, this);
                }

                MovementEmission entity$movementemission = this.getMovementEmission();
                if (entity$movementemission.emitsAnything() && !this.isPassenger()) {
                    double d1 = vec3.x;
                    double d2 = vec3.y;
                    double d3 = vec3.z;
                    this.flyDist = (float)((double)this.flyDist + vec3.length() * 0.6);
                    BlockPos blockpos1 = this.getOnPos();
                    BlockState blockstate1 = this.level().getBlockState(blockpos1);
                    boolean flag1 = blockstate1.is(BlockTags.CLIMBABLE) || blockstate1.is(Blocks.POWDER_SNOW);
                    if (!flag1) {
                        d2 = 0;
                    }

                    this.walkDist += (float)vec3.horizontalDistance() * 0.6F;
                    this.moveDist += (float)Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3) * 0.6F;
                    if (this.moveDist > ((IEntityAccess) this).getNextStep() && !blockstate1.isAir()) {
                        boolean flag2 = blockpos1.equals(blockpos);
                        boolean flag3 = ((IEntityAccess) this).invokeVaSEFB(blockpos, blockstate, entity$movementemission.emitsSounds(), flag2, pPos);
                        if (!flag2) {
                            flag3 |= ((IEntityAccess) this).invokeVaSEFB(blockpos1, blockstate1, false, entity$movementemission.emitsEvents(), pPos);
                        }

                        if (flag3) {
                            ((IEntityAccess) this).setNextStep(nextStep());
                        } else if (this.isInWater()) {
                            ((IEntityAccess) this).setNextStep(nextStep());
                            if (entity$movementemission.emitsSounds()) {
                                this.waterSwimSound();
                            }

                            if (entity$movementemission.emitsEvents()) {
                                this.gameEvent(GameEvent.SWIM);
                            }
                        }
                    } else if (blockstate1.isAir()) {
                        this.processFlappingMovement();
                    }
                }

                this.tryCheckInsideBlocks();
                float f = this.getBlockSpeedFactor();
                this.setDeltaMovement(this.getDeltaMovement().multiply(f, 1.0F, f));
                if (this.level().getBlockStatesIfLoaded(this.getBoundingBox().deflate(1.0E-6)).noneMatch((p_20127_) -> p_20127_.is(BlockTags.FIRE) || p_20127_.is(Blocks.LAVA))) {
                    if (this.getRemainingFireTicks() <= 0) {
                        this.setRemainingFireTicks(-this.getFireImmuneTicks());
                    }

                    if (this.wasOnFire && (this.isInPowderSnow || this.isInWaterRainOrBubble() || this.isInFluidType((fluidType, height) -> this.canFluidExtinguish(fluidType)))) {
                        this.playEntityOnFireExtinguishedSound();
                    }
                }

                if (this.isOnFire() && (this.isInPowderSnow || this.isInWaterRainOrBubble() || this.isInFluidType((fluidType, height) -> this.canFluidExtinguish(fluidType)))) {
                    this.setRemainingFireTicks(-this.getFireImmuneTicks());
                }

                level().getProfiler().pop();
            }
        }
    }

    @Unique
    private Vec3 refuel$vehicleCollide(Vec3 pVec){
        AABB aabb = this.getBoundingBox();
        List<VoxelShape> list = this.level().getEntityCollisions(this, aabb.expandTowards(pVec));
        Vec3 vec3 = pVec.lengthSqr() == 0 ? pVec : collideBoundingBox(this, pVec, aabb, this.level(), list);
        boolean flag = pVec.x != vec3.x;
        boolean flag1 = pVec.y != vec3.y;
        boolean flag2 = pVec.z != vec3.z;
        boolean flag3 = this.onGround() || flag1 && pVec.y < 0;
        float stepHeight = this.getStepHeight();
        if (stepHeight > 0 && flag3 && (flag || flag2)) {
            Vec3 vec31 = collideBoundingBox(this, new Vec3(pVec.x, stepHeight, pVec.z), aabb, level(), list);
            Vec3 vec32 = collideBoundingBox(this, new Vec3(0, stepHeight, 0), aabb.expandTowards(pVec.x, 0, pVec.z), level(), list);
            if (vec32.y < (double)stepHeight) {
                Vec3 vec33 = collideBoundingBox(this, new Vec3(pVec.x, 0, pVec.z), aabb.move(vec32), this.level(), list).add(vec32);
                if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr()) {
                    vec31 = vec33;
                }
            }

            if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                return vec31.add(collideBoundingBox(this, new Vec3(0.0F, -vec31.y + pVec.y, 0.0F), aabb.move(vec31), this.level(), list));
            }
        }

        if(refuel$sinceCollided > 2)
            refuel$onCollision(refuel$getSpeed());
        if(horizontalCollision && refuel$getSpeed() / refuel$getMaxSpeed() > 0.8 && vec3.y == 0)
            refuel$sinceCollided++;
        else refuel$sinceCollided = 0;

        return vec3;
    }

    @Unique
    private int refuel$sinceCollided;
}