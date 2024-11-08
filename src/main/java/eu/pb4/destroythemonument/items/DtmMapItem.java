package eu.pb4.destroythemonument.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class DtmMapItem extends Item implements PolymerItem {
    public DtmMapItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return Items.FILLED_MAP;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return null;
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context) {
        out.set(DataComponentTypes.MAP_ID, new MapIdComponent(0));
    }
}