package bogdan.refueled.mixin.common.car;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.config.ServerConfig;
import com.dragn0007.dragnvehicles.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
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
            ((IVehicleAccess) car).refuel$initTemperature();
            if(ServerConfig.spawnFull.get()) {
                ((IVehicleAccess) car).refuel$setBattery(((IVehicleAccess) car).refuel$getMaxBattery());
                if(car.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().get().fill(new FluidStack(Fluids.LAVA, Integer.MAX_VALUE), IFluidHandler.FluidAction.EXECUTE) == 0){
                    RefueledMain.LOGGER.warn( "{} (UUID: {}) could not accept a tank full of 'minecraft:lava', check server config to fix issue", car.getDisplayName().getString(), car.getStringUUID());
                }
            }
        }
        return car;
    }
}
