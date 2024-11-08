package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.blocks.DtmBlocks;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.util.function.Function;

public class DtmItems {
    public static final Item CLASS_SELECTOR = register("class_selector", (settings) -> new SimplePolymerItem(settings, Items.PAPER) {
        final Text NAME = Text.empty().append("[")
                .append(Text.translatable("item.destroy_the_monument.class_selector").formatted(Formatting.GOLD, Formatting.BOLD))
                .append("]").formatted(Formatting.GRAY);

        @Override
        public Text getName(ItemStack stack) {
            return NAME;
        }
    });
    public static final Item MULTI_BLOCK = register("multi_block", MultiBlockItem::new);
    public static final Item WEAK_GLASS = register("weak_glass", (settings) -> new PolymerBlockItem(DtmBlocks.WEAK_GLASS, settings.useBlockPrefixedTranslationKey(), Items.GLASS));
    public static final Item LADDER = register("ladder", (settings) -> new PolymerBlockItem(DtmBlocks.LADDER, settings.useBlockPrefixedTranslationKey(), Items.LADDER));
    public static final Item MAP = register("map", DtmMapItem::new);
    public static final Item TNT = register("tnt", DtmTntItem::new);

    public static void registerItems() {

    }

    private static <T extends Item> T register(String name, Function<Item.Settings, T> func) {
        var id =  DtmUtil.id(name);
        var block = func.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        Registry.register(Registries.ITEM, id, block);
        return block;
    }
}
