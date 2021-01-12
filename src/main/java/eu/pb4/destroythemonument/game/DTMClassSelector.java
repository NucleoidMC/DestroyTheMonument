package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.game.ui.ClassSelectorEntry;
import eu.pb4.destroythemonument.game.ui.ClassSelectorUI;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class DTMClassSelector {
    public static void openSelector(ServerPlayerEntity player, DTMPlayer dtmPlayer, DTMActive game) {
        ClassSelectorUI selector = ClassSelectorUI.create(new LiteralText("Select Class"), ui -> {
            ui.add(ClassSelectorEntry.ofIcon(Items.DIAMOND_SWORD)
                    .withName(new LiteralText("Warrior"))
                    .onUse(p -> {
                        changeKit(game, player, dtmPlayer, DTMKits.Kit.WARRIOR);
                    }));

            ui.add(ClassSelectorEntry.ofIcon(Items.BOW)
                    .withName(new LiteralText("Archer"))
                    .onUse(p -> {
                        changeKit(game, player, dtmPlayer, DTMKits.Kit.ARCHER);
                    }));
            ui.add(ClassSelectorEntry.ofIcon(Items.DIAMOND_CHESTPLATE)
                    .withName(new LiteralText("Tank"))
                    .onUse(p -> {
                        changeKit(game, player, dtmPlayer, DTMKits.Kit.TANK);
                    }));
            ui.add(ClassSelectorEntry.ofIcon(Items.IRON_AXE)
                    .withName(new LiteralText("Constructor"))
                    .onUse(p -> {
                        changeKit(game, player, dtmPlayer, DTMKits.Kit.CONSTRUCTOR);
                    }));
        });

        player.openHandledScreen(selector);
    }

    public static void changeKit(DTMActive game, ServerPlayerEntity player, DTMPlayer dtmPlayer, DTMKits.Kit kit) {
        dtmPlayer.selectedKit = kit;

        BlockBounds classChange = game.gameMap.teamRegions.get(dtmPlayer.team).classChange;

        if (classChange.contains(player.getBlockPos())) {
            dtmPlayer.activeKit = kit;
            dtmPlayer.resetTimers();
            game.setInventory(player, dtmPlayer);
        }
    }

}
