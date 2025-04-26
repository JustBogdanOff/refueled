package bogdan.refueled.common.items;

import bogdan.refueled.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class Canister extends Item{
    public Canister() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return super.useOn(context);
        }
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        LazyOptional<IFluidHandlerItem> lazyHandler = FluidUtil.getFluidHandler(context.getItemInHand());

        if(!context.getLevel().isClientSide && context.getClickedFace() == Direction.UP && context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof AbstractCauldronBlock cauldron){
            lazyHandler.ifPresent(handler -> {
                FluidStack fluid = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);

                if(getCapacity() - fluid.getAmount() >= 1000){
                        if(cauldron instanceof LayeredCauldronBlock && (fluid.getFluid() == Fluids.WATER || fluid.isEmpty())){
                            int waterLevel = context.getLevel().getBlockState(context.getClickedPos()).getValue(BlockStateProperties.LEVEL_CAULDRON);
                            if(waterLevel == 3){
                                handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                            }
                            else{
                                handler.fill(new FluidStack(Fluids.WATER, 333 * waterLevel), IFluidHandler.FluidAction.EXECUTE);
                            }
                            drainCauldron(context, false);
                            return;
                        }
                        if(cauldron instanceof LavaCauldronBlock && (fluid.getFluid() == Fluids.LAVA || fluid.isEmpty())){
                            handler.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
                            drainCauldron(context, true);
                            return;
                        }
                    }

                    if(fluid.getAmount() >= 1000){
                        if(fluid.getFluid() == Fluids.WATER){
                            int waterLevel = fluid.getAmount() / 333 >= 3 ? 1000 : (fluid.getAmount() / 333) * 333;
                            handler.drain(waterLevel, IFluidHandler.FluidAction.EXECUTE);
                            fillCauldron(context, false);
                            return;
                        }
                        if(fluid.getFluid() == Fluids.LAVA){
                            handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                            fillCauldron(context, true);
                        }
                    }
            });
            return InteractionResult.SUCCESS;
        }

        if (blockEntity == null)
            return super.useOn(context);

        if(blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, context.getClickedFace()).isPresent()){
            boolean success = handle(context.getItemInHand(), blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, context.getClickedFace()).resolve().get());
            if (success && !context.getLevel().isClientSide)
                context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.15f, 1f);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        if(ForgeCapabilities.FLUID_HANDLER_ITEM != null) {
            Optional<FluidStack> fsCap = FluidUtil.getFluidContained(stack);
            fsCap.ifPresentOrElse(
                    fs -> addInfo(fs.getDisplayName().getString(), fs.getAmount(), tooltip),
                    () -> addInfo("-", 0, tooltip)
            );
            return;
        }
        else addInfo("-", 0, tooltip);

        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    private void addInfo(String fluid, int amount, List<Component> tooltip) {
        tooltip.add(
                Component.translatable(
                        "tooltip.canister.fluid",
                        Component.literal(fluid).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY)
                        .append(amount == 0 ? Component.empty() : Component.translatable("tooltip.canister.amount", Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY))
        );
    }

    public boolean handle(ItemStack stack, IFluidHandler otherHandler){
        LazyOptional<IFluidHandlerItem> lazyHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        if(lazyHandler.isPresent()){
            IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().get();
            FluidStack fluidStack = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE),
                    otherFluidStack = otherHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if(!otherFluidStack.isEmpty()){
                // and if there's any space left in our handler
                if(handler.fill(new FluidStack(fluidStack, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE) > 0){
                    otherHandler.drain(handler.fill(otherFluidStack, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    return true;
                }
                // otherwise assume it's full and drain
                else{
                    handler.drain(otherHandler.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    return true;
                }
            }
            // If the otherHandler is empty and our handler contains anything
            else if(!fluidStack.isEmpty()){
                handler.drain(otherHandler.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }

        return false;
    }

    public int getCapacity() {
        return ServerConfig.canisterMax.get();
    }

    private void drainCauldron(UseOnContext context, boolean isLava){
        if(context.getPlayer() != null) context.getPlayer().awardStat(Stats.USE_CAULDRON);
        context.getLevel().setBlockAndUpdate(context.getClickedPos(), Blocks.CAULDRON.defaultBlockState());
        context.getLevel().playSound(null, context.getClickedPos(), isLava ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        context.getLevel().gameEvent(null, GameEvent.FLUID_PICKUP, context.getClickedPos());
    }

    private void fillCauldron(UseOnContext context, boolean isLava){
        if(context.getPlayer() != null) context.getPlayer().awardStat(Stats.FILL_CAULDRON);
        context.getLevel().setBlockAndUpdate(context.getClickedPos(), isLava ? Blocks.LAVA_CAULDRON.defaultBlockState() : Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
        context.getLevel().playSound(null, context.getClickedPos(), isLava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        context.getLevel().gameEvent(null, GameEvent.FLUID_PLACE, context.getClickedPos());
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if(!stack.isEmpty()) return new FluidHandlerItemStack(stack, getCapacity());
        return null;
    }

}
