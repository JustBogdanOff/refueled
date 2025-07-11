package bogdan.refueled.client.accessors;

import bogdan.refueled.common.accessors.IVehicleAccess;
import com.dragn0007.dragnvehicles.Animation;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public interface IVehicleAccessClient extends IVehicleAccess {
    void refuel$playSoundLoop(AbstractTickableSoundInstance loop, Level level);
    float refuel$getBatterySoundPitchLevel();

    // -> WHEELS
    void refuel$animate(ModelPart modelPart, Animation animation, float wheelRotation);
    float refuel$getWheelRotation(float partialTick);
    // -> SLOPING
    void refuel$offsetCamera(float partialTick, Camera camera);
    float refuel$getAngle(float yawDeg);
    float refuel$getVerticalOffset(float partialTick);
    Vec3 refuel$getPlayerOffsets(float partialTick, Player player);
    Quaternionf refuel$getPlayerRotation(float partialTick);
    Quaternionf refuel$getRotation(float partialTick);
}
