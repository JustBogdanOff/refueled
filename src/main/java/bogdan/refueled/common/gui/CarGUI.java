package bogdan.refueled.common.gui;

import bogdan.refueled.RefueledRegistry;
import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.config.ServerConfig;
import bogdan.refueled.mixin.common.accessor.ILevelAccess;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;

public class CarGUI extends AbstractContainerMenu {

    private final Container inventory, playerInventory;
    public final Entity car;

    public CarGUI(int id, Inventory playerInventory, FriendlyByteBuf extraData){
        this(id, playerInventory, ((ILevelAccess) playerInventory.player.level()).invokeGetEntities().get(extraData.readUUID()));
    }

    public CarGUI(int id, Inventory playerInventory, Entity car) {
        super(RefueledRegistry.CAR_GUI.get(), id);

        this.car = car;
        this.inventory = car == null ? new SimpleContainer(0) : ((IVehicleAccess) car).refuel$getContainer();
        this.playerInventory = playerInventory;
        int numRows = inventory.getContainerSize() / 8;

        for (int  j = 0; j < numRows; j++) {
            for (int k = 0; k < 8; k++) {
                addSlot(new Slot(inventory, 3 + k + j * 8, 26 + k * 18, 66 + j * 18));
            }
        }

        addSlot(new Slot(inventory, 0, 8, 66){
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent() && !(stack.getItem() instanceof BucketItem);
            }
        });
        addSlot(new Slot(inventory, 1, 8, 84){
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem().equals(Items.REDSTONE) || stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
            }
        });
        addSlot(new Slot(inventory, 2, 8, 102){
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return ServerConfig.getRepairItemData(stack) != null;
            }
        });

        addPlayerInventorySlots();
    }

    private void addPlayerInventorySlots() {
        if (playerInventory != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 9; j++) {
                    addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 134 + i * 18));
                }
            }

            for (int k = 0; k < 9; k++) {
                addSlot(new Slot(playerInventory, k, 8 + k * 18, 192));
            }
        }
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
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
            return true;
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