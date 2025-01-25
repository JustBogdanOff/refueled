package bogdan.refueled.common.gui.slots;

import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RepairSlot extends Slot {
    private final Entity car;
    private final Player player;

    public RepairSlot(Entity car, int index, int xPosition, int yPosition, Player player) {
        super(new SimpleContainer(1), index, xPosition, yPosition);
        this.car = car;
        this.player = player;
    }

    @Override
    public void set(ItemStack stack) {
        if (!(stack.getItem().equals(Items.IRON_INGOT))) {
            return;
        }

        if(stack.getItem().equals(Items.IRON_INGOT)) {
            if ((int) (((ICarInvoker) car).car$getHealth() / ((ICarInvoker) car).car$getMaxHealth()) <= 20) {

                stack.shrink(1);

                float health = ((ICarInvoker) car).car$getHealth() + ServerConfig.repairKitAmount.get().floatValue();
                if (0 <= health && health <= ((ICarInvoker) car).car$getMaxHealth()) {
                    ((ICarInvoker) car).car$setHealth(health);
                    if(!car.level().isClientSide) {
                        car.level().playSound(null, car.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1f, 0.75f);
                    }
                }
            }

            if (!player.getInventory().add(stack)) {
                Containers.dropItemStack(car.level(), car.getX(), car.getY(), car.getZ(), stack);
            }
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if(stack.getItem().equals(Items.IRON_INGOT)) {
            return (int) ((((ICarInvoker) car).car$getHealth() / ((ICarInvoker) car).car$getMaxHealth()) * 100f) <= 20;
        }
        return false;
    }
}
