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

public class VehicleStarting {
    private final UUID uuid;
    private final boolean start, playFailSound;

    public VehicleStarting(boolean start, boolean playFailSound, Player player) {
        this.start = start;
        this.playFailSound = playFailSound;
        this.uuid = player.getUUID();
    }

    public VehicleStarting(FriendlyByteBuf buf) {
        start = buf.readBoolean();
        playFailSound = buf.readBoolean();
        uuid = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(start);
        buf.writeBoolean(playFailSound);
        buf.writeUUID(uuid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        ServerPlayer player = supplier.get().getSender();
        if(player == null){
            RefueledMain.LOGGER.error("null packet sender");
            return false;
        }
        if (!player.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("Mismatched sender and packet UUIDs");
            return false;
        }
        Entity car = player.getVehicle();
        if(!isCar(car)) return false;

        if (player.equals(car.getControllingPassenger()))
            ((IVehicleAccess) car).refuel$setStarting(start, playFailSound);
        return true;
    }
}
