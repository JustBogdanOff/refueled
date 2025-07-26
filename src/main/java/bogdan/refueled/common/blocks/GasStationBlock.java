package bogdan.refueled.common.blocks;

import bogdan.refueled.RefueledRegistry;
import bogdan.refueled.common.blocks.blockentities.GasStationBlockEntity;
import bogdan.refueled.common.gui.GasStationGUI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static bogdan.refueled.Utils.getGasStationEntities;

public class GasStationBlock extends Block implements EntityBlock {
    public static final BooleanProperty GAS_STATION_BOTTOM = BooleanProperty.create("gas_station_bottom");

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(GAS_STATION_BOTTOM, BlockStateProperties.HORIZONTAL_FACING);
    }

    public GasStationBlock() {
        super(Properties.of().sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.IGNORE).mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2, 10).requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(GAS_STATION_BOTTOM, true));
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState pState, @NotNull Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if(pLevel.isClientSide)
            return InteractionResult.CONSUME;
        else {
            if(!pPlayer.isShiftKeyDown()){
                var pos = pState.getValue(GAS_STATION_BOTTOM) ? pPos : pPos.below();
                NetworkHooks.openScreen((ServerPlayer) pPlayer, getMenuProvider(pState, pLevel, pos), buf -> {
                    var lambdaPos = pState.getValue(GAS_STATION_BOTTOM) ? pPos : pPos.below();
                    buf.writeBlockPos(lambdaPos);
                    buf.writeVarIntArray(getGasStationEntities(pLevel, lambdaPos));
                });
            }
            return InteractionResult.SUCCESS;
        }
    }


    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockPos clickedPos = pContext.getClickedPos();
        BlockPos adjustedPos = clickedPos.above();
        Level level = pContext.getLevel();
        return level.getBlockState(adjustedPos).canBeReplaced(pContext) && level.getWorldBorder().isWithinBounds(adjustedPos) ? defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, pContext.getHorizontalDirection()) : null;
    }

    @Override
    public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        if (!pLevel.isClientSide){
            if(pPlayer.isCreative()) {
                boolean bottom = pState.getValue(GAS_STATION_BOTTOM);
                if (bottom) {
                    BlockPos adjusted = pPos.above();
                    BlockState above = pLevel.getBlockState(pPos.above());
                    if (above.is(this) && !above.getValue(GAS_STATION_BOTTOM)) {
                        pLevel.setBlock(adjusted, Blocks.AIR.defaultBlockState(), 35);
                        pLevel.levelEvent(pPlayer, 2001, adjusted, Block.getId(above));
                    }
                }
            }

            var pos = pPos;
            if(!pState.getValue(GAS_STATION_BOTTOM))
                pos = pos.below();
            pLevel.getBlockEntity(pos, RefueledRegistry.GAS_STATION_BLOCK_ENTITY.get()).ifPresent(gasStation -> {
                var lambdaPos = pPos;
                if(!pState.getValue(GAS_STATION_BOTTOM))
                    lambdaPos = lambdaPos.below();
                Containers.dropContents(pLevel, lambdaPos, gasStation.inventory);
            });
        }
        super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        if (!pLevel.isClientSide) {
            BlockPos above = pPos.above();
            pLevel.setBlock(above, pState.setValue(GAS_STATION_BOTTOM, false), 3);
            pLevel.blockUpdated(pPos, Blocks.AIR);
            pState.updateNeighbourShapes(pLevel, pPos, 3);
        }
    }

    @Override
    public @NotNull BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (pFacing == (pState.getValue(GAS_STATION_BOTTOM) ? Direction.UP : Direction.DOWN))
            return pFacingState.is(this) && (pFacingState.getValue(GAS_STATION_BOTTOM) != pState.getValue(GAS_STATION_BOTTOM)) ? pState : Blocks.AIR.defaultBlockState();
        else
            return pState;
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return blockState.getValue(GAS_STATION_BOTTOM) ? new GasStationBlockEntity(blockPos, blockState) : null;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        var facing = pState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        boolean bottom = pState.getValue(GAS_STATION_BOTTOM);

        if (facing == Direction.NORTH || facing == Direction.SOUTH)
            return bottom ? NorthSouth : NorthSouth.move(0, -1, 0);
        if (facing == Direction.WEST || facing == Direction.EAST)
            return bottom ? EastWest : EastWest.move(0, -1, 0);

        return Shapes.block();
    }

    private static final VoxelShape
            NorthSouth = Block.box(0, 0, 3, 16, 32, 13),
            EastWest = Block.box(3, 0, 0, 13, 32, 16);

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pBlockEntityType == RefueledRegistry.GAS_STATION_BLOCK_ENTITY.get() ? GasStationBlockEntity::tick : null;
    }

    @Override
    public @Nullable MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return pState.getBlock().getName();
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                return new GasStationGUI(i, playerInventory, pLevel.getBlockEntity(pPos, RefueledRegistry.GAS_STATION_BLOCK_ENTITY.get()).get(), getGasStationEntities(pLevel, pPos));
            }
        };
    }
}

