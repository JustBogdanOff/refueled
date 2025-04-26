package bogdan.refueled.common.sounds;

import bogdan.refueled.common.accessors.IVehicleAccess;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class RefueledHigh extends RefueledLoop {

    public RefueledHigh(Entity car, SoundEvent event, SoundSource category) {
        super(car, event, category);
    }

    @Override
    public void tick() {
        pitch = ((IVehicleAccess) car).refuel$getPitch();
        super.tick();
    }

    @Override
    public boolean shouldStopSound() {
        return ((IVehicleAccess) car).refuel$getSpeed() == 0 || !((IVehicleAccess) car).refuel$isStarted();
    }
}
