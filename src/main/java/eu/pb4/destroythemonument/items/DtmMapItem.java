package eu.pb4.destroythemonument.items;

import eu.pb4.polymer.item.VirtualItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class DtmMapItem extends Item implements VirtualItem {
    public DtmMapItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getVirtualItem() {
        return Items.FILLED_MAP;
    }

    @Override
    public ItemStack getVirtualItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        ItemStack stack = VirtualItem.super.getVirtualItemStack(itemStack, player);
        stack.getOrCreateTag().putInt("map", 0);
        return stack;
    }
}