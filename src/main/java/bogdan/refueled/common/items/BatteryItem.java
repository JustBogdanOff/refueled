package bogdan.refueled.common.items;

import bogdan.refueled.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class BatteryItem extends Item {
    public BatteryItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        if(ForgeCapabilities.ENERGY != null) {
            tooltip.add(Component.translatable("tooltip.charge", Component.literal(String.valueOf(stack.getCapability(ForgeCapabilities.ENERGY).resolve().get().getEnergyStored())).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        if(!stack.isEmpty()){
            return new EnergyStorageItem(stack, ServerConfig.batteryMax.get());
        }
        return null;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if(context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()){
            return super.useOn(context);
        }

        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if(context.getItemInHand().getCapability(ForgeCapabilities.ENERGY).isPresent() && (state.getBlock() == Blocks.REDSTONE_ORE || state.getBlock() == Blocks.DEEPSLATE_REDSTONE_ORE)){
            IEnergyStorage energy = context.getItemInHand().getCapability(ForgeCapabilities.ENERGY).resolve().get();

            if(energy.getMaxEnergyStored() - energy.getEnergyStored() > 8100){
                energy.receiveEnergy(8100, false);
                if(!context.getLevel().isClientSide){
                    context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1f, 0.15f);
                    if(state.getBlock() == Blocks.REDSTONE_ORE)
                        context.getLevel().setBlockAndUpdate(context.getClickedPos(), Blocks.STONE.defaultBlockState());
                    else
                        context.getLevel().setBlockAndUpdate(context.getClickedPos(), Blocks.DEEPSLATE.defaultBlockState());
                }

                return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
            }
        }

        return super.useOn(context);
    }

    public static class EnergyStorageItem implements IEnergyStorage, ICapabilityProvider{
        public static final String ENERGY_NBT_KEY = "Energy";
        private final LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> this);

        protected final @NotNull ItemStack container;
        protected final int capacity;

        public EnergyStorageItem(@NotNull ItemStack stack, int energyCapacity){
            this.container = stack;
            this.capacity = energyCapacity;
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if(this.container.getCount() != 1 || amount < 0){
                return 0;
            }

            final int stored = getEnergyStored();
            int received = Math.min(getMaxEnergyStored() - stored, amount);
            if (!simulate) {
                setEnergyStored(stored + received);
            }

            return received;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            if(this.container.getCount() != 1 || amount < 0){
                return 0;
            }

            final int stored = getEnergyStored();
            if(stored == 0){
                return 0;
            }

            int extracted = Math.min(stored, amount);
            if (!simulate) {
                setEnergyStored(stored - extracted);
            }

            return extracted;
        }

        @Override
        public int getEnergyStored() {
            if(this.container.getTag() != null && this.container.getTag().contains(ENERGY_NBT_KEY)){
                return this.container.getTag().getInt(ENERGY_NBT_KEY);
            }

            return 0;
        }

        protected void setEnergyStored(int amount){
            container.getOrCreateTag().putInt(ENERGY_NBT_KEY, amount);
        }

        @Override
        public int getMaxEnergyStored() {
            return this.capacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
            return ForgeCapabilities.ENERGY.orEmpty(capability, lazyEnergy);
        }
    }
}
