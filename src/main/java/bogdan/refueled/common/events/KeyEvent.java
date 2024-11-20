package bogdan.refueled.common.events;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.ICarInvoker;
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

    public KeyEvent() {

    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        Entity car = player.getVehicle();
        if (!isCar(car)) {
            return;
        }
        if (player.equals(car.getControllingPassenger())) {
            ((ICarInvoker) car).car$updateControls(Minecraft.getInstance().options.keyUp.isDown(), Minecraft.getInstance().options.keyDown.isDown(), Minecraft.getInstance().options.keyLeft.isDown(), Minecraft.getInstance().options.keyRight.isDown(), player);
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
                    player.displayClientMessage(Component.translatable("message.center_car"), true);
                    wasCenterPressed = true;
                }
            } else {
                wasCenterPressed = false;
            }
        }

        if (RefueledMain.CAR_GUI_KEY.isDown()) {
            if (!wasGuiPressed) {
                RefueledChannel.sendToServer(new VehicleGUI(player));
                wasGuiPressed = true;
            }
        } else {
            wasGuiPressed = false;
        }
    }
}
