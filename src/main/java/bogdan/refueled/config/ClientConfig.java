package bogdan.refueled.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;


    public static final ForgeConfigSpec.DoubleValue carZoom;
    public static final ForgeConfigSpec.DoubleValue carVolume;
    public static final ForgeConfigSpec.BooleanValue temperatureFahrenheit;
    public static final ForgeConfigSpec.BooleanValue speedImperial;
    public static final ForgeConfigSpec.BooleanValue speedDisplay;
    public static final ForgeConfigSpec.BooleanValue displayInUnits;
    public static final ForgeConfigSpec.IntValue pinnedType;
    public static final ForgeConfigSpec.BooleanValue reminderMessage;

    static {
        builder.push("cars");
        carZoom = builder
                .comment("Amount of zoom when sitting in a vehicle in 3rd person camera")
                .defineInRange("car_zoom", 4d, 0, 20d);

        carVolume = builder
                .comment("How loud the sound from the vehicles should be")
                .defineInRange("car_volume", 1d, 0, 1d);

        temperatureFahrenheit = builder
                .comment("Whether to display temperature as Fahrenheit")
                .define("fahrenheit_display", false);

        speedDisplay = builder
                .comment("Whether to display the current speed of the car you're in", "For unit system used, go to [speed_imperial]")
                .define("speed_display", true);

        speedImperial = builder
                .comment("If [speed_display] is enabled, to display speed in miles per hour")
                .define("speed_imperial", false);

        displayInUnits = builder
                .comment("Whether vehicle information in the gui should be display in percentages or units")
                .define("unit_display", false);

        pinnedType = builder
                .comment("Which stat's level should be shown as default in the meter bar of the vehicles' GUI")
                .comment("0 - None, 1 - Fuel, 2 - Health, 3 - Battery, 4 - Temperature")
                .defineInRange("pinned_type", 0, 0, 4);

        reminderMessage = builder
                .comment("Whether the player should be reminded how to start or open the vehicle's GUI")
                .define("reminder", true);

        builder.pop();
        SPEC = builder.build();
    }
}
