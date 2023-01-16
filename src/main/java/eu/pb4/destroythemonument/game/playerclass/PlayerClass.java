package eu.pb4.destroythemonument.game.playerclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.items.DtmItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.codecs.MoreCodecs;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PlayerClass(
        String name, ItemStack icon,
        Map<EntityAttribute, Double> attributes,
        ItemStack pickaxe, ItemStack axe,
        List<ItemStack> armorVisual, List<ItemStack> items,
        List<RestockableItem> restockableItems,
        int blocksToPlanks
) {
    public static final UUID TOOL_DAMAGE = UUID.fromString("c7e311fa-857a-4354-bbcb-2e589d9e868d");
    public static final int TOOL_REPAIR_COST = 9943235;

    public static final Codec<PlayerClass> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("class_name").forGetter(PlayerClass::name),
            MoreCodecs.ITEM_STACK.fieldOf("icon").forGetter(PlayerClass::icon),
            Codec.unboundedMap(Registries.ATTRIBUTE.getCodec(), Codec.DOUBLE).optionalFieldOf("attributes", Map.of()).forGetter(PlayerClass::attributes),
            MoreCodecs.ITEM_STACK.fieldOf("pickaxe_tool").forGetter(PlayerClass::pickaxe),
            MoreCodecs.ITEM_STACK.fieldOf("axe_tool").forGetter(PlayerClass::axe),
            Codec.list(MoreCodecs.ITEM_STACK).fieldOf("armor").forGetter(PlayerClass::armorVisual),
            Codec.list(MoreCodecs.ITEM_STACK).fieldOf("items").forGetter(PlayerClass::items),
            Codec.list(RestockableItem.CODEC).fieldOf("restockable_items").forGetter(PlayerClass::restockableItems),
            Codec.INT.optionalFieldOf("blocks_to_planks", 2).forGetter(PlayerClass::blocksToPlanks)
    ).apply(instance, PlayerClass::new));

    public void setupPlayer(ServerPlayerEntity player, TeamData team) {
        for (var x : this.attributes.entrySet()) {
            player.getAttributes().getCustomInstance(x.getKey()).setBaseValue(x.getValue());
        }

        player.getInventory().insertStack(ItemStackBuilder.of(this.pickaxe)
                .addModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(TOOL_DAMAGE, "tool", 0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL), EquipmentSlot.MAINHAND)
                .setUnbreakable()
                .setRepairCost(TOOL_REPAIR_COST)
                .build());

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

        player.equipStack(EquipmentSlot.HEAD, DtmItems.GENERIC_ITEM.create(this.armorVisual.get(0), team.getConfig().dyeColor().getRgb()));
        player.equipStack(EquipmentSlot.CHEST, DtmItems.GENERIC_ITEM.create(this.armorVisual.get(1), team.getConfig().dyeColor().getRgb()));
        player.equipStack(EquipmentSlot.LEGS, DtmItems.GENERIC_ITEM.create(this.armorVisual.get(2), team.getConfig().dyeColor().getRgb()));
        player.equipStack(EquipmentSlot.FEET, DtmItems.GENERIC_ITEM.create(this.armorVisual.get(3), team.getConfig().dyeColor().getRgb()));

        player.equipStack(EquipmentSlot.OFFHAND, new ItemStack(DtmItems.MAP));
    }

    public void updateMainTool(ServerPlayerEntity player, BlockState state) {
        ItemStack stack = player.getMainHandStack();
        if (stack.getRepairCost() == TOOL_REPAIR_COST) {
            ItemStack base = null;
            if (state.isIn(BlockTags.AXE_MINEABLE)) {
                if (!(stack.getItem() instanceof AxeItem)) {
                    base = this.axe;
                }
            } else if (state.isIn(BlockTags.PICKAXE_MINEABLE)) {
                if (!(stack.getItem() instanceof PickaxeItem)) {
                    base = this.pickaxe;
                }
            }

            if (base != null && !base.isEmpty()) {
                player.getInventory().setStack(player.getInventory().selectedSlot, ItemStackBuilder.of(base)
                        .addModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(TOOL_DAMAGE, "tool", 0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL), EquipmentSlot.MAINHAND)
                        .setUnbreakable()
                        .setRepairCost(TOOL_REPAIR_COST)
                        .build());
            }

        }
    }

    public void maybeRestockPlayer(ServerPlayerEntity player, PlayerData playerData) {
        for (RestockableItem ri : this.restockableItems) {
            var timer = playerData.restockTimers.getInt(ri);
            if (timer >= ri.restockTime && player.getInventory().count(ri.itemStack.getItem()) < ri.maxCount) {
                ItemStack stack = ri.itemStack.copy();
                player.getInventory().insertStack(stack);
                playerData.restockTimers.put(ri, 0);
            }
        }
    }


    public static class RestockableItem {
        public static final Codec<RestockableItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                MoreCodecs.ITEM_STACK.fieldOf("item").forGetter(i -> i.itemStack),
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
