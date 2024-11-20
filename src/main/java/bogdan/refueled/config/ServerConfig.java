package bogdan.refueled.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.*;

public class ServerConfig {
    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;


    public static final ForgeConfigSpec.DoubleValue modernStepHeight;
    public static final ForgeConfigSpec.DoubleValue classicStepHeight;
    public static final ForgeConfigSpec.DoubleValue truckStepHeight;
    public static final ForgeConfigSpec.DoubleValue suvStepHeight;
    public static final ForgeConfigSpec.DoubleValue sportStepHeight;
    public static final ForgeConfigSpec.DoubleValue bikeStepHeight;

    public static final ForgeConfigSpec.DoubleValue modernRamDamage;
    public static final ForgeConfigSpec.DoubleValue classicRamDamage;
    public static final ForgeConfigSpec.DoubleValue truckRamDamage;
    public static final ForgeConfigSpec.DoubleValue suvRamDamage;
    public static final ForgeConfigSpec.DoubleValue sportRamDamage;
    public static final ForgeConfigSpec.DoubleValue bikeRamDamage;

    public static final ForgeConfigSpec.DoubleValue modernFuelEff;
    public static final ForgeConfigSpec.DoubleValue classicFuelEff;
    public static final ForgeConfigSpec.DoubleValue truckFuelEff;
    public static final ForgeConfigSpec.DoubleValue suvFuelEff;
    public static final ForgeConfigSpec.DoubleValue sportFuelEff;
    public static final ForgeConfigSpec.DoubleValue bikeFuelEff;

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<String>>> fuelEff;

    public static final ForgeConfigSpec.BooleanValue hornFlee;
    public static final ForgeConfigSpec.BooleanValue useBattery;
    public static final ForgeConfigSpec.BooleanValue damageEntities;
    public static final ForgeConfigSpec.BooleanValue collideWithEntities;
    public static final ForgeConfigSpec.DoubleValue offroadSpeed;
    public static final ForgeConfigSpec.DoubleValue onroadSpeed;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> roadBlocks;

    public static final ForgeConfigSpec.DoubleValue repairKitAmount;
    public static final ForgeConfigSpec.BooleanValue useSubstitutes;
    public static final ForgeConfigSpec.BooleanValue explodeOnDeath;

    static {
        builder.push("car");
        builder.push("vehicles");
        builder.push("modern");
            modernStepHeight = builder
                    .comment("Stepping height for modern cars")
                    .defineInRange("step_height", 1d, 0, 8d);
            modernRamDamage = builder
                    .comment("Damage applied to entities ran over by modern cars")
                    .defineInRange("damage", 20d, 0, 100d);
            modernFuelEff = builder
                .comment("Modern cars' fuel consumption")
                .defineInRange("fuel_eff", 0.7d * 0.5d, 0, 10d);
        builder.pop();

        builder.push("classic");
        classicStepHeight = builder
                .comment("Stepping height for classic cars")
                .defineInRange("step_height", 1d, 0, 8d);
        classicRamDamage = builder
                .comment("Damage applied to entities ran over by classic cars")
                .defineInRange("damage", 20d, 0, 100d);
        classicFuelEff = builder
                .comment("Classic cars' fuel consumption")
                .defineInRange("fuel_eff", 0.8d * 0.5d, 0, 10d);
        builder.pop();

        builder.push("truck");
        truckStepHeight = builder
                .comment("Stepping height for trucks")
                .defineInRange("step_height", 2d, 0, 8d);
        truckRamDamage = builder
                .comment("Damage applied to entities ran over by trucks")
                .defineInRange("damage", 30d, 0, 100d);
        truckFuelEff = builder
                .comment("Trucks' fuel consumption")
                .defineInRange("fuel_eff", 0.6d * 0.8d, 0, 10d);
        builder.pop();

        builder.push("suv");
        suvStepHeight = builder
                .comment("Stepping height for SUVs")
                .defineInRange("step_height", 1.6d, 0, 8d);
        suvRamDamage = builder
                .comment("Damage applied to entities ran over by SUVs")
                .defineInRange("damage", 25d, 0, 100d);
        suvFuelEff = builder
                .comment("SUVs cars' fuel consumption")
                .defineInRange("fuel_eff", 0.6d * 0.8d, 0, 10d);
        builder.pop();

        builder.push("sport");
        sportStepHeight = builder
                .comment("Stepping height for sport cars")
                .defineInRange("step_height", 0.6d, 0, 8d);
        sportRamDamage = builder
                .comment("Damage applied to entities ran over by sport cars")
                .defineInRange("damage", 15d, 0, 100d);
        sportFuelEff = builder
                .comment("Sport cars' fuel consumption")
                .defineInRange("fuel_eff", 0.9d * 0.25d, 0, 10d);
        builder.pop();

        builder.push("bike");
        bikeStepHeight = builder
                .comment("Stepping height for motorcycles")
                .defineInRange("step_height", 2d, 0, 8d);
        bikeRamDamage = builder
                .comment("Damage applied to entities ran over by motorcycles")
                .defineInRange("damage", 10d, 0, 100d);
        bikeFuelEff = builder
                .comment("Motorcycles' fuel consumption")
                .defineInRange("fuel_eff", 0.9d * 0.5d, 0, 10d);
        builder.pop(2);

        fuelEff = builder
                .comment("Fluids defined as acceptable fuels for vehicles, along with their efficiency")
                .defineList("fuels", List.of(List.of("car:bio_diesel", "100")), ServerConfig::validateFuel);

        useBattery = builder
                .comment("Whether to use the battery")
                .define("use_battery", false);

        hornFlee = builder
                .comment("Whether mobs run away from the horn")
                .define("horn_flee", true);

        damageEntities = builder
                .comment("Whether to damage any entities in the way of the vehicle")
                .define("damage_entities", true);

        collideWithEntities = builder
                .comment("Whether to stop the car as if it came in collision with a block when impacting an entity")
                .define("collide_with_entities", false);

        offroadSpeed = builder
                .comment("Speed modifier for DragN's vehicles on non-road blocks")
                .defineInRange("offroad_speed", 1d, 0.001d, 10d);

        onroadSpeed = builder
                .comment("The speed modifier for cars on road blocks", "On road blocks are defined in the 'road_blocks' section of this config")
                .defineInRange("onroad_speed", 1.5d, 0.001d, 10d);

        builder.push("road_blocks");
        roadBlocks = builder
                .comment("A list of blocks considered on-road for cars", "If it starts with '#' it is a tag")
                .defineList("blocks", List.of("#refueled:road_blocks", "#car:asphalt_blocks"), ServerConfig::validateBlock);
        builder.pop();

        repairKitAmount = builder
                .comment("Defines how much the repair kit should heal the car")
                .defineInRange("repair_kit_amount", 5f, 0f, 100f);

        useSubstitutes = builder
                .comment("Whether to allow Ultimate Car Mod item substitutes (Coal block for fuel, iron block for repair kit, redstone dust for battery)")
                .define("use_substitutes", true);

        explodeOnDeath = builder
                .comment("Whether the vehicle should cause an explosion on death")
                .define("explode", true);

        builder.pop();
        SPEC = builder.build();
    }

    private static boolean validateBlock(final Object obj)
    {
        if(obj instanceof String tag) {
            if (tag.startsWith("#")) {
                if (!ResourceLocation.isValidResourceLocation(tag.substring(1))) {
                    return false;
                }
                return ForgeRegistries.BLOCKS.tags().getTagNames().anyMatch(blockTag -> blockTag.location().toString().equals(tag.substring(1)));
            }

            if (!ResourceLocation.isValidResourceLocation(tag)) {
                return false;
            }
            return ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(tag));
        }
        return false;
    }

    private static boolean validateFuel(final Object obj)
    {
        if(obj instanceof List<?> list) {
            if(list.get(0) instanceof String fluid && list.get(1) instanceof String value) {
                if(!ResourceLocation.isValidResourceLocation(fluid) || !NumberUtils.isCreatable(value)){
                    return false;
                }
                return ForgeRegistries.FLUIDS.containsKey(new ResourceLocation(fluid));
            }
        }
        return false;
    }
}
