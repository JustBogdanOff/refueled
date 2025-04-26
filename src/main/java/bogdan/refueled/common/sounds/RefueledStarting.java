package bogdan.refueled.common.sounds;

import bogdan.refueled.common.accessors.IVehicleAccess;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import static bogdan.refueled.Utils.isCar;

public class RefueledStarting extends RefueledLoop {

    public RefueledStarting(Entity car, SoundEvent event, SoundSource category) {
        super(car, event, category);
        this.looping = true;
    }

    @Override
    public void tick() {
        if (isCar(car)) {
            pitch = ((IVehicleAccess) car).refuel$getBatterySoundPitchLevel();
        }
        super.tick();
    }

    @Override
    public boolean shouldStopSound() {
        if (!isCar(car)) {
            return true;
        }
        return !((IVehicleAccess) car).refuel$isStarting();
    }
}
