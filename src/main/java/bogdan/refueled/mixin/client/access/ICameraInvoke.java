package bogdan.refueled.mixin.client.access;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface ICameraInvoke {
    @Invoker("move")
    void cam$move(double pDistanceOffset, double pVerticalOffset, double pHorizontalOffset);

    @Invoker("getMaxZoom")
    double cam$getMaxZoom(double pStartingDistance);

    @Invoker("setPosition")
    void cam$setPosition(Vec3 pos);
}
