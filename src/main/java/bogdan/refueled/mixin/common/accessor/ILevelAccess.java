package bogdan.refueled.mixin.common.accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Level.class)
public interface ILevelAccess {
    @Invoker("getEntities")
    LevelEntityGetter<Entity> invokeGetEntities();
}
