package bogdan.refueled.common.gui.slots;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.config.ServerConfig;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FuelSlot extends Slot {
    private final Entity car;
    private final Player player;

    public FuelSlot(Entity car, int index, int xPosition, int yPosition, Player player) {
        super(new SimpleContainer(1), index, xPosition, yPosition);
        this.car = car;
        this.player = player;
    }

    @Override
    public void set(ItemStack stack) {
        if (!(stack.getItem().equals(Items.LAVA_BUCKET))) {
            return;
        }

        Fluid fuelType = ((ICarInvoker) car).car$getFluid() == null ? Fluids.LAVA.getSource() : ((ICarInvoker) car).car$getFluid();
        int amountToFill = Math.min(((ICarInvoker) car).car$getMaxFuel() - ((ICarInvoker) car).car$getFuel(), 800);

        if (amountToFill > 0) {
            stack.shrink(1);
            IFluidHandler handler = (IFluidHandler) car;
            handler.fill(new FluidStack(fuelType, amountToFill), IFluidHandler.FluidAction.EXECUTE);
            if (!car.level().isClientSide) {
                car.level().playSound(null, car.getX() + 0.5D, car.getY() + 0.5D, car.getZ() + 0.5D, SoundEvents.BREWING_STAND_BREW, SoundSource.MASTER, 0.15f, 1f);
            }
        }

        if (!player.getInventory().add(stack)) {
            Containers.dropItemStack(car.level(), car.getX(), car.getY(), car.getZ(), stack);
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if(stack.getItem().equals(Items.LAVA_BUCKET)){
            return ((ICarInvoker) car).car$getFuel() < ((ICarInvoker) car).car$getMaxFuel();
        }
        return false;
    }
}
