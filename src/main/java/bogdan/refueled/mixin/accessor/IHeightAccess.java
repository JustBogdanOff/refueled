package bogdan.refueled.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IHeightAccess {
    @Accessor("eyeHeight")
    void setEyeHeight(float height);
}
