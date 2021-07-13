package eu.pb4.destroythemonument.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.destroythemonument.game.PlayerData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

import java.util.List;

public class Kit {
    public static final Codec<Kit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("kit_name").forGetter(kit -> kit.name),
            Identifier.CODEC.fieldOf("icon").forGetter(kit -> kit.icon),
            Codec.list(ItemStack.CODEC).fieldOf("armor").forGetter(kit -> kit.armor),
            Codec.list(ItemStack.CODEC).fieldOf("items").forGetter(kit -> kit.items),
            Codec.list(RestockableItem.CODEC).fieldOf("restockable_items").forGetter(kit -> kit.restockableItems),
            Codec.INT.optionalFieldOf("blocks_to_planks", 2).forGetter(kit -> kit.blocksToPlanks),
            Codec.DOUBLE.optionalFieldOf("health", 20.0).forGetter(kit -> kit.health)
    ).apply(instance, Kit::new));

    public final String name;
    public final Identifier icon;
    public final List<ItemStack> armor;
    public final List<ItemStack> items;
    public final List<RestockableItem> restockableItems;
    public final int blocksToPlanks;
    public final double health;


    public Kit(String name, Identifier icon, List<ItemStack> armor, List<ItemStack> items, List<RestockableItem> restockableItems, int blockToPlanks, double health) {
        this.name = name;
        this.icon = icon;
        this.armor = armor;
        this.items = items;
        this.restockableItems = restockableItems;
        this.blocksToPlanks = blockToPlanks;
        this.health = health;
    }


    public void equipPlayer(ServerPlayerEntity player, GameTeam team) {
        for (ItemStack itemStack : this.items) {
            player.getInventory().insertStack(ItemStackBuilder.of(itemStack).setUnbreakable().build());
        }

        for (RestockableItem ri : this.restockableItems) {
            if (ri.startingCount > 0) {
                ItemStack stack = ri.itemStack.copy();
                stack.setCount(ri.startingCount);
                player.getInventory().insertStack(stack);
            }
        }

        player.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(this.armor.get(0)).setUnbreakable().setDyeColor(team.dye().getRgb()).build());
        player.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(this.armor.get(1)).setUnbreakable().setDyeColor(team.dye().getRgb()).build());
        player.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(this.armor.get(2)).setUnbreakable().setDyeColor(team.dye().getRgb()).build());
        player.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(this.armor.get(3)).setUnbreakable().setDyeColor(team.dye().getRgb()).build());
    }

    public void maybeRestockPlayer(ServerPlayerEntity player, PlayerData playerData) {
        for (RestockableItem ri : this.restockableItems) {
            if (playerData.restockTimers.getInt(ri) >= ri.restockTime && player.getInventory().count(ri.itemStack.getItem()) < ri.maxCount) {
                ItemStack stack = ri.itemStack.copy();
                player.getInventory().insertStack(stack);
                playerData.restockTimers.put(ri, 0);
            }
        }
    }


    public static class RestockableItem {
        public static final Codec<RestockableItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("item").forGetter(i -> i.itemStack),
                Codec.INT.fieldOf("restock_time").forGetter(i -> i.restockTime),
                Codec.INT.fieldOf("max_count").forGetter(i -> i.maxCount),
                Codec.INT.optionalFieldOf("start_count", 0).forGetter(i -> i.startingCount),
                Codec.INT.optionalFieldOf("start_offset", 0).forGetter(i -> i.startingOffset)
            ).apply(instance, RestockableItem::new));

        public final ItemStack itemStack;
        public final int restockTime;
        public final int maxCount;
        public final int startingCount;
        public final int startingOffset;

        public RestockableItem(ItemStack itemStack, int restockTime, int maxCount, int startingCount, int startingOffset) {
            this.itemStack = itemStack;
            this.restockTime = restockTime;
            this.maxCount = maxCount;
            this.startingCount = startingCount;
            this.startingOffset = startingOffset;
        }

    }
}
