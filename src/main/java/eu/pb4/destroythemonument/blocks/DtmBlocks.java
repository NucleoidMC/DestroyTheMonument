package eu.pb4.destroythemonument.blocks;

import eu.pb4.destroythemonument.other.DtmUtil;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DtmBlocks {
    public static final Block WEAK_GLASS = register("weak_glass", BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(0.2f, 0).noLootTable(), WeakGlassBlock::new);
    public static final Block LADDER = register("ladder", BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER), FloatingLadderBlock::new);

    public static void register() {}

    private static <T extends Block> T register(String name, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, T> func) {
        var id =  DtmUtil.id(name);
        var block = func.apply(settings.setId(ResourceKey.create(Registries.BLOCK, id)));
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        return block;
    }

}
