package eu.pb4.destroythemonument.blocks;

import eu.pb4.destroythemonument.other.DtmUtil;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class DtmBlocks {
    public static final Block WEAK_GLASS = new WeakGlassBlock(AbstractBlock.Settings.copy(Blocks.GLASS).strength(0.2f, 0).dropsNothing());
    public static final Block LADDER = new FloatingLadderBlock(AbstractBlock.Settings.copy(Blocks.LADDER));

    public static void register() {
        register("weak_glass", WEAK_GLASS);
        register("ladder", LADDER);
    }

    private static void register(String name, Block block) {
        Registry.register(Registries.BLOCK, DtmUtil.id(name), block);
    }

}
