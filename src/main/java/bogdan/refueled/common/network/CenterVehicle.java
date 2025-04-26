package bogdan.refueled.common.network;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.IVehicleAccess;
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
        if (player == null){
            RefueledMain.LOGGER.error("Packet sender is null");
            return false;
        }
        if(!player.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("Mismatched sender and packet UUID");
            return false;
        }

        Entity car = player.getVehicle();
        if(!isCar(car))  return false;

        if (player.equals(car.getControllingPassenger())) ((IVehicleAccess) car).refuel$centerCar();
        var msg = new CenterVehicleClient(player);
        player.serverLevel().getPlayers(serverPlayers -> serverPlayers.distanceTo(car) < 128).forEach(srvrPlyr -> RefueledChannel.sendToPlayer(msg, srvrPlyr));
        return true;
    }
}
