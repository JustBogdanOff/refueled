package bogdan.refueled.common.network;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.IVehicleAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null){
            RefueledMain.LOGGER.error("Unable to process packet because client player is null");
            return;
        }

        Player driver = player.level().getPlayerByUUID(uuid);
        if(driver == null){
            RefueledMain.LOGGER.error("Could not center vehicle because it's driver is null");
            return;
        }

        if (!driver.getUUID().equals(uuid)) {
            RefueledMain.LOGGER.error("Mismatched sender and packet UUIDs");
            return;
        }

        Entity car = driver.getVehicle();
        if(!isCar(car))  return;

        if(driver.equals(car.getControllingPassenger())){
            ((IVehicleAccess) car).refuel$centerCar();
        }
    }
}

