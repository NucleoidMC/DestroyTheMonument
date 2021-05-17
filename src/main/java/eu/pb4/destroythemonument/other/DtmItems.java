package eu.pb4.destroythemonument.other;

import eu.pb4.polymer.item.BasicVirtualItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;

public class DtmItems {
    public static final Item CLASS_SELECTOR = createBasic(Items.PAPER, Rarity.EPIC);

    public static void registerItems() {
        register("class_selector", CLASS_SELECTOR);
    }

    private static Item createBasic(Item virtual) {
        return new BasicVirtualItem(new Item.Settings(), virtual);
    }

    private static Item createBasic(Item virtual, Rarity rarity) {
        return new BasicVirtualItem(new Item.Settings().rarity(rarity), virtual);
    }

    private static void register(String name, Item item) {
        Registry.register(Registry.ITEM, DtmUtil.id(name), item);
    }
}
