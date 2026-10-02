package com.site21.bittermelon.common.content.blocks.barrel;

import com.site21.bittermelon.common.systems.fluid.substance.MixtureOwner;
import com.site21.bittermelon.common.systems.fluid.substance.SubstanceFluid;
import com.site21.bittermelon.common.systems.substance.SubstanceMixture;
import com.site21.bittermelon.init.neoforge.BitterBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import static com.site21.bittermelon.common.content.blocks.barrel.DrumBlock.ROLLING;
import static net.minecraft.world.level.block.Block.UPDATE_CLIENTS;
import static net.minecraft.world.level.block.BrushableBlock.TICK_DELAY;

public class DrumBlockEntity extends BlockEntity implements MixtureOwner {
    private static final float ROT_SPEED = 0.1f;

    private SubstanceMixture mixture;
    private final Runnable mixtureChangedCallback = this::setChanged;
    private Direction moveDirection;
    private float rot = 1.0f;
    private float rot0 = 1.0f;

    public DrumBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BitterBlockEntities.DRUM_BLOCK_ENTITY.get(), worldPosition, blockState);
        mixture = new SubstanceMixture();
        mixture.setChangedCallback(mixtureChangedCallback);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        tickAnimation();

        if (animationFinished() && moveDirection != null) {
            move(level, pos, state);
        }

        if (level.getGameTime() % 20 != 0) return;

        if (mixture != null) {
            mixture.tickReactions(level, pos);
        }
    }

    private void move(Level level, BlockPos pos, BlockState state) {
        BlockPos newPos = pos.relative(moveDirection);
        BlockState newState = state
                .setValue(ROLLING, false)
                .setValue(DrumBlock.FACING, rollFacing(state.getValue(DrumBlock.FACING), moveDirection));
        level.setBlock(newPos, newState, Block.UPDATE_ALL);

        if (level.getBlockEntity(newPos) instanceof DrumBlockEntity movedBarrel) {
            movedBarrel.setMixture(getMixture());
        }

        level.removeBlock(pos, false);
    }

    private static Direction rollFacing(Direction facing, Direction dir) {
        if (facing == dir) return Direction.DOWN;
        if (facing == dir.getOpposite()) return Direction.UP;
        if (facing == Direction.UP) return dir;
        if (facing == Direction.DOWN) return dir.getOpposite();
        return facing;
    }

    public void tickAnimation() {
        if (!animationFinished() && moveDirection != null) {
            rot0 = rot;
            rot += 0.05f + (ROT_SPEED * (1.0f - mixture.getVolume() / (float) SubstanceFluid.FULL_BLOCK_VOLUME));
        }
    }

    public boolean animationFinished() {
        return rot >= 1.0f;
    }

    public float getRotationProgress(float partialTick) {
        if (animationFinished()) return 1.0f;
        return Mth.lerp(partialTick, rot0, rot);
    }

    public void setMoveDirection(Direction moveDirection) {
        this.moveDirection = moveDirection;
        rot = 0.0f;
        rot0 = 0.0f;
    }

    public Direction getMoveDirection() {
        return moveDirection;
    }

    public SubstanceMixture getMixture() {
        return mixture;
    }

    public void setMixture(SubstanceMixture mixture) {
        this.mixture = mixture;
        mixture.setChangedCallback(mixtureChangedCallback);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("mixture", SubstanceMixture.CODEC, mixture);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("mixture", SubstanceMixture.CODEC).ifPresent(loaded -> {
            int oldColor = this.mixture.getColor();
            loaded.setChangedCallback(mixtureChangedCallback);
            this.mixture = loaded;
            int newColor = loaded.getColor();

            if (level != null && level.isClientSide()) {
                if (oldColor != newColor) {
                    level.sendBlockUpdated(worldPosition, Blocks.AIR.defaultBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
                }
            }
        });
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), UPDATE_CLIENTS);
        }
    }
}
