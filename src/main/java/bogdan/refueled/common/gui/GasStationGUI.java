package bogdan.refueled.common.gui;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.RefueledRegistry;
import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.common.blocks.blockentities.GasStationBlockEntity;
import bogdan.refueled.config.ServerConfig;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class GasStationGUI extends AbstractContainerMenu {

    private final Container playerInventory;
    public final GasStationBlockEntity gasStation;
    public final DataSlot fuelButton;
    public final int[] entitySlots;
    public final List<Integer> foundEntities = new ArrayList<>();

    public GasStationGUI(int id, Inventory playerInventory, FriendlyByteBuf extraData){
        this(id, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos(), RefueledRegistry.GAS_STATION_BLOCK_ENTITY.get()).get(), extraData.readVarIntArray(4));
    }

    public GasStationGUI(int id, Inventory playerInventory, GasStationBlockEntity gasStation, int[] foundEntities) {
        super(RefueledRegistry.GAS_STATION_GUI.get(), id);
        this.playerInventory = playerInventory;
        this.gasStation = gasStation;
        Arrays.stream(foundEntities).filter(entityId -> entityId != -1).forEach(this.foundEntities::add);

        this.fuelButton = DataSlot.standalone();
        this.entitySlots = new int[4];

        addSlot(new Slot(gasStation.inventory, 0, 152, 22){
            @Override
            public boolean mayPlace(ItemStack pStack) {
                if(pStack.getItem() instanceof BucketItem) return false;

                if (pStack.copyWithCount(1).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()){
                    if(gasStation.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent()){
                        IFluidHandlerItem itemHandler = pStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().get();
                        IFluidHandler handler = gasStation.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().get();
                        FluidStack ourStored = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE),
                                itemStored = itemHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);

                        if(!ourStored.isEmpty())
                            return itemStored.isEmpty() || ourStored.isFluidEqual(itemStored);
                        else
                            return ServerConfig.getFuelEfficiency(itemStored.getFluid()) > 0;
                    }
                }
                return false;
            }
        });
        addSlot(new Slot(gasStation.inventory, 1, 152, 40){
            @Override
            public boolean mayPlace(ItemStack pStack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player pPlayer) {
                return false;
            }

            @Override
            public boolean isHighlightable() {
                return false;
            }
        });
        addSlot(new Slot(gasStation.inventory, 2, 152, 58){
            @Override
            public boolean mayPlace(ItemStack pStack) {
                return false;
            }
        });

        addDataSlot(fuelButton);
        addDataSlot(DataSlot.shared(entitySlots, 0));
        addDataSlot(DataSlot.shared(entitySlots, 1));
        addDataSlot(DataSlot.shared(entitySlots, 2));
        addDataSlot(DataSlot.shared(entitySlots, 3));


        addPlayerInventorySlots();
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if(id == 0){
            if(gasStation.selected == null)
                return false;
            gasStation.fuelVehicle();
            return true;
        }

        if(id >= 1 && id <= entitySlots.length){
            gasStation.selected = player.level().getEntity(foundEntities.get(--id));
            return true;
        }
        return false;
    }

    private void addPlayerInventorySlots() {
        if (playerInventory != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 9; j++) {
                    addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 94 + i * 18));
                }
            }

            for (int k = 0; k < 9; k++) {
                addSlot(new Slot(playerInventory, k, 8 + k * 18, 152));
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

            if (index < gasStation.inventory.getContainerSize()) {
                if (!moveItemStackTo(stack, gasStation.inventory.getContainerSize(), slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, gasStation.inventory.getContainerSize(), false)) {
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
        return !gasStation.isRemoved() && player.isAlive() && stillValid(ContainerLevelAccess.create(player.level(), gasStation.getBlockPos()), player, gasStation.getBlockState().getBlock());
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        gasStation.inventory.stopOpen(player);
    }
}