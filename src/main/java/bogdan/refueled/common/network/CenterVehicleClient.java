package bogdan.refueled.common.network;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.ICarInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

import static bogdan.refueled.Utils.isCar;

public class CenterVehicleClient {
    private final UUID uuid;

    public CenterVehicleClient(Player player) {
        this.uuid = player.getUUID();
    }

    public CenterVehicleClient(FriendlyByteBuf buf) {
        uuid = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleOnClient);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private void handleOnClient(){
        Player player = Minecraft.getInstance().player;
        Player driver = player.level().getPlayerByUUID(uuid);
        if (!driver.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("The UUID of the sender was not equal to the packet UUID");
            return;
        }

        Entity car = driver.getVehicle();
        if(!isCar(car)) {
            return;
        }

        if(driver.equals(car.getControllingPassenger())){
            ((ICarInvoker) car).car$centerCar();
        }
    }
}

