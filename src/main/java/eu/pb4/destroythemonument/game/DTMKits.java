package eu.pb4.destroythemonument.game;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
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
        WARRIOR,
        ARCHER,
        CONSTRUCTOR,
        TANK
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
    }

    public static void tryToRestockPlayer(ServerPlayerEntity player, DTMPlayer dtmPlayer) {
        if (dtmPlayer.buildBlockTimer >= 80 && dtmPlayer.activeKit != Kit.CONSTRUCTOR) {
            dtmPlayer.buildBlockTimer -= 80;
            if (player.inventory.count(Items.OAK_PLANKS) < 24) {
                player.inventory.insertStack(new ItemStack(Items.OAK_PLANKS));
            }
        }

        switch (dtmPlayer.activeKit) {
            case WARRIOR:
                if (dtmPlayer.baseItemTimer >= 800) {
                    dtmPlayer.baseItemTimer -= 800;
                    if (player.inventory.count(Items.COOKED_BEEF) < 5) {
                        player.inventory.insertStack(new ItemStack(Items.COOKED_BEEF));
                    }
                }
                break;
            case TANK:
                if (dtmPlayer.baseItemTimer >= 800) {
                    dtmPlayer.baseItemTimer -= 800;
                    if (player.inventory.count(Items.COOKED_BEEF) < 5) {
                        player.inventory.insertStack(new ItemStack(Items.COOKED_BEEF));
                    }
                }
                break;
            case ARCHER:
                if (dtmPlayer.baseItemTimer >= 800) {
                    dtmPlayer.baseItemTimer -= 800;
                    if (player.inventory.count(Items.COOKED_CHICKEN) < 5) {
                        player.inventory.insertStack(new ItemStack(Items.COOKED_BEEF));
                    }
                }

                if (dtmPlayer.specialItemTimer >= 200) {
                    dtmPlayer.baseItemTimer -= 200;
                        if (player.inventory.count(Items.ARROW) < 8) {
                        player.inventory.insertStack(new ItemStack(Items.ARROW));
                    }
                }
                break;
            case CONSTRUCTOR:
                if (dtmPlayer.buildBlockTimer >= 60) {
                    dtmPlayer.buildBlockTimer -= 60;
                    if (player.inventory.count(Items.OAK_PLANKS) < 64) {
                        player.inventory.insertStack(new ItemStack(Items.OAK_PLANKS));
                    }
                }

                if (dtmPlayer.baseItemTimer >= 800) {
                    dtmPlayer.baseItemTimer -= 800;
                    if (player.inventory.count(Items.COOKED_CHICKEN) < 5) {
                        player.inventory.insertStack(new ItemStack(Items.COOKED_BEEF));
                    }
                }

                if (dtmPlayer.specialItemTimer >= 1000) {
                    dtmPlayer.baseItemTimer -= 1000;
                    if (player.inventory.count(Items.TNT) < 1) {
                        player.inventory.insertStack(new ItemStack(Items.TNT));
                    }
                }
                break;
        }
    }


    private static void giveWarriorKit(ServerPlayerEntity player, GameTeam team) {
        player.inventory.insertStack(ItemStackBuilder.of(Items.DIAMOND_SWORD).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.DIAMOND_PICKAXE).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.WOODEN_AXE).addEnchantment(Enchantments.EFFICIENCY, 1).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.COOKED_BEEF).setCount(5).build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.OAK_PLANKS).setCount(24).build());

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
        player.inventory.insertStack(ItemStackBuilder.of(Items.COOKED_CHICKEN).setCount(5).build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.OAK_PLANKS).setCount(24).build());
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
        player.inventory.insertStack(ItemStackBuilder.of(Items.COOKED_BEEF).setCount(5).build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.OAK_PLANKS).setCount(64).build());


        player.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.GOLDEN_HELMET).setUnbreakable().build());
        player.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.IRON_CHESTPLATE).addEnchantment(Enchantments.BLAST_PROTECTION, 6).setUnbreakable().build());
        player.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setUnbreakable().setColor(team.getColor()).build());
    }

    private static void giveTankKit(ServerPlayerEntity player, GameTeam team) {
        player.inventory.insertStack(ItemStackBuilder.of(Items.STONE_SWORD).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.DIAMOND_PICKAXE).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.WOODEN_AXE).addEnchantment(Enchantments.EFFICIENCY, 1).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.COOKED_BEEF).setCount(5).build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.OAK_PLANKS).setCount(24).build());

        player.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.DIAMOND_CHESTPLATE).setUnbreakable().build());
        player.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setUnbreakable().setColor(team.getColor()).build());
        player.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.NETHERITE_BOOTS).setUnbreakable().build());
    }
}
