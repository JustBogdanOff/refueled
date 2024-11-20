package bogdan.refueled.common.gui.slots;

import bogdan.refueled.common.sounds.RefueledSounds;
import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.config.ServerConfig;
import de.maxhenkel.car.items.ModItems;
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
        if (!(stack.getItem().equals(Items.IRON_BLOCK) && ServerConfig.useSubstitutes.get()) && !stack.getItem().equals(ModItems.REPAIR_KIT.get())) {
            return;
        }

        if(stack.getItem().equals(Items.IRON_BLOCK)) {
            if ((int) (((ICarInvoker) car).car$getHealth() / ((ICarInvoker) car).car$getMaxHealth()) <= 5) {

                stack.shrink(1);

                float health = ((ICarInvoker) car).car$getHealth() + 0.4f * ServerConfig.repairKitAmount.get().floatValue();
                if (0 <= health && health <= ((ICarInvoker) car).car$getMaxHealth()) {
                    ((ICarInvoker) car).car$setHealth(health);
                }
                RefueledSounds.playSound(RefueledSounds.RATCHET.get(), car.level(), car.blockPosition(), null, SoundSource.BLOCKS);
            }

            if (!player.getInventory().add(stack)) {
                Containers.dropItemStack(car.level(), car.getX(), car.getY(), car.getZ(), stack);
            }
        }
        if(stack.getItem().equals(ModItems.REPAIR_KIT.get())){
            if ((int) (((ICarInvoker) car).car$getHealth() / ((ICarInvoker) car).car$getMaxHealth()) <= 5) {

                stack.shrink(1);

                float health = ((ICarInvoker) car).car$getHealth() + ServerConfig.repairKitAmount.get().floatValue();
                if (0 <= health && health <= ((ICarInvoker) car).car$getMaxHealth()) {
                    ((ICarInvoker) car).car$setHealth(health);
                }
                RefueledSounds.playSound(RefueledSounds.RATCHET.get(), car.level(), car.blockPosition(), null, SoundSource.BLOCKS);
            }

            if (!player.getInventory().add(stack)) {
                Containers.dropItemStack(car.level(), car.getX(), car.getY(), car.getZ(), stack);
            }
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if((stack.getItem().equals(Items.IRON_BLOCK) && ServerConfig.useSubstitutes.get()) || stack.getItem().equals(ModItems.REPAIR_KIT.get())) {
            return (int) ((((ICarInvoker) car).car$getHealth() / ((ICarInvoker) car).car$getMaxHealth()) * 100f) <= 5;
        }
        return false;
    }
}
