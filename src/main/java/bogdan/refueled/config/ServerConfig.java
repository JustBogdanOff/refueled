package bogdan.refueled.config;

import bogdan.refueled.RefueledMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ServerConfig SERVER;

    static {
        final Pair<ServerConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static void register(final ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    public static BooleanValue explodeOnDeath;
    public static BooleanValue vehiclePersist;
    public static BooleanValue spawnFull;
    public static BooleanValue useBattery;
    public static BooleanValue damageEntities;
    public static BooleanValue collideWithEntities;

    public static IntValue canisterMax;
    public static IntValue batteryMax;

    public static DoubleValue offroadSpeed;

    public static ConfigValue<List<Number>> vehicleFuel;
    public static ConfigValue<List<Number>> vehicleBattery;
    public static ConfigValue<List<Number>> vehicleMaxSpeed;
    public static ConfigValue<List<Number>> vehicleMaxRevSpeed;
    public static ConfigValue<List<Number>> vehicleAcc;
    public static ConfigValue<List<Number>> vehicleStepHeight;
    public static ConfigValue<List<Number>> vehicleRamDamage;
    public static ConfigValue<List<Number>> vehicleFuelEff;
    public static ConfigValue<List<Number>> vehicleHealth;
    public static ConfigValue<List<? extends String>> solidEntities;
    public static ConfigValue<List<List<Number>>> vehicleSteering;
    public static ConfigValue<List<? extends List<String>>> fuelEff;
    public static ConfigValue<List<? extends List<String>>> repairItems;
    public static ConfigValue<List<? extends List<String>>> roadBlocks;

    public ServerConfig(ForgeConfigSpec.Builder builder){
        builder.push("vehicles")
                    .comment("Each of these configs are represented using an array that affect in order respectively", "Modern cars, Classic cars, Trucks, SUVs, Sport cars, Motorcycles");
        vehicleMaxSpeed = builder
                .comment("Maximal speeds the vehicles can reach in blocks per tick")
                .define("speed", List.of(
                        // body * engine * dragn007
                        0.85 * 0.75 * 1.2,
                        0.9 * 0.75 * 1.18,
                        0.8 * 0.65 * 1.15,
                        0.8 * 0.65 * 1.15,
                        1.2 * 0.9 * 1.24,
                        1.2 * 0.75 * 1.28
                ), ServerConfig::validateNumber);

        vehicleMaxRevSpeed = builder
                .comment("Vehicles' top reverse speeds in blocks per tick")
                .define("reverse_speed", List.of(
                        // engine * dragn007
                        0.2 * 1.2,
                        0.2 * 1.18,
                        0.15 * 1.15,
                        0.15 * 1.15,
                        0.25 * 1.25,
                        0.2 * 1.28
                ), ServerConfig::validateNumber);

        vehicleAcc = builder
                .comment("Acceleration for the vehicles")
                .define("acceleration", List.of(
                        // body * engine * dragn007
                        0.04 * 0.95 * 1.2,
                        0.04 * 1 * 1.18,
                        0.035 * 0.8 * 1.15,
                        0.035 * 0.8 * 1.15,
                        0.03 * 1 * 1.24,
                        0.04 * 1 * 1.28
                ), ServerConfig::validateNumber);

        vehicleHealth = builder
                .comment("Vehicles' max health")
                .define("health", List.of(
                        100d,
                        100d,
                        150d,
                        125d,
                        80d,
                        60d
                ), ServerConfig::validateNumber);

        vehicleFuelEff = builder
                .comment("Vehicles' fuel consumption", "Higher = More efficient")
                .define("fuel_efficiency", List.of(
                        0.7 * 0.5,
                        0.8 * 0.5,
                        0.6 * 0.8,
                        0.6 * 0.8,
                        0.9 * 0.25,
                        0.9 * 0.5
                ), ServerConfig::validateNumber);

        vehicleSteering = builder
                .comment("How much the vehicles can steer at max speed and almost full stop",
                        "Having both values equal will keep steering constant, irrelevant of speed")
                .define("steering", List.of(
                        List.of(1.1, 3.3),
                        List.of(1.1, 3.3),
                        List.of(1.1, 3.3),
                        List.of(1.1, 3.3),
                        List.of(1.1, 3.3),
                        List.of(1.1, 4.95)
                ), ServerConfig::validateSteering);

        vehicleStepHeight = builder
                .comment("Vehicles' max stepping height")
                .define("step_height", List.of(
                        1d, 1d, 2d, 1.6, 0.6, 2d
                ), ServerConfig::validateNumber);

        vehicleRamDamage = builder
                .comment("Baseline damage to calculate onto mobs hit by the vehicles")
                .define("ram_damage", List.of(
                        20d, 20d, 30d, 25d, 15d, 10d
                ), ServerConfig::validateNumber);

        vehicleFuel = builder
                .comment("Vehicles' fuel capacity")
                .define("fuel_capacity", List.of(
                        1500, 1500, 2000, 2000, 1000, 1000
                ), ServerConfig::validateNumber);

        vehicleBattery = builder
                .comment("Vehicles' fuel capacity")
                .define("battery_capacity", List.of(
                        8000, 8000, 10000, 10000, 6000, 4000
                ), ServerConfig::validateNumber);
        builder.pop();

        useBattery = builder
                .comment("Whether to use the battery")
                .define("use_battery", false);

        damageEntities = builder
                .comment("Whether to damage any entities in the way of the vehicle")
                .define("damage_entities", true);

        collideWithEntities = builder
                .comment("Whether to stop the car as if it came in collision with a block when impacting an entity")
                .define("collide_with_entities", false);

        offroadSpeed = builder
                .comment("Speed modifier for DragN's vehicles on non-road blocks")
                .defineInRange("offroad_speed", 1d, 0.001d, 10d);

        explodeOnDeath = builder
                .comment("Whether the vehicle should cause an explosion on death")
                .define("explode", false);

        canisterMax = builder
                .comment("How much can the canister hold of a fluid, in [mB]")
                .defineInRange("canister_max", 2000, 400, 16000);

        batteryMax = builder
                .comment("How much the battery can hold FE")
                .defineInRange("battery_max", 6000, 400, 16000);

        vehiclePersist = builder
                .comment("Whether vehicles should persist in-game when it's only passenger disconnects")
                .define("vehicle_persist", true);

        spawnFull = builder
                .comment("Whether spawn eggs should spawn full tank and battery vehicles")
                .define("spawn_full", true);

        repairItems = builder
                .comment("What items should be considered vehicle-repairable, along with how much of said item is required to repair, and how much HP should it repair", "Any starting with '#' are considered an item tag")
                .defineList("repair_items", List.of(List.of("minecraft:iron_ingot", "4", "4.5"), List.of("#forge:ingots/steel", "1", "6")), ServerConfig::validateRepairItem);

        fuelEff = builder
                .comment("Fluids defined as acceptable fuels for vehicles, along with their efficiency")
                .defineList("fuels", List.of(List.of("minecraft:lava", "1")), ServerConfig::validateFuel);

        solidEntities = builder
                .comment("Running over entity blacklist", "Having collide_with_entities enabled makes this a whitelist instead")
                .defineList("solid_entities", List.of("minecraft:ender_dragon"), ServerConfig::validateEntity);

        builder.push("road_blocks");
            roadBlocks = builder
                    .comment("A list of blocks considered on-road for cars", "Any starting with '#' are considered a block tag")
                    .defineListAllowEmpty("blocks", List.of(List.of("minecraft:smooth_stone", "1.25"), List.of("#refueled:road_blocks", "1.5")), ServerConfig::validateBlock);
        builder.pop();
    }

    private static boolean validateEntity(final Object unsureElement){
        if(unsureElement instanceof String resourceLocation)
            return ResourceLocation.isValidResourceLocation(resourceLocation);

        return false;
    }

    private static boolean validateSteering(final Object unsureList){
        if(unsureList instanceof List<?> list){
            if(list.size() != 6){
                if(list.size() < 6)
                    return warn("Steering server config list {} below expected size, defaulting.", 6 - list.size());
                else RefueledMain.LOGGER.warn("Steering server config list {} above expected size", list.size() - 6);
            }

            for(var unsureValues : list){
                if(unsureValues instanceof List<?> values){
                    if(values.size() != 2){
                        if(values.size() < 2)
                            return warn("An array in the steering config list is {} below expected size, defaulting.", 6 - list.size());
                        else RefueledMain.LOGGER.warn("An array in the steering config list is {} above expected size.", 6 - list.size());
                    }

                    if(values.get(0) instanceof String minValue && values.get(1) instanceof String maxValue){
                        try{
                            if(Float.parseFloat(minValue) <= 0 || Float.parseFloat(maxValue) <= 0){
                                RefueledMain.LOGGER.warn("Zero or negative number in the steering server config list, defaulting.");
                                return false;
                            }
                        } catch (NumberFormatException exception){
                            return warn("Unparsable number in one of the arrays of the steering server config list, defaulting.");
                        }
                    }
                }
            }

            return true;
        }

        return false;
    }

    private static boolean validateNumber(final Object unsureList){
        if(unsureList instanceof List<?> list){
            if(list.size() != 6){
                if(list.size() < 6){
                    return warn("Server config list {} below expected size, defaulting.", 6 - list.size());
                }
                else RefueledMain.LOGGER.warn("Server config list {} above expected size.", list.size() - 6);
            }

            for(var unsureInt : list){
                if(unsureInt instanceof String number){
                    try{
                        if(Double.parseDouble(number) <= 0) {
                            RefueledMain.LOGGER.warn("Zero or negative number in one of the server config lists");
                            return false;
                        }
                    } catch (NumberFormatException exception){
                        return warn("Unparsable number in one of the server config lists, defaulting.");
                    }
                }
            }

            return true;
        }

        return false;
    }

    private static boolean validateBlock(final Object element) {
        if(element instanceof List<?> list) {
            if(list.size() < 2)
                return warn("Incomplete server config list value @ road_blocks, discarding.");

            if(list.get(0) instanceof String block && list.get(1) instanceof String multiplier){
                try{
                    if(Double.parseDouble(multiplier) <= 0)
                        return false;
                } catch (NumberFormatException exception){
                    return warn("Invalid server config value @ road_blocks/{}, discarding.", block);
                }

                if (block.startsWith("#"))
                    block = block.substring(1);
                return ResourceLocation.isValidResourceLocation(block);
            }
        }

        return false;
    }

    private static boolean validateFuel(final Object element) {
        if(element instanceof List<?> list) {
            if(list.size() < 2)
                return warn("Incomplete server config list value @ fuels, discarding.");

            if(list.get(0) instanceof String fluid && list.get(1) instanceof String efficiency) {
                try{
                    if(Double.parseDouble(efficiency) <= 0)
                        return false;
                } catch(NumberFormatException exception){
                    return warn("Invalid server config value @ fuels/{}, discarding.", fluid);
                }

                return ResourceLocation.isValidResourceLocation(fluid);
            }
        }

        return false;
    }

    private static boolean validateRepairItem(final Object element){
        if(element instanceof List<?> list){
            if(list.size() < 3)
                return warn("Incomplete server config list value in repair_items, discarding.");

            if(list.get(0) instanceof String itemOrTag && list.get(1) instanceof String count && list.get(2) instanceof String repairValue){
                try{
                    if(Integer.parseInt(count) < 0 || Float.parseFloat(repairValue) <= 0)
                        return false;
                } catch(NumberFormatException exception){
                    return warn("Invalid server config value @ repair_items/{}, discarding.", itemOrTag);
                }

                if (itemOrTag.startsWith("#"))
                    itemOrTag = itemOrTag.substring(1);
                return ResourceLocation.isValidResourceLocation(itemOrTag);
            }
        }

        return false;
    }

    private static boolean warn(String warning, Object... var){
        RefueledMain.LOGGER.warn(warning, var);
        return false;
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

    public static boolean isEntityOnList(Entity entity){
        if(entity == null) return false;

        for (String entityString : ServerConfig.solidEntities.get()) {
            if(ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(entityString)))
                return true;
        }

        return false;
    }
}
