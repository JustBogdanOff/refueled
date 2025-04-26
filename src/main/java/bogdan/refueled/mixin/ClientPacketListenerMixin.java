package bogdan.refueled.mixin;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static bogdan.refueled.Utils.isCar;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Redirect(
            method = "handleSetEntityPassengersPacket",
            at = @At(
            value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
                    ordinal = 0
            )
    )
    private MutableComponent car$injectTutorialMessage(String pKey, Object[] pArgs, ClientboundSetPassengersPacket pPacket){
        //noinspection DataFlowIssue
        if(ClientConfig.reminderMessage.get() && isCar(this.minecraft.player.getVehicle()))
            return Component.translatable("message.start_vehicle", RefueledMain.START_KEY.getTranslatedKeyMessage(), RefueledMain.CAR_GUI_KEY.getTranslatedKeyMessage());

        return Component.translatable("mount.onboard", this.minecraft.options.keyShift.getTranslatedKeyMessage());
    }
}
