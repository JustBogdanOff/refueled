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

public class CanisterItem extends Item {
    public CanisterItem() {
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

                BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());
                if(be != null && be.getCapability(ForgeCapabilities.FLUID_HANDLER, ctx.getClickedFace()).isPresent()){
                    IFluidHandler blockHandler = be.getCapability(ForgeCapabilities.FLUID_HANDLER, ctx.getClickedFace()).resolve().get();
                    var success = handle(handler, blockHandler);
                    if (success){
                        if(!ctx.getLevel().isClientSide) {
                            ctx.getLevel().playSound(null, ctx.getClickedPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.15f, 1f);
                        }
                        if(ctx.getItemInHand().getCount() > 1) {
                            ctx.getItemInHand().shrink(1);
                            if (!ctx.getPlayer().getInventory().add(copy))
                                ctx.getPlayer().drop(copy, true);
                        }
                        else ctx.getPlayer().setItemInHand(ctx.getHand(), copy);
                        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
                    }
                }

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

        return InteractionResult.FAIL;
    }

    public boolean handle(IFluidHandlerItem handler, IFluidHandler otherHandler){
        FluidStack stored = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE),
                otherStored = otherHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (!otherStored.isEmpty()) {
            // if we can accept their fluid
            if (handler.fill(otherStored, IFluidHandler.FluidAction.SIMULATE) > 0 && otherHandler.drain(handler.fill(otherStored, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE).getAmount() > 0) {
                var ourFilled = handler.fill(otherStored, IFluidHandler.FluidAction.EXECUTE);
                otherHandler.drain(ourFilled, IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
            // otherwise assume it's full and drain
            else if (otherHandler.fill(stored, IFluidHandler.FluidAction.SIMULATE) > 0 && handler.drain(otherHandler.fill(stored, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE).getAmount() > 0) {
                var otherFilled = otherHandler.fill(stored, IFluidHandler.FluidAction.EXECUTE);
                handler.drain(otherFilled, IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }
        // If the otherHandler is empty and our handler contains anything
        else if (!stored.isEmpty() && otherHandler.fill(stored, IFluidHandler.FluidAction.SIMULATE) > 0 && handler.drain(otherHandler.fill(stored, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE).getAmount() > 0) {
            var otherFilled = otherHandler.fill(stored, IFluidHandler.FluidAction.EXECUTE);
            handler.drain(otherFilled, IFluidHandler.FluidAction.EXECUTE);
            return true;
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
        if(ForgeCapabilities.FLUID_HANDLER_ITEM.isRegistered()) {
            stack.copyWithCount(1).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(
                    handler -> {
                        var fs = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                        if(fs.isEmpty())
                            addInfo("-", 0, tooltip);
                        else
                            addInfo(fs.getDisplayName().getString(), fs.getAmount(), tooltip);
                    }
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
}
