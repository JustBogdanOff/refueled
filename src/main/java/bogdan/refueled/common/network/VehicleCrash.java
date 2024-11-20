package bogdan.refueled.common.network;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.ICarInvoker;
import net.minecraft.network.FriendlyByteBuf;
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
        Entity car = supplier.get().getSender().getVehicle();
        // Here we are server side
        if (!car.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("The UUID of the sender was not equal to the packet UUID");
            return false;
        }

        if(!isCar(car)) return false;

        ((ICarInvoker) car).car$onCollision(speed);
        return true;
    }
}
