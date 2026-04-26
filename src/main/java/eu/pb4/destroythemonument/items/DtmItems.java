package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.blocks.DtmBlocks;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;

public class DtmItems {
    public static final Item CLASS_SELECTOR = register("class_selector", (settings) -> new SimplePolymerItem(settings, Items.PAPER) {
        final Component NAME = Component.empty().append("[")
                .append(Component.translatable("item.destroy_the_monument.class_selector").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append("]").withStyle(ChatFormatting.GRAY);

        @Override
        public Component getName(ItemStack stack) {
            return NAME;
        }
    });
    public static final Item MULTI_BLOCK = register("multi_block", MultiBlockItem::new);
    public static final Item WEAK_GLASS = register("weak_glass", (settings) -> new PolymerBlockItem(DtmBlocks.WEAK_GLASS, settings.useBlockDescriptionPrefix(), Items.GLASS));
    public static final Item LADDER = register("ladder", (settings) -> new PolymerBlockItem(DtmBlocks.LADDER, settings.useBlockDescriptionPrefix(), Items.LADDER));
    public static final Item MAP = register("map", DtmMapItem::new);
    public static final Item TNT = register("tnt", DtmTntItem::new);
    public static final Item MINING_TOOL = register("mining_tool", settings -> {
        var lookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        return new SimplePolymerItem(
                settings.component(DataComponents.TOOL, new Tool(List.of(
                                Tool.Rule.deniesDrops(lookup.getOrThrow(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)),
                                Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), 7.5F),
                                Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_AXE), 6.0F),
                                Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), 2.5F),
                                Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_HOE), 2.5F)
                        ), 1.0F, 1, true)
                ), Items.IRON_PICKAXE);
    });

    public static void registerItems() {

    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> func) {
        var id =  DtmUtil.id(name);
        var block = func.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        Registry.register(BuiltInRegistries.ITEM, id, block);
        return block;
    }
}
