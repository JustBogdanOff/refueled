package bogdan.refueled.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;


    public static final ForgeConfigSpec.DoubleValue carZoom;
    public static final ForgeConfigSpec.DoubleValue carVolume;
    public static final ForgeConfigSpec.BooleanValue thirdPersonCameraEnter;
    public static final ForgeConfigSpec.BooleanValue temperatureFahrenheit;
    public static final ForgeConfigSpec.BooleanValue speedImperial;
    public static final ForgeConfigSpec.BooleanValue speedDisplay;

    static {
        builder.push("cars");
        carZoom = builder
                .comment("Amount of zoom when sitting in a vehicle in 3rd person camera")
                .defineInRange("car_zoom", 4d, 0, 20d);

        carVolume = builder
                .comment("How loud the sound from the vehicles should be")
                .defineInRange("car_volume", 1d, 0, 1d);

        thirdPersonCameraEnter = builder
                .comment("Whether to switch to 3rd person camera when entering a vehicle")
                .define("third_person_camera_enter", true);

        temperatureFahrenheit = builder
                .comment("Whether to display temperature as Fahrenheit")
                .define("fahrenheit_display", false);

        speedDisplay = builder
                .comment("Whether to display the current speed of the car you're in", "For unit system used, go to [speed_imperial]")
                .define("speed_display", true);

        speedImperial = builder
                .comment("If [speed_display] is enabled, to display speed in miles per hour")
                .define("speed_imperial", false);

        builder.pop();
        SPEC = builder.build();
    }
}
