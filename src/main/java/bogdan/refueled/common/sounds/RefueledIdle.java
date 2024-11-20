package bogdan.refueled.common.sounds;

import bogdan.refueled.common.accessors.ICarInvoker;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class RefueledIdle extends RefueledLoop {

    private final float volumeToReach;

    public RefueledIdle(Entity car, SoundEvent event, SoundSource category) {
        super(car, event, category);
        volumeToReach = volume;
        volume = volume / 2.5F;
    }

    @Override
    public void tick() {
        if (volume < volumeToReach) {
            volume = Math.min(volume + volumeToReach / 2.5F, volumeToReach);
        }
        super.tick();
    }

    @Override
    public boolean shouldStopSound() {
        if (((ICarInvoker) car).car$getSpeed() != 0) {
            return true;
        } else if (!((ICarInvoker) car).car$isStarted()) {
            return true;
        }
        return false;
    }
}
