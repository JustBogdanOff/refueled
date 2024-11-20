package bogdan.refueled.common.network;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.ICarInvoker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

import static bogdan.refueled.Utils.isCar;

public class CenterVehicle {
    private final UUID uuid;

    public CenterVehicle(Player player) {
        this.uuid = player.getUUID();
    }

    public CenterVehicle(FriendlyByteBuf buf) {
        uuid = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        ServerPlayer player = supplier.get().getSender();
        // Here we are server side
        if (!player.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("The UUID of the sender was not equal to the packet UUID");
            return false;
        }

        Entity car = player.getVehicle();
        if(!isCar(car)) {
            return false;
        }

        if (player.equals(car.getControllingPassenger())) {
            ((ICarInvoker) car).car$centerCar();
        }

        CenterVehicleClient msg = new CenterVehicleClient(player);
        player.serverLevel().getPlayers(serverPlayers -> serverPlayers.distanceTo(car) <= 128F).forEach(srvrPlyr -> RefueledChannel.sendToPlayer(msg, srvrPlyr));
        return true;
    }
}