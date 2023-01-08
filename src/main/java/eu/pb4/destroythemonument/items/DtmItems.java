package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.blocks.DtmBlocks;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

public class DtmItems {
    public static final Item CLASS_SELECTOR = new SimplePolymerItem(new Item.Settings(), Items.PAPER) {
        final Text NAME = Text.empty().append("[")
                .append(Text.translatable("item.destroy_the_monument.class_selector").formatted(Formatting.GOLD, Formatting.BOLD))
                .append("]").formatted(Formatting.GRAY);

        @Override
        public Text getName(ItemStack stack) {
            return NAME;
        }
    };
    public static final Item MULTI_BLOCK = new MultiBlockItem(new Item.Settings());
    public static final Item WEAK_GLASS = new PolymerBlockItem(DtmBlocks.WEAK_GLASS, new Item.Settings(), Items.GLASS);
    public static final Item LADDER = new PolymerBlockItem(DtmBlocks.LADDER, new Item.Settings(), Items.LADDER);
    public static final Item MAP = new DtmMapItem(new Item.Settings());
    public static final GenericItem GENERIC_ITEM = new GenericItem(new Item.Settings());

    public static void registerItems() {
        register("class_selector", CLASS_SELECTOR);
        register("multi_block", MULTI_BLOCK);
        register("weak_glass", WEAK_GLASS);
        register("map", MAP);
        register("ladder", LADDER);
        register("generic", GENERIC_ITEM);
    }

    private static void register(String name, Item item) {
        Registry.register(Registries.ITEM, DtmUtil.id(name), item);
    }
}
