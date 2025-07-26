package bogdan.refueled.mixin.common.car;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.config.ServerConfig;
import com.dragn0007.dragnvehicles.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {CarItem.class, ClassicItem.class, TruckItem.class, SUVItem.class, SportCarItem.class, MotorcycleItem.class})
public class CarSpawnMixin {
    @Redirect(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;")
    )
    private Entity car$injectOnSpawn(EntityType<?> instance, ServerLevel serverLevel, ItemStack itemStack, Player player, BlockPos pos, MobSpawnType pPlayer, boolean pPos, boolean pSpawnType){
        Entity car = instance.spawn(serverLevel, itemStack, player, pos.above(), MobSpawnType.SPAWN_EGG, false, false);
        if(car != null){
            CompoundTag itemTag = itemStack.getTag();
            if(itemTag != null && itemTag.contains("refueled")){
                CompoundTag tag = itemTag.getCompound("refueled");
                if(tag.contains("temperature"))
                    ((IVehicleAccess) car).refuel$setTemperature(tag.getFloat("temperature"));

                if(tag.contains("Health"))
                    ((IVehicleAccess) car).refuel$setHealth(tag.getFloat("Health"));

                if(tag.contains("energy"))
                    ((IVehicleAccess) car).refuel$setBattery(tag.getInt("energy"));

                if(tag.contains("Fluid")){
                    FluidStack stack = FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid"));
                    if(stack.isEmpty())
                        RefueledMain.LOGGER.error("Vehicle spawn egg ({}) contained refueled.Fluid tag but returned empty fluid stack", itemStack);
                    else try {
                        ((IVehicleAccess) car).refuel$setFuel(stack.getAmount());
                        //noinspection DataFlowIssue
                        ((IVehicleAccess) car).refuel$setFuelType(ForgeRegistries.FLUIDS.getKey(stack.getFluid()).toString());
                    } catch (NullPointerException e){
                        RefueledMain.LOGGER.error("Tried to spawn car with non-existent fluid: {}", stack.getFluid());
                    }
                }

                if(tag.contains("custom"))
                    car.setCustomName(Component.literal(tag.getString("custom")));

                tag.remove("refueled");
            } else if(ServerConfig.spawnFull.get()) {
                ((IVehicleAccess) car).refuel$initTemperature();
                ((IVehicleAccess) car).refuel$setBattery(((IVehicleAccess) car).refuel$getMaxBattery());
                ((IVehicleAccess) car).refuel$setFuelType(ServerConfig.fuelEff.get().get(0).get(0));
                ((IVehicleAccess) car).refuel$setFuel(((IVehicleAccess) car).refuel$getMaxFuel());
                ((IVehicleAccess) car).refuel$setHealth(((IVehicleAccess) car).refuel$getMaxHealth());
            }
        }
        return car;
    }
}
