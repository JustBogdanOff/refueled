package bogdan.refueled.config;

import bogdan.refueled.RefueledMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class ServerConfig {
    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<List<Double>>> vehicleSteering;
    public static final ForgeConfigSpec.ConfigValue<List<Double>> vehicleSpeed;
    public static final ForgeConfigSpec.ConfigValue<List<Double>> vehicleRevSpeed;
    public static final ForgeConfigSpec.ConfigValue<List<Double>> vehicleAcc;
    public static final ForgeConfigSpec.ConfigValue<List<Double>> vehicleStepHeight;
    public static final ForgeConfigSpec.ConfigValue<List<Double>> vehicleRamDamage;
    public static final ForgeConfigSpec.ConfigValue<List<Double>> vehicleFuelEff;
    public static final ForgeConfigSpec.ConfigValue<List<Double>> vehicleHealth;
    public static final ForgeConfigSpec.ConfigValue<List<Integer>> vehicleFuel;
    public static final ForgeConfigSpec.ConfigValue<List<Integer>> vehicleBattery;

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<String>>> fuelEff;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<String>>> repairItems;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<String>>> roadBlocks;

    public static final ForgeConfigSpec.BooleanValue useBattery;
    public static final ForgeConfigSpec.BooleanValue damageEntities;
    public static final ForgeConfigSpec.BooleanValue collideWithEntities;
    public static final ForgeConfigSpec.DoubleValue offroadSpeed;

    public static final ForgeConfigSpec.IntValue canisterMax;
    public static final ForgeConfigSpec.IntValue batteryMax;
    public static final ForgeConfigSpec.BooleanValue explodeOnDeath;
    public static final ForgeConfigSpec.BooleanValue vehiclePersist;
    public static final ForgeConfigSpec.BooleanValue spawnFull;

    static {
        builder.push("refueled");
            builder.push("vehicles").comment("Each of these configs are represented using an array that affect in order respectively", "Modern cars, Classic cars, Trucks, SUVs, Sport cars, Motorcycles");
                vehicleSpeed = builder
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

                vehicleRevSpeed = builder
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
                        .comment("Acceleration for the vehicles")
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
                    .defineInRange("canister_max", 1500, 400, 16000);

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
                    .defineList("fuels", List.of(List.of("minecraft:lava", "100")), ServerConfig::validateFuel);

            builder.push("road_blocks");
                roadBlocks = builder
                        .comment("A list of blocks considered on-road for cars", "Any starting with '#' are considered a block tag")
                        .defineListAllowEmpty("blocks", List.of(List.of("minecraft:smooth_stone", "1.25"), List.of("#refueled:road_blocks", "1.5")), ServerConfig::validateBlock);
        builder.pop(2);
        SPEC = builder.build();
    }

    private static boolean validateSteering(final Object unsureList){
        if(unsureList instanceof List<?> list){
            if(list.size() != 6){
                if(list.size() < 6){
                    RefueledMain.LOGGER.warn("Steering server config list {} below expected size, defaulting.", 6 - list.size());
                    return false;
                }
                else RefueledMain.LOGGER.warn("Steering server config list {} above expected size", list.size() - 6);
            }

            for(var unsureValues : list){
                if(unsureValues instanceof List<?> values){
                    if(values.size() != 2){
                        if(values.size() < 2){
                            RefueledMain.LOGGER.warn("An array in the server config list is {} below expected size, defaulting.", 6 - list.size());
                            return false;
                        }
                    }

                    if(values.get(0) instanceof String minValue && values.get(1) instanceof String maxValue){
                        try{
                            if(Float.parseFloat(minValue) <= 0 || Float.parseFloat(maxValue) <= 0){
                                RefueledMain.LOGGER.warn("Zero or negative number in the steering server config list, defaulting.");
                                return false;
                            }
                        } catch (NumberFormatException exception){
                            RefueledMain.LOGGER.warn("Invalid number in one of the arrays of the steering server config list, defaulting.");
                            return false;
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
                    RefueledMain.LOGGER.warn("Server config list {} below expected size, defaulting.", 6 - list.size());
                    return false;
                }
                else RefueledMain.LOGGER.warn("Server config list {} above expected size.", list.size() - 6);
            }

            for(var unsureInt : list){
                if(unsureInt instanceof String number){
                    try{
                        if(Float.parseFloat(number) <= 0) {
                            RefueledMain.LOGGER.warn("Zero or negative number in one of the server config lists");
                            return false;
                        }
                    } catch (NumberFormatException exception){
                        RefueledMain.LOGGER.warn("Invalid number in one of the server config lists, defaulting.");
                        return false;
                    }
                }
            }

            return true;
        }

        return false;
    }

    private static boolean validateBlock(final Object element) {
        if(element instanceof List<?> list) {
            if(list.size() < 2){
                RefueledMain.LOGGER.warn("Incomplete server config list value @ road_blocks, discarding.");
                return false;
            }

            if(list.get(0) instanceof String block && list.get(1) instanceof String multiplier){
                try{
                    if(Float.parseFloat(multiplier) <= 0)
                        return false;
                } catch (NumberFormatException exception){
                    RefueledMain.LOGGER.warn("Invalid server config value @ road_blocks/{}, discarding.", block);
                    return false;
                }

                if (block.startsWith("#")) {
                    var blockTags = ForgeRegistries.BLOCKS.tags();
                    if (!ResourceLocation.isValidResourceLocation(block.substring(1)) || blockTags == null)
                        return false;

                    return blockTags.getTagNames().anyMatch(blockTag -> blockTag.location().toString().equals(block.substring(1)));
                }

                if (!ResourceLocation.isValidResourceLocation(block)) return false;
                return ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(block));
            }
        }

        return false;
    }

    private static boolean validateFuel(final Object element) {
        if(element instanceof List<?> list) {
            if(list.size() < 2){
                RefueledMain.LOGGER.warn("Incomplete server config list value @ fuels, discarding.");
                return false;
            }

            if(list.get(0) instanceof String fluid && list.get(1) instanceof String efficiency) {
                try{
                    if(Float.parseFloat(efficiency) <= 0)
                        return false;
                } catch(NumberFormatException exception){
                    RefueledMain.LOGGER.warn("Invalid server config value @ fuels/{}, discarding.", fluid);
                    return false;
                }

                if(!ResourceLocation.isValidResourceLocation(fluid))
                    return false;

                return ForgeRegistries.FLUIDS.getKeys().contains(new ResourceLocation(fluid));
            }
        }

        return false;
    }

    private static boolean validateRepairItem(final Object element){
        if(element instanceof List<?> list){
            if(list.size() < 3){
                RefueledMain.LOGGER.warn("Incomplete server config list value in repair_items, discarding.");
                return false;
            }

            if(list.get(0) instanceof String itemOrTag && list.get(1) instanceof String count && list.get(2) instanceof String repairValue){
                try{
                    if(Integer.parseInt(count) < 0 || Float.parseFloat(repairValue) <= 0)
                        return false;
                } catch(NumberFormatException exception){
                    RefueledMain.LOGGER.warn("Invalid server config value @ repair_items/{}, discarding.", itemOrTag);
                    return false;
                }

                if (itemOrTag.startsWith("#")) {
                    var itemTags = ForgeRegistries.ITEMS.tags();
                    if (!ResourceLocation.isValidResourceLocation(itemOrTag.substring(1)) || itemTags == null)
                        return false;

                    return itemTags.getTagNames().anyMatch(tagKey -> tagKey.location().toString().equals(itemOrTag.substring(1)));
                }

                if (!ResourceLocation.isValidResourceLocation(itemOrTag))
                    return false;

                return ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemOrTag));
            }
        }

        return false;
    }
}
