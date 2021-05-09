package eu.pb4.destroythemonument.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.destroythemonument.game.PlayerData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

import java.util.List;

public class Kit {
    public static final Codec<Kit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("kit_name").forGetter(kit -> kit.name),
            Identifier.CODEC.fieldOf("icon").forGetter(kit -> kit.icon),
            Codec.INT.fieldOf("number_of_planks").forGetter(kit -> kit.numberOfPlanks),
            Codec.INT.fieldOf("blocks_to_planks").forGetter(kit -> kit.blockToPlanks),
            Codec.INT.fieldOf("planks_restock_time").forGetter(kit -> kit.planksRestockTime),
            ItemStack.CODEC.fieldOf("food_item").forGetter(kit -> kit.foodItem),
            Codec.INT.fieldOf("food_restock_time").forGetter(kit -> kit.foodRestockTime),
            Codec.list(ItemStack.CODEC).fieldOf("armor").forGetter(kit -> kit.armor),
            Codec.list(ItemStack.CODEC).fieldOf("items").forGetter(kit -> kit.items),
            ItemStack.CODEC.optionalFieldOf("additional_item", new ItemStack(Items.AIR)).forGetter(kit -> kit.additionalItem),
            Codec.INT.optionalFieldOf("additional_restock_time", 9999).forGetter(kit -> kit.additionalRestockTime)
        ).apply(instance, Kit::new));

    public final String name;
    public final Identifier icon;
    public final int numberOfPlanks;
    public final int blockToPlanks;
    public final int planksRestockTime;
    public final ItemStack foodItem;
    public final int foodRestockTime;
    public final List<ItemStack> armor;
    public final List<ItemStack> items;
    public final ItemStack additionalItem;
    public final int additionalRestockTime;


    public Kit(String name, Identifier icon, int numberOfPlanks, int blockToPlanks, int planksRestockTime, ItemStack foodItem,
               int foodRestockTime, List<ItemStack> armor, List<ItemStack> items, ItemStack additionalItem, int additionalRestockTime) {
        this.name = name;
        this.icon = icon;
        this.numberOfPlanks = numberOfPlanks;
        this.blockToPlanks = blockToPlanks;
        this.planksRestockTime = planksRestockTime;
        this.foodItem = foodItem;
        this.foodRestockTime = foodRestockTime;
        this.armor = armor;
        this.items = items;
        this.additionalItem = additionalItem;
        this.additionalRestockTime = additionalRestockTime;
    }


    public void equipPlayer(ServerPlayerEntity player, GameTeam team) {
        for (ItemStack itemStack : this.items) {
            player.inventory.insertStack(ItemStackBuilder.of(itemStack).setUnbreakable().build());
        }

        player.inventory.insertStack(this.foodItem.copy());
        if (this.additionalItem != null) {
            player.inventory.insertStack(this.additionalItem.copy());
        }

        player.inventory.insertStack(new ItemStack(Items.OAK_PLANKS, this.numberOfPlanks));

        player.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(this.armor.get(0)).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(this.armor.get(1)).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(this.armor.get(2)).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(this.armor.get(3)).setUnbreakable().setColor(team.getColor()).build());
    }

    public void maybeRestockPlayer(ServerPlayerEntity player, PlayerData dtmPlayer) {
        if (player.inventory.count(Items.OAK_PLANKS) >= this.numberOfPlanks) {
            dtmPlayer.buildBlockTimer = 0;
        } else {
            if (dtmPlayer.buildBlockTimer >= this.planksRestockTime) {
                dtmPlayer.buildBlockTimer -= this.planksRestockTime;
                player.inventory.insertStack(new ItemStack(Items.OAK_PLANKS));
            }
        }

        if (player.inventory.count(this.foodItem.getItem()) >= this.foodItem.getCount()) {
            dtmPlayer.foodItemTimer = 0;
        } else {
            if (dtmPlayer.foodItemTimer >= this.foodRestockTime) {
                dtmPlayer.foodItemTimer -= this.foodRestockTime;
                player.inventory.insertStack(ItemStackBuilder.of(this.foodItem).setCount(1).build());
            }
        }

        if (this.additionalItem.getItem() != Items.AIR) {
            if (player.inventory.count(this.additionalItem.getItem()) >= this.additionalItem.getCount()) {
                dtmPlayer.additionalItemTimer = 0;
            } else {
                if (dtmPlayer.additionalItemTimer >= this.additionalRestockTime) {
                    dtmPlayer.additionalItemTimer -= this.additionalRestockTime;
                    player.inventory.insertStack(ItemStackBuilder.of(this.additionalItem).setCount(1).build());
                }
            }
        }

    }
}
