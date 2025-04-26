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

public class ControlVehicle {
    private final boolean forward, backward, left, right;
    private final UUID uuid;

    public ControlVehicle(boolean forward, boolean backward, boolean left, boolean right, Player player) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.uuid = player.getUUID();
    }

    public ControlVehicle(FriendlyByteBuf buf) {
        forward = buf.readBoolean();
        backward = buf.readBoolean();
        left = buf.readBoolean();
        right = buf.readBoolean();
        uuid = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(forward);
        buf.writeBoolean(backward);
        buf.writeBoolean(left);
        buf.writeBoolean(right);
        buf.writeUUID(uuid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if(player == null){
            RefueledMain.LOGGER.error("Packet sender is null");
            return false;
        }
        if (!player.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("Mismatched sender and packet UUID");
            return false;
        }
        Entity car = player.getVehicle();
        if(!isCar(car)) return false;

        ((IVehicleAccess) car).refuel$updateControls(forward, backward, left, right, player);
        return true;
    }
}
