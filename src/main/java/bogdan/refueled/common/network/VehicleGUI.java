package bogdan.refueled.common.network;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.IVehicleAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

import static bogdan.refueled.Utils.isCar;

public class VehicleGUI {
    private final UUID uuid;

    public VehicleGUI(Player player) {
        this.uuid = player.getUUID();
    }

    public VehicleGUI(FriendlyByteBuf buf) {
        uuid = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        ServerPlayer player = supplier.get().getSender();
        if(player == null){
            RefueledMain.LOGGER.error("Packet sender is null");
            return false;
        }

        if (!player.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("Mismatched packet and sender UUID");
            return false;
        }
        if(!isCar(player.getVehicle())) return false;

        ((IVehicleAccess) player.getVehicle()).refuel$openGUI(player);
        return true;
    }
}
