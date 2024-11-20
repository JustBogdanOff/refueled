package bogdan.refueled.mixin.car.client;

import bogdan.refueled.common.accessors.ICarInvoker;
import com.dragn0007.dragnvehicles.Animation;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;

import static bogdan.refueled.Utils.mod;

@Mixin(Animation.class)
public abstract class AnimationMixin implements ICarInvoker {

    @Mutable
    @Unique
    public void car$animate(ModelPart modelPart, Animation animation, float wheelRotation) {
        float t = mod(wheelRotation, animation.length);
        int i = 0;

        for(; i < animation.keyFrames.length; i++) {
            if(t < animation.keyFrames[i].time) {
                break;
            }
        }

        int idx1 = (int) mod(i - 1, animation.keyFrames.length);
        int idx2 = i % animation.keyFrames.length;

        Animation.KeyFrame k1 = animation.keyFrames[Math.min(idx1, idx2)];
        Animation.KeyFrame k2 = animation.keyFrames[Math.max(idx1, idx2)];

        float f = (t - k1.time) / (k2.time - k1.time);

        modelPart.xRot = k1.rot.x() + (k2.rot.x() - k1.rot.x()) * f;
        modelPart.yRot = k1.rot.y() + (k2.rot.y() - k1.rot.y()) * f;
        modelPart.zRot = k1.rot.z() + (k2.rot.z() - k1.rot.z()) * f;
    }
}
