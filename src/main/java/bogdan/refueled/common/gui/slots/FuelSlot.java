package bogdan.refueled.common.gui.slots;

import bogdan.refueled.common.sounds.RefueledSounds;
import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.config.ServerConfig;
import de.maxhenkel.car.fluids.ModFluids;
import de.maxhenkel.car.items.ItemCanister;
import de.maxhenkel.car.items.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
        if (!(stack.getItem().equals(Items.COAL_BLOCK) && ServerConfig.useSubstitutes.get()) && !stack.getItem().equals(ModItems.CANISTER.get())) {
            return;
        }

        if(stack.getItem().equals(ModItems.CANISTER.get())) {
            boolean success = ItemCanister.fuelFluidHandler(stack, (IFluidHandler) car);

            if (success) {
                RefueledSounds.playSound(SoundEvents.BREWING_STAND_BREW, car.level(), car.blockPosition(), null, SoundSource.MASTER);
            }
        }
        else{
            Fluid fuelType = ((ICarInvoker) car).car$getFluid() == Fluids.EMPTY || ((ICarInvoker) car).car$getFluid() == null ? ModFluids.BIO_DIESEL.get() : ((ICarInvoker) car).car$getFluid();
            int amountToFill = Math.min(((ICarInvoker) car).car$getMaxFuel() - ((ICarInvoker) car).car$getFuel(), 243);

            if (amountToFill > 0) {
                stack.shrink(1);
                IFluidHandler handler = (IFluidHandler) car;
                handler.fill(new FluidStack(fuelType, amountToFill), IFluidHandler.FluidAction.EXECUTE);
                RefueledSounds.playSound(SoundEvents.BREWING_STAND_BREW, car.level(), car.blockPosition(), null, SoundSource.MASTER);
            }
        }

        if (!player.getInventory().add(stack)) {
            Containers.dropItemStack(car.level(), car.getX(), car.getY(), car.getZ(), stack);
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if(stack.getItem().equals(Items.COAL_BLOCK) && ServerConfig.useSubstitutes.get()){
            return ((ICarInvoker) car).car$getFuel() < ((ICarInvoker) car).car$getMaxFuel();
        }

        if(stack.getItem().equals(ModItems.CANISTER.get())){
            if(!stack.hasTag()){
                return false;
            }

            if(!stack.getTag().contains("fuel")){
                return false;
            }
            FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(stack.getTag().getCompound("fuel"));
            if(fluidStack == null || fluidStack.isEmpty()){
                return false;
            }

            return ((ICarInvoker) car).car$getFuel() < ((ICarInvoker) car).car$getMaxFuel() && fluidStack.getAmount() <= 0;
        }

        return false;
    }
}
