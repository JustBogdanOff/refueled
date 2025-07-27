package bogdan.refueled.common.gui;

import bogdan.refueled.RefueledRegistry;
import bogdan.refueled.mixin.common.accessor.ILevelAccess;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TruckGUI extends AbstractContainerMenu {
    private final Container inventory, playerInventory;
    public final Truck truck;

    public TruckGUI(int id, Inventory playerInventory, FriendlyByteBuf extraData){
        this(id, playerInventory, (Truck) ((ILevelAccess) playerInventory.player.level()).invokeGetEntities().get(extraData.readUUID()));
    }

    public TruckGUI(int id, Inventory playerInventory, Truck truck) {
        super(RefueledRegistry.TRUCK_GUI.get(), id);
        this.truck = truck;
        this.inventory = truck == null ? new SimpleContainer(0) : truck.inventory;
        this.playerInventory = playerInventory;

        for(int i = 0; i < inventory.getContainerSize(); i++)
            addSlot(new Slot(inventory, i, 152, 18)); // Stacks all the slots in the 1st row on the 9th slot

        addPlayerInventorySlots();
    }


    private void addPlayerInventorySlots() {
        if (playerInventory != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 9; j++) {
                    addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
                }
            }

            for (int k = 0; k < 9; k++) {
                addSlot(new Slot(playerInventory, k, 8 + k * 18, 198));
            }
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            if (index < inventory.getContainerSize()) {
                if (!moveItemStackTo(stack, inventory.getContainerSize(), slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, inventory.getContainerSize(), false)) {
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

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (inventory == null) {
            return false;
        }
        return inventory.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (inventory != null) {
            inventory.stopOpen(player);
        }
    }
}
