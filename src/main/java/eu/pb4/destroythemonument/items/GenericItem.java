package eu.pb4.destroythemonument.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.impl.PolymerImplUtils;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class GenericItem extends Item implements PolymerItem {
    public GenericItem(Settings settings) {
        super(settings);
    }

    @Override
    public String getTranslationKey() {
        return "";
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, ServerPlayerEntity player) {
        if (itemStack.hasNbt() && itemStack.getNbt().contains("DisplayItem", NbtElement.COMPOUND_TYPE)) {
            var id = Identifier.tryParse(itemStack.getSubNbt("DisplayItem").getString("id"));
            return Registries.ITEM.get(id);
        }

        return Items.STONE;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipContext context, ServerPlayerEntity player) {
        var out = PolymerItemUtils.createItemStack(itemStack, context, player);

        if (itemStack.hasNbt() && itemStack.getNbt().contains("DisplayItem", NbtElement.COMPOUND_TYPE)) {
            var def = itemStack.getSubNbt("DisplayItem");

            if (def.contains("tag", NbtElement.COMPOUND_TYPE)) {
                out.getNbt().copyFrom(def.getCompound("tag"));
            }
        }

        return out;
    }

    public ItemStack create(ItemStack stack) {
        var out = new ItemStack(this);
        out.getOrCreateNbt().put("DisplayItem", stack.writeNbt(new NbtCompound()));
        return out;
    }
}
