package com.site21.bittermelon.common.content.blocks.barrel;

import com.site21.bittermelon.common.content.items.substance.FluidContainerItem;
import com.site21.bittermelon.common.systems.substance.SubstanceStack;
import com.site21.bittermelon.init.neoforge.BitterDataComponents;
import com.site21.bittermelon.util.SubstanceUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DrumBlock extends Block implements Fallable, EntityBlock {
    private static final double INSET = 0.001;
    private static final VoxelShape COLLISION = Shapes.box(INSET, 0, INSET, 1 - INSET, 1, 1 - INSET);

    private static final int TICK_DELAY = 2;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public DrumBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new DrumBlockEntity(worldPosition, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, state, blockEntity) -> {
            if (blockEntity instanceof DrumBlockEntity barrel) {
                barrel.tick(lvl, pos, state);
            }
        };
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide()) return;
        Vec3 offset = entity.position().subtract(Vec3.atCenterOf(pos));
        if (offset.x * offset.x + offset.z * offset.z < 1.0E-4) return;

        Direction pushDirection = Direction.getApproximateNearest(offset.x, 0, offset.z).getOpposite();
        Direction facing = state.getValue(FACING);
        if (pushDirection.getAxis() == facing.getAxis()) return;

        if (level.getBlockEntity(pos) instanceof DrumBlockEntity barrel) {
            if (facing == Direction.UP) {
                if (level.getRandom().nextFloat() < 0.2f + (1 - barrel.getMixture().getVolume() / 1000f)) {
                    level.setBlockAndUpdate(pos, state.setValue(FACING, pushDirection));
                    playFlipSound(level, pos, state);
                }
                return;
            }

            BlockPos newPos = pos.relative(pushDirection);
            if (!isFree(level.getBlockState(newPos))) return;

            level.removeBlock(pos, false);
            level.setBlock(newPos, state.setValue(FACING, facing), Block.UPDATE_ALL);
            if (level.getBlockEntity(newPos) instanceof DrumBlockEntity movedBarrel) {
                movedBarrel.setMixture(barrel.getMixture());
            }
            playRollSound(level, newPos, state);
        }
    }

    private void playFlipSound(Level level, BlockPos pos, BlockState state) {
        level.playSound(
                null,
                pos,
                state.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS,
                state.getSoundType().getVolume(),
                state.getSoundType().getPitch()
        );
    }

    private void playRollSound(Level level, BlockPos pos, BlockState state) {
        level.playSound(
                null,
                pos,
                state.getSoundType().getStepSound(),
                SoundSource.BLOCKS,
                state.getSoundType().getVolume(),
                state.getSoundType().getPitch()
        );
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(OPEN) && facing != Direction.UP) {
            spill(level, pos, facing);
        }

        if (isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinY()) {
            FallingBlockEntity.fall(level, pos, state);
        }
    }

    private void spill(ServerLevel level, BlockPos pos, Direction facing) {
        if (facing == Direction.DOWN && !level.getBlockState(pos.below()).canBeReplaced()) return;

        if (level.getBlockEntity(pos) instanceof DrumBlockEntity barrel) {
            BlockPos spillPos = pos.relative(facing);
            List<SubstanceStack> substances = barrel.getMixture().spreadSubstancesByPercentage(0.1f);
            SubstanceUtil.spill(level, spillPos, substances);
            barrel.getMixture().removeSubstances(substances);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, TICK_DELAY);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        ticks.scheduleTick(pos, this, TICK_DELAY);
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    public static boolean isFree(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.liquid() || state.canBeReplaced();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            boolean open = state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, !open), UPDATE_ALL);
            level.playSound(
                    null,
                    pos,
                    open ? SoundEvents.BARREL_CLOSE : SoundEvents.BARREL_OPEN,
                    SoundSource.BLOCKS,
                    state.getSoundType().getVolume(),
                    state.getSoundType().getPitch()
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }
}
