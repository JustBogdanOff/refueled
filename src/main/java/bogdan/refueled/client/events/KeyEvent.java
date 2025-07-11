package bogdan.refueled.client.events;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.common.network.VehicleGUI;
import bogdan.refueled.common.network.VehicleStarting;
import bogdan.refueled.common.network.CenterVehicle;
import bogdan.refueled.common.network.RefueledChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static bogdan.refueled.Utils.isCar;

@OnlyIn(Dist.CLIENT)
public class KeyEvent {
    private boolean wasStartPressed, wasGuiPressed, wasCenterPressed;

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        Player player = mc.player;
        if (player == null) return;

        Entity car = player.getVehicle();
        if (!isCar(car)) return;
        if (player.equals(car.getControllingPassenger())) {
            ((IVehicleAccess) car).refuel$updateControls(mc.options.keyUp.isDown(), mc.options.keyDown.isDown(), mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(), player);

            if (RefueledMain.START_KEY.isDown()) {
                if (!wasStartPressed) {
                    RefueledChannel.sendToServer(new VehicleStarting(true, false, player));
                    wasStartPressed = true;
                }
            } else {
                if (wasStartPressed) {
                    RefueledChannel.sendToServer(new VehicleStarting(false, true, player));
                }
                wasStartPressed = false;
            }

            if (RefueledMain.CENTER_KEY.isDown()) {
                if (!wasCenterPressed) {
                    RefueledChannel.sendToServer(new CenterVehicle(player));
                    player.displayClientMessage(Component.translatable("message.center_vehicle"), true);
                    wasCenterPressed = true;
                }
            } else wasCenterPressed = false;
        }

        if (RefueledMain.CAR_GUI_KEY.isDown()) {
            if (!wasGuiPressed) {
                RefueledChannel.sendToServer(new VehicleGUI(player));
                wasGuiPressed = true;
            }
        } else wasGuiPressed = false;
    }
}
