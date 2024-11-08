package eu.pb4.destroythemonument.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import xyz.nucleoid.packettweaker.PacketContext;

public class FloatingLadderBlock extends LadderBlock implements PolymerBlock {
    public FloatingLadderBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.LADDER.getDefaultState().with(HorizontalFacingBlock.FACING, state.get(HorizontalFacingBlock.FACING));
    }

    @Override
    public boolean handleMiningOnServer(ItemStack tool, BlockState state, BlockPos pos, ServerPlayerEntity player) {
        return false;
    }
}
