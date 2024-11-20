package bogdan.refueled.mixin.accessor;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface ICameraInvoke {
    @Invoker("move")
    void invokeMove(double pDistanceOffset, double pVerticalOffset, double pHorizontalOffset);

    @Invoker("getMaxZoom")
    double invokeMaxZoom(double pStartingDistance);
}
