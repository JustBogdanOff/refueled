package bogdan.refueled.common.sounds;

import bogdan.refueled.common.accessors.ICarInvoker;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class RefueledHigh extends RefueledLoop {

    public RefueledHigh(Entity car, SoundEvent event, SoundSource category) {
        super(car, event, category);
    }

    @Override
    public void tick() {
        pitch = ((ICarInvoker) car).car$getPitch();
        super.tick();
    }

    @Override
    public boolean shouldStopSound() {
        if (((ICarInvoker) car).car$getSpeed() == 0F) {
            return true;
        } else if (!((ICarInvoker) car).car$isStarted()) {
            return true;
        }

        return false;
    }
}
