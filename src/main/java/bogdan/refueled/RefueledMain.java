package bogdan.refueled;

import bogdan.refueled.client.GasStationBlockEntityRenderer;
import bogdan.refueled.client.gui.CarGUIScreen;
import bogdan.refueled.client.events.KeyEvent;
import bogdan.refueled.client.events.RenderEvent;
import bogdan.refueled.client.gui.GasStationGUIScreen;
import bogdan.refueled.client.gui.TruckGUIScreen;
import bogdan.refueled.common.blocks.blockentities.GasStationBlockEntity;
import bogdan.refueled.common.gui.GasStationGUI;
import bogdan.refueled.server.PlayerEvents;
import bogdan.refueled.common.network.RefueledChannel;
import bogdan.refueled.config.ClientConfig;
import bogdan.refueled.config.ServerConfig;
import bogdan.refueled.server.RefueledCommands;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Mod(RefueledMain.MODID)
public class RefueledMain {
    public static final String MODID = "refueled";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RefueledMain() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ServerConfig.register(ModLoadingContext.get());

        RefueledRegistry.init(modEventBus);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new RefueledCommands());

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
            modEventBus.addListener(this::onRegisterKeybinds);
            modEventBus.addListener(this::clientSetup);
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        RefueledChannel.register();
        MinecraftForge.EVENT_BUS.register(new PlayerEvents());
    }

    @OnlyIn(Dist.CLIENT)
    public void clientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new KeyEvent());
        MinecraftForge.EVENT_BUS.register(new RenderEvent());


        event.enqueueWork(() -> {
            MenuScreens.register(RefueledRegistry.CAR_GUI.get(), CarGUIScreen::new);
            MenuScreens.register(RefueledRegistry.TRUCK_GUI.get(), TruckGUIScreen::new);
            MenuScreens.register(RefueledRegistry.GAS_STATION_GUI.get(), GasStationGUIScreen::new);
            BlockEntityRenderers.register(RefueledRegistry.GAS_STATION_BLOCK_ENTITY.get(), GasStationBlockEntityRenderer::new);
        });
    }

    public static KeyMapping CAR_GUI_KEY, START_KEY, CENTER_KEY;

    public void onRegisterKeybinds(RegisterKeyMappingsEvent event){
        CAR_GUI_KEY = new KeyMapping("key.refueled.car_gui", GLFW.GLFW_KEY_I, "category.refueled");
        START_KEY = new KeyMapping("key.refueled.car_start", GLFW.GLFW_KEY_R, "category.refueled");
        CENTER_KEY = new KeyMapping("key.refueled.center_car", GLFW.GLFW_KEY_SPACE, "category.refueled");

        event.register(CAR_GUI_KEY);
        event.register(START_KEY);
        event.register(CENTER_KEY);
    }
}
