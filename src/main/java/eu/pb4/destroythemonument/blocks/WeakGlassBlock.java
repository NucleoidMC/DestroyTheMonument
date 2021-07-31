package eu.pb4.destroythemonument.blocks;

import eu.pb4.polymer.block.VirtualBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlassBlock;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public class WeakGlassBlock extends GlassBlock implements VirtualBlock {
    public WeakGlassBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Block getVirtualBlock() {
        return Blocks.GLASS;
    }

    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        world.breakBlock(hit.getBlockPos(), false);
        super.onProjectileHit(world, state, hit, projectile);
    }
}
