package bogdan.refueled.common.blocks.blockentities;

import bogdan.refueled.RefueledRegistry;
import bogdan.refueled.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static bogdan.refueled.Utils.readInventory;
import static bogdan.refueled.Utils.saveInventory;

public class GasStationBlockEntity extends BlockEntity{
    public GasStationBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(RefueledRegistry.GAS_STATION_BLOCK_ENTITY.get(), pPos, pBlockState);
        fuelingBox = getFuelingBox(pPos, pBlockState);

        tank = new FluidTank(getFuelCapacity(), fs -> ServerConfig.getFuelEfficiency(fs.getFluid()) > 0){
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        fluidHandler = LazyOptional.of(() -> tank);

        inventory = new SimpleContainer(3);
        itemHandler = LazyOptional.of(() -> new InvWrapper(inventory));
    }

    public final FluidTank tank;
    public final Container inventory;
    private final LazyOptional<IFluidHandler> fluidHandler;
    private final LazyOptional<IItemHandler> itemHandler;

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if((side == null || side == Direction.DOWN || canReachHandler(getBlockState(), side)) && cap == ForgeCapabilities.FLUID_HANDLER)
            return fluidHandler.cast();

        if(cap == ForgeCapabilities.ITEM_HANDLER)
            return itemHandler.cast();

        return super.getCapability(cap, side);
    }

    public int getFuelCapacity(){
        return ServerConfig.gasStationMax.get();
    }

    private boolean canReachHandler(BlockState state, Direction side){
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        if(facing == Direction.NORTH || facing == Direction.SOUTH)
            return side == Direction.WEST || side == Direction.EAST;
        else if(facing == Direction.EAST || facing == Direction.WEST)
            return side == Direction.NORTH || side == Direction.SOUTH;

        return false;
    }

    @Override
    public void load(CompoundTag tag) {
        tank.readFromNBT(tag);
        readInventory(tag, "inventory", inventory);
        super.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tank.writeToNBT(tag);
        saveInventory(tag, "inventory", inventory);
        super.saveAdditional(tag);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(){
        CompoundTag tag = new CompoundTag();
        tank.writeToNBT(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        tank.readFromNBT(tag);
        setChanged();
    }

    @Override
    public void setChanged() {
        if(level != null){
            setChanged(level, getBlockPos(), getBlockState());
            if(!level.isClientSide)
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    }

    public final AABB fuelingBox;

    public static AABB getFuelingBox(BlockPos pos, BlockState state){
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        if(facing == Direction.NORTH || facing == Direction.SOUTH)
            return NorthSouthBox.move(pos.getCenter());
        if(facing == Direction.EAST || facing == Direction.WEST)
            return EastWestBox.move(pos.getCenter());

        return new AABB(0, -0.5, 0, 0, 2, 0).move(pos.getCenter());
    }

    public Entity selected;
    private static final AABB
            NorthSouthBox = new AABB(-1.5, -0.5, -3.5, 1.5, 2, 3.5),
            EastWestBox = new AABB(-3.5, -0.5, -1.5, 3.5, 2, 1.5);

    public boolean fueling = false, fuelingItem = false, drainingItem = false;

    public void fuelVehicle(){
        if(selected != null) getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handler -> {
            selected.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(
                    entityHandler -> {

                        FluidStack fuel = new FluidStack(tank.getFluid(), ServerConfig.gasStationTransferMax.get());
                        if(!tank.isEmpty() && entityHandler.fill(fuel, IFluidHandler.FluidAction.SIMULATE) > 0) {
                            entityHandler.fill(handler.drain(ServerConfig.gasStationTransferMax.get(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                            fueling = true;
                        }
                        else fueling = false;
                    }
            );
        });
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        final int maxTransfer = ServerConfig.gasStationTransferMax.get();
        GasStationBlockEntity gasStation = (GasStationBlockEntity) blockEntity;
        LazyOptional<IFluidHandler> uncastHandler = gasStation.getCapability(ForgeCapabilities.FLUID_HANDLER);
        Container inv = gasStation.inventory;
        uncastHandler.ifPresent(handler -> {
            FluidStack ourStored = handler.drain(maxTransfer, IFluidHandler.FluidAction.SIMULATE);

            if(gasStation.fueling){
                gasStation.fuelVehicle();
            }

            if(!inv.getItem(1).isEmpty()) {
                ItemStack copy = inv.getItem(1).copy();
                if (copy.getCount() > 1) copy.setCount(1);

                copy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(
                        itemHandler -> {
                            FluidStack itemStored = itemHandler.drain(maxTransfer, IFluidHandler.FluidAction.SIMULATE);

                            if (gasStation.fuelingItem) {
                                gasStation.drainingItem = false;

                                if (itemHandler.fill(ourStored, IFluidHandler.FluidAction.SIMULATE) > 0) {
                                    var filled = itemHandler.fill(ourStored, IFluidHandler.FluidAction.EXECUTE);
                                    handler.drain(filled, IFluidHandler.FluidAction.EXECUTE);

                                    if (filled < maxTransfer || handler.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty())
                                        gasStation.fuelingItem = false;

                                    if (!gasStation.fuelingItem)
                                        tryToOutput(copy, gasStation);
                                    else inv.setItem(1, copy);

                                    gasStation.setChanged();
                                } else {
                                    gasStation.fuelingItem = false;
                                    tryToOutput(copy, gasStation);
                                    gasStation.setChanged();
                                }
                            } else if (gasStation.drainingItem) {
                                if (handler.fill(itemStored, IFluidHandler.FluidAction.SIMULATE) > 0) {
                                    var filled = handler.fill(itemStored, IFluidHandler.FluidAction.EXECUTE);
                                    itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);

                                    if (filled < maxTransfer || itemHandler.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty())
                                        gasStation.drainingItem = false;

                                    if (!gasStation.drainingItem)
                                        tryToOutput(copy, gasStation);
                                    else inv.setItem(1, copy);

                                    gasStation.setChanged();
                                } else {
                                    gasStation.drainingItem = false;
                                    tryToOutput(copy, gasStation);
                                    gasStation.setChanged();
                                }
                            } else {
                                var output = inv.getItem(2);
                                if(output.isEmpty() || output.copyWithCount(1).equals(copy, true)){
                                    inv.setItem(1, ItemStack.EMPTY);

                                    if(output.isEmpty()) inv.setItem(2, copy);
                                    else output.grow(1);

                                    gasStation.setChanged();
                                }
                            }
                        });
            }
            else if(!inv.getItem(0).isEmpty()){
                ItemStack original = inv.getItem(0), copy = original.copy();
                if(copy.getCount() > 1) copy.setCount(1);

                copy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(
                        itemHandler -> {
                            FluidStack itemStored = itemHandler.drain(maxTransfer, IFluidHandler.FluidAction.SIMULATE);

                            if(!itemStored.isEmpty()){
                                if(handler.fill(itemStored, IFluidHandler.FluidAction.SIMULATE) > 0){
                                    var filled = handler.fill(itemStored, IFluidHandler.FluidAction.EXECUTE);
                                    itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);

                                    if(original.getCount() > 1) original.shrink(1);
                                    else inv.setItem(0, ItemStack.EMPTY);

                                    if(filled == maxTransfer){
                                        gasStation.drainingItem = true;
                                        inv.setItem(1, copy);
                                    }
                                    else
                                        inv.setItem(2, copy);

                                    gasStation.setChanged();

                                }
                            } else if(!ourStored.isEmpty()){
                                if(itemHandler.fill(ourStored, IFluidHandler.FluidAction.SIMULATE) > 0){
                                    var filled = itemHandler.fill(ourStored, IFluidHandler.FluidAction.EXECUTE);
                                    handler.drain(filled, IFluidHandler.FluidAction.EXECUTE);

                                    if(original.getCount() > 1) original.shrink(1);
                                    else inv.setItem(0, ItemStack.EMPTY);

                                    if(filled == maxTransfer){
                                        gasStation.fuelingItem = true;
                                        inv.setItem(1, copy);
                                    }
                                    else
                                        inv.setItem(2, copy);

                                    gasStation.setChanged();

                                }
                            }
                        });
            }
            else {
                gasStation.fuelingItem = false;
                gasStation.drainingItem = false;
            }
        });
    }

    public Component getFluidText() {
        if(tank.isEmpty()) return Component.translatable("block.refueled.gas_station.empty");
        else return Component.translatable(tank.getFluid().getFluid().getFluidType().getDescriptionId());
    }

    public Component getAmountText(){
        if(tank.isEmpty()) return Component.literal("- mB");
        else return Component.translatable("tooltip.canister.amount", tank.getFluid().getAmount());
    }

    public static void tryToOutput(ItemStack copy, GasStationBlockEntity gasStation){
        var output = gasStation.inventory.getItem(2);

        if(output.isEmpty()){
            gasStation.inventory.setItem(1, ItemStack.EMPTY);
            gasStation.inventory.setItem(2, copy);
        }
        else if(output.copyWithCount(1).equals(copy, true)){
            gasStation.inventory.setItem(1, ItemStack.EMPTY);
            output.grow(1);
        }
        else gasStation.inventory.setItem(1, copy);

        gasStation.setChanged();
    }
}
