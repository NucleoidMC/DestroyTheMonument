package eu.pb4.destroythemonument.blocks;

import eu.pb4.destroythemonument.other.DtmUtil;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.function.Function;

public class DtmBlocks {
    public static final Block WEAK_GLASS = register("weak_glass", AbstractBlock.Settings.copy(Blocks.GLASS).strength(0.2f, 0).dropsNothing(), WeakGlassBlock::new);
    public static final Block LADDER = register("ladder", AbstractBlock.Settings.copy(Blocks.LADDER), FloatingLadderBlock::new);

    public static void register() {

    }

    private static <T extends Block> T register(String name, AbstractBlock.Settings settings, Function<AbstractBlock.Settings, T> func) {
        var id =  DtmUtil.id(name);
        var block = func.apply(settings.registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        Registry.register(Registries.BLOCK, id, block);
        return block;
    }

}
