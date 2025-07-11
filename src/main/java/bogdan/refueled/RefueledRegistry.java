package bogdan.refueled;

import bogdan.refueled.common.gui.CarGUI;
import bogdan.refueled.common.gui.TruckGUI;
import bogdan.refueled.common.items.Battery;
import bogdan.refueled.common.items.Canister;
import bogdan.refueled.mixin.common.accessor.ILevelAccess;
import bogdan.refueled.server.FuelTypeArgument;
import com.dragn0007.dragnvehicles.registry.ItemRegistry;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RefueledRegistry {
    public static void init(IEventBus modEventBus) {
        MENUS.register(modEventBus);
        SOUND_REGISTER.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ARGUMENT_TYPES.register(modEventBus);
    }
    // ITEMS

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RefueledMain.MODID);

    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery", Battery::new);
    public static final RegistryObject<Item> CANISTER = ITEMS.register("canister", Canister::new);


    // GUI'S

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RefueledMain.MODID);

    public static final RegistryObject<MenuType<CarGUI>> CAR_GUI = MENUS.register("vehicle_gui",
            () -> IForgeMenuType.create((id, playerInv, data) ->{
                Entity car = ((ILevelAccess) playerInv.player.level()).invokeGetEntities().get(data.readUUID());
                if(car == null) return null;

                return new CarGUI(id, car, playerInv);
            })
    );

    public static final RegistryObject<MenuType<TruckGUI>> TRUCK_GUI = MENUS.register("truck_gui",
            () -> IForgeMenuType.create((id, playerInv, data) ->{
                Entity truck = ((ILevelAccess) playerInv.player.level()).invokeGetEntities().get(data.readUUID());
                if(truck == null) return null;

                return new TruckGUI(id, truck, playerInv);
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

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RefueledMain.MODID);

    public static final RegistryObject<CreativeModeTab> REFUELED_TAB = CREATIVE_MODE_TABS.register("refueled_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.translatable("itemGroup.refueled.base"))
            .icon(() -> CANISTER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                ItemRegistry.ITEMS.getEntries().forEach(itemRegistry -> output.accept(itemRegistry.get().getDefaultInstance()));
                ITEMS.getEntries().forEach(itemReg -> output.accept(itemReg.get().getDefaultInstance()));
            })
            .build());

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, RefueledMain.MODID);

    public static final RegistryObject<ArgumentTypeInfo<FuelTypeArgument, ?>> FUEL_TYPE_ARGUMENT = ARGUMENT_TYPES.register("fuel_type_argument", () -> ArgumentTypeInfos.registerByClass(FuelTypeArgument.class, SingletonArgumentInfo.contextFree(FuelTypeArgument::id)));
}
