package eu.pb4.destroythemonument.blocks;

import eu.pb4.polymer.block.VirtualBlock;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public class FloatingLadderBlock extends LadderBlock implements VirtualBlock {
    public FloatingLadderBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return true;
    }

    @Override
    public Block getVirtualBlock() {
        return Blocks.LADDER;
    }

    @Override
    public BlockState getVirtualBlockState(BlockState state) {
        return Blocks.LADDER.getDefaultState().with(HorizontalFacingBlock.FACING, state.get(HorizontalFacingBlock.FACING));
    }
}
