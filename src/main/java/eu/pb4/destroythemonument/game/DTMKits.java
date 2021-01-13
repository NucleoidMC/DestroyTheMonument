package eu.pb4.destroythemonument.game;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

/*
 * Rework it later
 */

public class DTMKits {
    public enum Kit {
        WARRIOR("Warrior", 24, 80, 5, Items.COOKED_COD, 800),
        ARCHER("Archer", 16, 80, 5, Items.COOKED_CHICKEN, 800),
        CONSTRUCTOR("Constructor", 64, 50, 5, Items.COOKED_BEEF, 800),
        TANK("Tank", 16, 80, 5, Items.COOKED_BEEF, 800);

        public final String name;
        public final int plankNumber;
        public final int planksRestock;
        public final int foodNumber;
        public final Item foodItem;
        public final int foodRestock;

        private Kit(String name, int plankNumber, int planksRestock, int foodNumber, Item foodItem, int foodRestock) {
            this.name = name;
            this.plankNumber = plankNumber;
            this.planksRestock = planksRestock;

            this.foodNumber = foodNumber;
            this.foodItem = foodItem;
            this.foodRestock = foodRestock;

        }
    }


    public static void equipPlayer(ServerPlayerEntity player, DTMPlayer dtmPlayer) {
        switch (dtmPlayer.activeKit) {
            case WARRIOR:
                DTMKits.giveWarriorKit(player, dtmPlayer.team);
                break;
            case ARCHER:
                DTMKits.giveArcherKit(player, dtmPlayer.team);
                break;
            case CONSTRUCTOR:
                DTMKits.giveConstructorKit(player, dtmPlayer.team);
                break;
            case TANK:
                DTMKits.giveTankKit(player, dtmPlayer.team);
                break;
        }

        player.inventory.insertStack(ItemStackBuilder.of(dtmPlayer.activeKit.foodItem).setCount(dtmPlayer.activeKit.foodNumber).build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.OAK_PLANKS).setCount(dtmPlayer.activeKit.plankNumber).build());
    }

    public static void tryToRestockPlayer(ServerPlayerEntity player, DTMPlayer dtmPlayer) {
        if (player.inventory.count(Items.OAK_PLANKS) >= dtmPlayer.activeKit.plankNumber) {
            dtmPlayer.buildBlockTimer = 0;
        } else {
            if (dtmPlayer.buildBlockTimer >= dtmPlayer.activeKit.planksRestock) {
                dtmPlayer.buildBlockTimer -= dtmPlayer.activeKit.planksRestock;
                player.inventory.insertStack(new ItemStack(Items.OAK_PLANKS));
            }
        }

        if (player.inventory.count(dtmPlayer.activeKit.foodItem) >= dtmPlayer.activeKit.foodNumber) {
            dtmPlayer.baseItemTimer = 0;
        } else if (dtmPlayer.baseItemTimer >= dtmPlayer.activeKit.foodRestock) {
            dtmPlayer.baseItemTimer -= dtmPlayer.activeKit.foodRestock;
            player.inventory.insertStack(new ItemStack(dtmPlayer.activeKit.foodItem));
        }


        switch (dtmPlayer.activeKit) {
            case ARCHER:
                if (player.inventory.count(Items.COOKED_CHICKEN) >= 5) {
                    dtmPlayer.baseItemTimer = 0;
                } else if (dtmPlayer.baseItemTimer >= 800) {
                    dtmPlayer.baseItemTimer -= 800;
                    player.inventory.insertStack(new ItemStack(Items.COOKED_CHICKEN));
                }

                if (player.inventory.count(Items.ARROW) >= 8) {
                    dtmPlayer.specialItemTimer = 0;
                } else if (dtmPlayer.specialItemTimer >= 200) {
                    dtmPlayer.baseItemTimer -= 200;
                    player.inventory.insertStack(new ItemStack(Items.ARROW));
                }
                break;

            case CONSTRUCTOR:
                if (player.inventory.count(Items.TNT) >= 1) {
                    dtmPlayer.specialItemTimer = 0;
                } else if (dtmPlayer.specialItemTimer >= 1000) {
                    dtmPlayer.baseItemTimer -= 1000;
                    player.inventory.insertStack(new ItemStack(Items.TNT));
                }
                break;
        }
    }


    private static void giveWarriorKit(ServerPlayerEntity player, GameTeam team) {
        player.inventory.insertStack(ItemStackBuilder.of(Items.DIAMOND_SWORD).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.DIAMOND_PICKAXE).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.WOODEN_AXE).addEnchantment(Enchantments.EFFICIENCY, 1).setUnbreakable().build());

        player.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.IRON_CHESTPLATE).setUnbreakable().build());
        player.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.IRON_BOOTS).setUnbreakable().build());
    }

    private static void giveArcherKit(ServerPlayerEntity player, GameTeam team) {
        player.inventory.insertStack(ItemStackBuilder.of(Items.GOLDEN_SWORD).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.BOW).addEnchantment(Enchantments.POWER, 1).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.DIAMOND_PICKAXE).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.WOODEN_AXE).addEnchantment(Enchantments.EFFICIENCY, 1).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.ARROW).setCount(8).build());


        player.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.CHAINMAIL_CHESTPLATE).setUnbreakable().build());
        player.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.GOLDEN_BOOTS).setUnbreakable().build());
    }

    private static void giveConstructorKit(ServerPlayerEntity player, GameTeam team) {
        player.inventory.insertStack(ItemStackBuilder.of(Items.WOODEN_SWORD).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.TNT).build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.DIAMOND_PICKAXE).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.IRON_AXE).addEnchantment(Enchantments.EFFICIENCY, 2).setUnbreakable().build());


        player.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.GOLDEN_HELMET).setUnbreakable().build());
        player.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.IRON_CHESTPLATE).addEnchantment(Enchantments.BLAST_PROTECTION, 6).setUnbreakable().build());
        player.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setUnbreakable().setColor(team.getColor()).build());
    }

    private static void giveTankKit(ServerPlayerEntity player, GameTeam team) {
        player.inventory.insertStack(ItemStackBuilder.of(Items.STONE_SWORD).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.DIAMOND_PICKAXE).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.WOODEN_AXE).addEnchantment(Enchantments.EFFICIENCY, 1).setUnbreakable().build());

        player.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.DIAMOND_CHESTPLATE).setUnbreakable().build());
        player.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.NETHERITE_BOOTS).setUnbreakable().build());
    }
}
