package com.site21.bittermelon.common.content.blocks.barrel;

import com.site21.bittermelon.common.systems.fluid.substance.MixtureOwner;
import com.site21.bittermelon.common.systems.substance.SubstanceContainer;
import com.site21.bittermelon.common.systems.substance.SubstanceMixture;
import com.site21.bittermelon.init.neoforge.BitterBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DrumBlockEntity extends BlockEntity implements MixtureOwner {
    private SubstanceMixture mixture;

    public DrumBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BitterBlockEntities.DRUM_BLOCK_ENTITY.get(), worldPosition, blockState);
        mixture = new SubstanceMixture();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 20 != 0) return;

        if (mixture != null) {
            mixture.tickReactions(level, pos);
        }
    }

    public SubstanceMixture getMixture() {
        return mixture;
    }

    public void setMixture(SubstanceMixture mixture) {
        this.mixture = mixture;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("mixture", SubstanceMixture.CODEC, mixture);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mixture = input.read("mixture", SubstanceMixture.CODEC).orElse(new SubstanceMixture());
    }
}
