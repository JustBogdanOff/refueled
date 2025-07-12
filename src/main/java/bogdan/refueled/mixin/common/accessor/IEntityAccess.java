package bogdan.refueled.mixin.common.accessor;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface IEntityAccess {
    @Accessor("passengers")
    ImmutableList<Entity> getPassengers();

    @Accessor("passengers")
    void setPassengers(ImmutableList<Entity> entities);

    @Accessor("boardingCooldown")
    void setBoardingCooldown(int cooldown);

    @Accessor("nextStep")
    void setNextStep(float nextStep);

    @Accessor("nextStep")
    float getNextStep();

    @Invoker("vibrationAndSoundEffectsFromBlock")
    boolean invokeVaSEFB(BlockPos pPos, BlockState pState, boolean pPlayStepSound, boolean pBroadcastGameEvent, Vec3 p_286448_);
}
