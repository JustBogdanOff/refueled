package bogdan.refueled.common.gui.slots;

import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.config.ServerConfig;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BatterySlot extends Slot {
    private final Entity car;
    private final Player player;

    public BatterySlot(Entity car, int index, int xPosition, int yPosition, Player player) {
        super(new SimpleContainer(1), index, xPosition, yPosition);
        this.car = car;
        this.player = player;
    }

    @Override
    public void set(ItemStack stack) {
        if (!(stack.getItem().equals(Items.REDSTONE))) {
            return;
        }

        if(stack.getItem().equals(Items.REDSTONE)) {
            int energy = 81;
            int energyToFill = ((ICarInvoker) car).car$getMaxBattery() - ((ICarInvoker) car).car$getBattery();
            int fill = ((ICarInvoker) car).car$getBattery() + Math.min(energy, energyToFill);

            if (fill > 0) {
                stack.shrink(1);
                ((ICarInvoker) car).car$setBattery(fill);
            }

            if (!player.getInventory().add(stack)) {
                Containers.dropItemStack(car.level(), car.getX(), car.getY(), car.getZ(), stack);
            }
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if(stack.getItem().equals(Items.REDSTONE)){
            return (int) (((float) ((ICarInvoker) car).car$getBattery() / (float) ((ICarInvoker) car).car$getMaxBattery()) * 100f) < 100;
        }
        return false;
    }
}
