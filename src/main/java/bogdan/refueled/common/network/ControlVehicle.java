package bogdan.refueled.common.network;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.ICarInvoker;
import net.minecraft.network.FriendlyByteBuf;
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

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        Player player = ctx.get().getSender();
        if (!player.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("The UUID of the sender was not equal to the packet UUID");
            return false;
        }
        Entity car = player.getVehicle();
        if(!isCar(car)) return false;

        ((ICarInvoker) car).car$updateControls(forward, backward, left, right, player);
        return true;
    }
}
