package bogdan.refueled;

import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.config.ServerConfig;
import com.dragn0007.dragnvehicles.item.*;
import com.dragn0007.dragnvehicles.vehicle.car.Car;
import com.dragn0007.dragnvehicles.vehicle.classic.Classic;
import com.dragn0007.dragnvehicles.vehicle.motorcycle.Motorcycle;
import com.dragn0007.dragnvehicles.vehicle.sportcar.SportCar;
import com.dragn0007.dragnvehicles.vehicle.suv.SUV;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

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

    public static float getRoadBlockMultiplier(BlockState state){
        for(var list : ServerConfig.roadBlocks.get()){
            var block = list.get(0);

            if(block.startsWith("#")){
                if(state.getTags().anyMatch(blockTag -> blockTag.location().toString().equals(block.substring(1)))){
                    return Float.parseFloat(list.get(1));
                }
            }
            else{
                var blockKey = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                if(blockKey != null && blockKey.toString().equals(block)){
                    return Float.parseFloat(list.get(1));
                }
            }
        }

        return 0;
    }

    public static float getFuelEfficiency(Fluid fluid){
        if (fluid != null) {
            for(List<String> fuelValue : ServerConfig.fuelEff.get()){
                if (fluid == ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fuelValue.get(0)))) {
                    return Float.parseFloat(fuelValue.get(1));
                }
            }
        }

        return 0;
    }

    public static List<String> getRepairItemData(ItemStack item){
        for(List<String> list : ServerConfig.repairItems.get()){
            var repairItem = list.get(0);

            if(repairItem.startsWith("#")){
                if(item.getTags().anyMatch(repairTag -> repairTag.location().toString().equals(repairItem.substring(1)))) {
                    return List.of(list.get(1), list.get(2));
                }
            }
            else {
                var itemKey = ForgeRegistries.ITEMS.getKey(item.getItem());
                if(itemKey != null && itemKey.toString().equals(repairItem)){
                    return List.of(list.get(1), list.get(2));
                }
            }
        }

        return null;
    }

    public static float round(float value, int scale) {
        return (float) (Math.floor(value * Math.pow(10, scale)) * Math.pow(0.1, scale));
    }

    public static int getHP(Entity car){
        return Mth.floor((((IVehicleAccess) car).refuel$getHealth() / ((IVehicleAccess) car).refuel$getMaxHealth()) * 100);
    }
}
