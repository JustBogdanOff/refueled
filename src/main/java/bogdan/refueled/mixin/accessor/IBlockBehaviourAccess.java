package bogdan.refueled.mixin.accessor;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.class)
public interface IBlockBehaviourAccess {
    @Accessor("hasCollision")
    boolean hasCollision();
}
