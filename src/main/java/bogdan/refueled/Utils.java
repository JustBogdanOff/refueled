package bogdan.refueled;

import bogdan.refueled.config.ServerConfig;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class Utils {
    public static float mod(float n, float m) {
        while(n < 0) {
            n += m;
        }
        return n % m;
    }

    public static boolean isInBounds(float number, float bound, float tolerance) {
        if (number > bound - tolerance && number < bound + tolerance) {
            return true;
        }
        return false;
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

    public static boolean isRoadBlock(BlockState state){
        return ServerConfig.roadBlocks.get().stream().anyMatch(configTag -> configTag.startsWith("#") ? state.getTags().anyMatch(tag -> tag.equals(ForgeRegistries.BLOCKS.tags().getTag(BlockTags.create(new ResourceLocation(configTag.substring(1)))).getKey())) : state.getBlock().equals(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(configTag))));
    }

    public static boolean car$isSoundPlaying(SoundInstance sound) {
        if (sound == null) {
            return false;
        }
        return Minecraft.getInstance().getSoundManager().isActive(sound);
    }

    public static float getFuelEfficiency(Fluid fluid){
        int fluidEff = 0;

        if (fluid == null) {
            fluidEff = 100;
        } else {
            int[] fuelIndex = new int[1];
            boolean isFuel = ServerConfig.fuelEff.get().stream().anyMatch(fuelAndValue -> {
                if(fuelAndValue.get(0).equals(ForgeRegistries.FLUIDS.getKey(fluid).toString())){
                    fuelIndex[0] = ServerConfig.fuelEff.get().stream().toList().indexOf(fuelAndValue);
                }
                return fuelAndValue.get(0).equals(ForgeRegistries.FLUIDS.getKey(fluid).toString());
            });
            if (isFuel) {
                fluidEff = Integer.parseInt(ServerConfig.fuelEff.get().stream().toList().get(fuelIndex[0]).get(1));
            }
        }

        return fluidEff;
    }

    @Nullable
    public static Entity getCarByUUID(Player player, UUID uuid) {
        double distance = 10D;
        return player.level().getEntitiesOfClass(Entity.class, new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance), entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }

    public static float round(float value, int scale) {
        return (float) (Math.round(value * Math.pow(10, scale)) / Math.pow(10, scale));
    }
}
