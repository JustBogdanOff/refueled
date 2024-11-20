package bogdan.refueled.common.sounds;

import bogdan.refueled.config.ClientConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RefueledLoop extends AbstractTickableSoundInstance {

    protected Entity car;

    public RefueledLoop(Entity car, SoundEvent event, SoundSource category) {
        super(event, category, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = ClientConfig.carVolume.get().floatValue();
        this.pitch = 1F;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        this.updatePos();
    }

    public void updatePos() {
        this.x = (float) car.getX();
        this.y = (float) car.getY();
        this.z = (float) car.getZ();
    }

    @Override
    public void tick() {
        if (isStopped()) {
            return;
        }

        if (!car.isAlive()) {
            setDonePlaying();
            return;
        }

        LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || !player.isAlive()) {
            setDonePlaying();
            return;
        }

        if (shouldStopSound()) {
            setDonePlaying();
            return;
        }

        updatePos();
    }

    public void setDonePlaying() {
        stop();
    }

    public abstract boolean shouldStopSound();
}
