package bogdan.refueled;

import bogdan.refueled.common.gui.CarGUI;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.UUID;

import static bogdan.refueled.Utils.getCarByUUID;

public class RefueledRegistry {
    public static void init(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }

    // GUI'S

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RefueledMain.MODID);

    public static final RegistryObject<MenuType<CarGUI>> CAR_GUI = MENU_TYPES.register("car_gui", () ->
            IForgeMenuType.create((windowId, inv, data) -> {
                Entity car = getCarByUUID(inv.player, data.readUUID());
                if (car == null) {
                    return null;
                }
                return new CarGUI(windowId, inv, (Container) car);
            })
    );

    // DAMAGE TYPES

    public static final ResourceKey<DamageType> CAR_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(RefueledMain.MODID, "hit_by_car"));
}
