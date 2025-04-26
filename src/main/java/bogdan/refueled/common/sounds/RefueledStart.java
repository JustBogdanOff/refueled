package bogdan.refueled.common.sounds;

import bogdan.refueled.common.accessors.IVehicleAccess;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class RefueledStart extends RefueledLoop {
    public RefueledStart(Entity car, SoundEvent event, SoundSource category) {
        super(car, event, category);
        this.looping = false;
    }

    @Override
    public boolean shouldStopSound() {
        return !((IVehicleAccess) car).refuel$isStarted();
    }
}
