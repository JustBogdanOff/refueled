package bogdan.refueled.common.gui;

import bogdan.refueled.RefueledRegistry;
import bogdan.refueled.common.gui.slots.BatterySlot;
import bogdan.refueled.common.gui.slots.FuelSlot;
import bogdan.refueled.common.gui.slots.RepairSlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CarGUI extends AbstractContainerMenu {

    private final Container car, playerInventory;
    private int getInvOffset(){
        return 82;
    }

    public CarGUI(int id, Inventory playerInventory, Container car) {
        super(RefueledRegistry.CAR_GUI.get(), id);
        this.playerInventory = playerInventory;
        this.car = car;
        int numRows = car.getContainerSize() / 9;

        for (int j = 0; j < numRows; j++) {
            for (int k = 0; k < 9; k++) {
                addSlot(new Slot(car, k + j * 9, 8 + k * 18, 98 + j * 18));
            }
        }

        addSlot(new FuelSlot((Entity) car, 0, 98, 66, playerInventory.player));
        addSlot(new BatterySlot((Entity) car, 0, 116, 66, playerInventory.player));
        addSlot(new RepairSlot((Entity) car, 0, 134, 66, playerInventory.player));

        addPlayerInventorySlots();
    }

    private void addPlayerInventorySlots() {
        if (playerInventory != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 9; j++) {
                    addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + getInvOffset()));
                }
            }

            for (int k = 0; k < 9; k++) {
                addSlot(new Slot(playerInventory, k, 8 + k * 18, 142 + getInvOffset()));
            }
        }
    }

    public int getInventorySize() {
        if (car == null) {
            return 0;
        }
        return car.getContainerSize();
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            if (index < getInventorySize()) {
                if (!moveItemStackTo(stack, getInventorySize(), slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, getInventorySize(), false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    public Entity getCar(){
        return (Entity) car;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (car == null) {
            return true;
        }
        return car.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (car != null) {
            car.stopOpen(player);
        }
    }
}