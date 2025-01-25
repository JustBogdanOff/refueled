package bogdan.refueled.mixin.accessor;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IEntityAccess {
    @Accessor("passengers")
    ImmutableList<Entity> getPassengers();

    @Accessor("passengers")
    void setPassengers(ImmutableList<Entity> entities);

    @Accessor("boardingCooldown")
    void setBoardingCooldown(int cooldown);
}
