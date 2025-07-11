package bogdan.refueled.common.items;

import bogdan.refueled.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
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

public class Canister extends Item {
    public Canister() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        if(ctx.getPlayer() == null) return super.useOn(ctx);

        if(ctx.getPlayer().isShiftKeyDown()){
            ItemStack copy = ctx.getItemInHand().copyWithCount(1);
            LazyOptional<IFluidHandlerItem> lazyHandler = copy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if(lazyHandler.isPresent()){
                IFluidHandlerItem handler = lazyHandler.resolve().get();
                FluidStack stored = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                BlockPos fluidPos = ctx.getClickedPos();
                var state = ctx.getLevel().getBlockState(fluidPos);
                // See if block is water-loggable
                if(!state.getProperties().contains(BlockStateProperties.WATERLOGGED)) {
                    fluidPos = new BlockPos(ctx.getClickedFace().getNormal().offset(fluidPos));
                    state = ctx.getLevel().getBlockState(fluidPos);
                    // Otherwise see if clicked-face-adjacent block is empty or a fluid block
                    if(!(state.isAir() || state.getBlock() instanceof LiquidBlock))
                        return InteractionResult.FAIL;
                }

                Fluid blockFluid = ctx.getLevel().getFluidState(fluidPos).isSource() ? ctx.getLevel().getFluidState(fluidPos).getType() : Fluids.EMPTY;
                // See if we have enough space and the appropriate fluid in our handler to pickup fluid
                if(handler.fill(new FluidStack(blockFluid, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE) >= 1000 && FluidUtil.tryPickUpFluid(copy, ctx.getPlayer(), ctx.getLevel(), fluidPos, ctx.getClickedFace()).success){
                    handler.fill(new FluidStack(blockFluid, 1000), IFluidHandler.FluidAction.EXECUTE);
                    if(ctx.getItemInHand().getCount() > 1) {
                        ctx.getItemInHand().shrink(1);
                        if (!ctx.getPlayer().getInventory().add(copy))
                            ctx.getPlayer().drop(copy, true);
                    }
                    else ctx.getPlayer().setItemInHand(ctx.getHand(), copy);
                    ctx.getLevel().gameEvent(ctx.getPlayer(), GameEvent.FLUID_PICKUP, fluidPos);
                    return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
                }
                // Else try to place fluid
                else if(FluidUtil.tryPlaceFluid(ctx.getPlayer(), ctx.getLevel(), ctx.getHand(), fluidPos, handler, new FluidStack(stored, 1000))){
                    if(ctx.getItemInHand().getCount() > 1) {
                        ctx.getItemInHand().shrink(1);
                        if (!ctx.getPlayer().getInventory().add(copy))
                            ctx.getPlayer().drop(copy, true);
                    }
                    else ctx.getPlayer().setItemInHand(ctx.getHand(), copy);
                    ctx.getLevel().gameEvent(ctx.getPlayer(), GameEvent.FLUID_PLACE, fluidPos);
                    return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
                }
            }
        }
        else{
            BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());
            if (be == null) return super.useOn(ctx);

            if(be.getCapability(ForgeCapabilities.FLUID_HANDLER, ctx.getClickedFace()).isPresent()){
                boolean success = handle(ctx.getItemInHand(), be.getCapability(ForgeCapabilities.FLUID_HANDLER, ctx.getClickedFace()).resolve().get());
                if (success && !ctx.getLevel().isClientSide)
                    ctx.getLevel().playSound(null, ctx.getClickedPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.15f, 1f);
                return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
            }
        }

        return InteractionResult.FAIL;
    }

    public boolean handle(ItemStack stack, IFluidHandler otherHandler){
        LazyOptional<IFluidHandlerItem> lazyHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        if(lazyHandler.isPresent()){
            IFluidHandlerItem handler = lazyHandler.resolve().get();
            FluidStack stored = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE),
                    otherStored = otherHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if(!otherStored.isEmpty()){
                // and if there's any space left in our handler
                if(handler.fill(new FluidStack(stored, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE) > 0){
                    otherHandler.drain(handler.fill(otherStored, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    return true;
                }
                // otherwise assume it's full and drain
                else{
                    handler.drain(otherHandler.fill(stored, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    return true;
                }
            }
            // If the otherHandler is empty and our handler contains anything
            else if(!stored.isEmpty()){
                handler.drain(otherHandler.fill(stored, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }

        return false;
    }

    public int getCapacity() {
        return ServerConfig.canisterMax.get();
    }

    private void addInfo(String fluid, int amount, List<Component> tooltip) {
        tooltip.add(
                Component.translatable(
                        "tooltip.canister.fluid",
                        Component.literal(fluid).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY)
                        .append(amount == 0 ? Component.empty() : Component.translatable("tooltip.canister.amount", Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY))
        );
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        if(ForgeCapabilities.FLUID_HANDLER_ITEM != null) {
            FluidUtil.getFluidContained(stack).ifPresentOrElse(
                    fs -> addInfo(fs.getDisplayName().getString(), fs.getAmount(), tooltip),
                    () -> addInfo("-", 0, tooltip)
            );
            return;
        }
        else addInfo("-", 0, tooltip);

        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public @NotNull ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if(!stack.isEmpty()) return new FluidHandlerItemStack(stack, getCapacity());
        return null;
    }

    private boolean canBlockContainFluid(Level level, BlockPos pos, Fluid fluid) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof LiquidBlockContainer liquidBlock && liquidBlock.canPlaceLiquid(level, pos, state, fluid);
    }
}
