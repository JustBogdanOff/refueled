package bogdan.refueled;

import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.common.blocks.blockentities.GasStationBlockEntity;
import com.dragn0007.dragnvehicles.item.*;
import com.dragn0007.dragnvehicles.vehicle.car.Car;
import com.dragn0007.dragnvehicles.vehicle.classic.Classic;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCar;
import com.dragn0007.dragnvehicles.vehicle.suv.SUV;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.List;

public abstract class Utils {
    public static float mod(float n, float m) {
        while(n < 0) {
            n += m;
        }
        return n % m;
    }

    public static boolean isInBounds(float number, float bound, float tolerance) {
        return number > bound - tolerance && number < bound + tolerance;
    }

    public static float subtractToZero(float num, float sub) {
        float erg;
        if (num < 0F) {
            erg = num + sub;
            if (erg > 0F) {
                erg = 0F;
            }
        } else {
            erg = num - sub;
            if (erg < 0F) {
                erg = 0F;
            }
        }

        return erg;
    }

    public static void readInventory(CompoundTag compound, String name, Container inv) {
        if (!compound.contains(name)) {
            return;
        }

        ListTag tagList = compound.getList(name, 10);

        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag slot = tagList.getCompound(i);
            int j = slot.getInt("Slot");

            if (j >= 0 && j < inv.getContainerSize()) {
                inv.setItem(j, ItemStack.of(slot));
            }
        }
    }

    public static void saveInventory(CompoundTag compound, String name, Container inv) {
        ListTag tagList = new ListTag();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putInt("Slot", i);
                inv.getItem(i).save(slot);
                tagList.add(slot);
            }
        }

        compound.put(name, tagList);
    }

    public static boolean isCar(Entity car){
        return car instanceof Car || car instanceof Classic || car instanceof Truck || car instanceof SUV || car instanceof SportCar || car instanceof Motorcycle;
    }

    public static boolean isCar(Item item){
        return item instanceof CarItem || item instanceof ClassicItem || item  instanceof TruckItem || item instanceof SUVItem || item instanceof SportCarItem || item instanceof MotorcycleItem;
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isSoundPlaying(SoundInstance sound) {
        if (sound == null)
            return false;

        return Minecraft.getInstance().getSoundManager().isActive(sound);
    }

    public static float round(float value, int scale) {
        return (float) (Math.floor(value * Math.pow(10, scale)) * Math.pow(0.1, scale));
    }

    public static int getHP(Entity car){
        return Mth.floor((((IVehicleAccess) car).refuel$getHealth() / ((IVehicleAccess) car).refuel$getMaxHealth()) * 100);
    }

    public static int[] getGasStationEntities(Level level, BlockPos pos){
        List<Entity> entities = level.getEntities(null, GasStationBlockEntity.getFuelingBox(pos, level.getBlockState(pos)));
        int[] array = new int[4];
        for(int i = 0; i < 4; i++){
            if(entities.size() - 1 >= i && entities.get(i).getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent())
                array[i] = entities.get(i).getId();
            else array[i] = -1;
        }

        return array;
    }
}
