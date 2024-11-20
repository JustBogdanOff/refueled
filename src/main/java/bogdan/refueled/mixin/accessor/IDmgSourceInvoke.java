package bogdan.refueled.mixin.accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DamageSources.class)
public interface IDmgSourceInvoke {
    @Invoker("source")
    DamageSource invokeSource(ResourceKey<DamageType> damageType);
}
