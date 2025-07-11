package bogdan.refueled.common.network;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.IVehicleAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

import static bogdan.refueled.Utils.isCar;

public class VehicleCrash {
    private final UUID uuid;
    private final float speed;

    public VehicleCrash(Entity car, float speed) {
        this.uuid = car.getUUID();
        this.speed = speed;
    }

    public VehicleCrash(FriendlyByteBuf buf) {
        uuid = buf.readUUID();
        speed = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeFloat(speed);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        ServerPlayer player = supplier.get().getSender();
        if(player == null){
            RefueledMain.LOGGER.error("Packet sender is null");
            return false;
        }
        Entity car = player.getVehicle();
        if(car == null){
            RefueledMain.LOGGER.error("null vehicle when processing its crash");
            return false;
        }
        if (!car.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("Mismatched packet and vehicle UUID");
            return false;
        }

        if(!isCar(car)) return false;

        RefueledMain.LOGGER.debug("crashed the vehicle on the server");
        ((IVehicleAccess) car).refuel$onCollision(speed);
        return true;
    }
}
