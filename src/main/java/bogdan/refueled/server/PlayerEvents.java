package bogdan.refueled.server;

import bogdan.refueled.config.ServerConfig;
import bogdan.refueled.mixin.common.accessor.ILevelAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static bogdan.refueled.Utils.isCar;

public class PlayerEvents {
    public static final String REFUELED_KEY = "refueled_last_entity";

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event){
        if(ServerConfig.vehiclePersist.get() && !event.getEntity().level().isClientSide){
            ServerPlayer player = (ServerPlayer) event.getEntity();
            Entity car = player.getVehicle();
            if(isCar(car)) {
                var oldPos = player.position();
                player.stopRiding();
                player.setPos(oldPos);

                CompoundTag carTag = new CompoundTag();
                carTag.putUUID("vehicle", car.getUUID());
                carTag.putIntArray("pos", new int[]{car.getBlockX(), car.getBlockY(), car.getBlockZ()});
                player.getPersistentData().put(REFUELED_KEY, carTag);

                CompoundTag playerTag = car.getPersistentData().contains(REFUELED_KEY) ? car.getPersistentData().getCompound(REFUELED_KEY) : new CompoundTag();
                playerTag.putUUID(player.getDisplayName().getString(), player.getUUID());
                car.getPersistentData().put(REFUELED_KEY, playerTag);
            }
            else if(player.getPersistentData().contains(REFUELED_KEY)) player.getPersistentData().remove(REFUELED_KEY);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        if(ServerConfig.vehiclePersist.get() && !event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            if(player.getPersistentData().contains(REFUELED_KEY)) {
                CompoundTag tag = player.getPersistentData().getCompound(REFUELED_KEY);
                if(((ILevelAccess) player.level()).invokeGetEntities().get(tag.getUUID("vehicle")) != null){
                    Entity vehicle = player.serverLevel().getEntity(tag.getUUID("vehicle"));
                    if(vehicle == null){
                        player.getPersistentData().remove(REFUELED_KEY);
                        return;
                    }

                    if(vehicle.getBlockX() == tag.getIntArray("pos")[0] && vehicle.getBlockY() == tag.getIntArray("pos")[1] && vehicle.getBlockZ() == tag.getIntArray("pos")[2]){
                        player.startRiding(vehicle);
                    }
                }

                player.getPersistentData().remove(REFUELED_KEY);
            }
        }
    }
}
