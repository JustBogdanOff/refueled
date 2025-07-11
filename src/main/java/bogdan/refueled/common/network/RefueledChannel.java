package bogdan.refueled.common.network;

import bogdan.refueled.RefueledMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class RefueledChannel {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(RefueledMain.MODID, "refueled"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(CenterVehicle.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CenterVehicle::new)
                .encoder(CenterVehicle::toBytes)
                .consumerMainThread(CenterVehicle::handle)
                .add();

        net.messageBuilder(VehicleGUI.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(VehicleGUI::new)
                .encoder(VehicleGUI::toBytes)
                .consumerMainThread(VehicleGUI::handle)
                .add();

        net.messageBuilder(VehicleStarting.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(VehicleStarting::new)
                .encoder(VehicleStarting::toBytes)
                .consumerMainThread(VehicleStarting::handle)
                .add();

        net.messageBuilder(VehicleCrash.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(VehicleCrash::new)
                .encoder(VehicleCrash::toBytes)
                .consumerMainThread(VehicleCrash::handle)
                .add();

        net.messageBuilder(ControlVehicle.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ControlVehicle::new)
                .encoder(ControlVehicle::toBytes)
                .consumerMainThread(ControlVehicle::handle)
                .add();

        net.messageBuilder(CenterVehicleClient.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CenterVehicleClient::new)
                .encoder(CenterVehicleClient::toBytes)
                .consumerMainThread(CenterVehicleClient::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
