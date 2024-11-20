package bogdan.refueled.mixin.car;

import bogdan.refueled.common.accessors.ICarInvoker;
import com.dragn0007.dragnvehicles.item.*;
import de.maxhenkel.car.fluids.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({CarItem.class, ClassicItem.class, TruckItem.class, SUVItem.class, SportCarItem.class, MotorcycleItem.class})
public class CarSpawnMixin {
    @Redirect(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;"),
        remap = false
    )
    private Entity car$injectOnSpawn(EntityType<?> instance, ServerLevel serverLevel, ItemStack itemStack, Player player, BlockPos pos, MobSpawnType pPlayer, boolean pPos, boolean pSpawnType){
        Entity car = instance.spawn(serverLevel, itemStack, player, pos.above(), MobSpawnType.SPAWN_EGG, false, false);
        if(car != null){
            ((ICarInvoker) car).car$setBattery(((ICarInvoker) car).car$getMaxBattery());
            ((IFluidHandler) car).fill(new FluidStack(ModFluids.BIO_DIESEL.get(), ((ICarInvoker) car).car$getMaxFuel()), IFluidHandler.FluidAction.EXECUTE);
        }
        return car;
    }
}
