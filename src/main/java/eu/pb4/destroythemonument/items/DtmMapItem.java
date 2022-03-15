package eu.pb4.destroythemonument.items;

import eu.pb4.polymer.api.item.PolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class DtmMapItem extends Item implements PolymerItem {
    public DtmMapItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack stack, @Nullable ServerPlayerEntity player) {
        return Items.FILLED_MAP;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        ItemStack stack = PolymerItem.super.getPolymerItemStack(itemStack, player);
        stack.getOrCreateNbt().putInt("map", 0);
        return stack;
    }
}