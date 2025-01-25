package bogdan.refueled;

import bogdan.refueled.common.gui.CarGUI;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static bogdan.refueled.Utils.getCarByUUID;

public class RefueledRegistry {
    public static void init(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
        SOUND_REGISTER.register(modEventBus);
    }

    // GUI'S

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RefueledMain.MODID);

    public static final RegistryObject<MenuType<CarGUI>> CAR_GUI = MENU_TYPES.register("refueled_gui", () ->
            IForgeMenuType.create((windowId, inv, data) -> {
                Entity car = getCarByUUID(inv.player, data.readUUID());
                if (car == null) {
                    return null;
                }
                return new CarGUI(windowId, inv, (Container) car);
            })
    );

    // SOUNDS

    private static final DeferredRegister<SoundEvent> SOUND_REGISTER = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RefueledMain.MODID);

    public static final RegistryObject<SoundEvent> ENGINE = SOUND_REGISTER.register("engine", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RefueledMain.MODID, "engine")));
    public static final RegistryObject<SoundEvent> SPORT_ENGINE = SOUND_REGISTER.register("sport_engine", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RefueledMain.MODID, "sport_engine")));
    public static final RegistryObject<SoundEvent> TRUCK_ENGINE = SOUND_REGISTER.register("truck_engine", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RefueledMain.MODID, "truck_engine")));

    // DAMAGE TYPES

    public static final ResourceKey<DamageType>
            vehicleCollision = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(RefueledMain.MODID, "hit_by_vehicle")),
            vehicleExplosion = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(RefueledMain.MODID, "hit_by_vehicle_death"));
}
