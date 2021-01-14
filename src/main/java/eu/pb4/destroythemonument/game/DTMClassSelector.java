package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.game.ui.ClassSelectorEntry;
import eu.pb4.destroythemonument.game.ui.ClassSelectorUI;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class DTMClassSelector {
    public static void openSelector(ServerPlayerEntity player, DTMPlayer dtmPlayer, DTMActive game) {
        ClassSelectorUI selector = ClassSelectorUI.create(new TranslatableText("destroythemonument.text.selectclass"), ui -> {
            ui.add(ClassSelectorEntry.ofIcon(Items.DIAMOND_SWORD)
                    .withName(new TranslatableText("destroythemonument.class.warrior"))
                    .onUse(p -> {
                        changeKit(game, player, dtmPlayer, DTMKits.Kit.WARRIOR);
                    }));

            ui.add(ClassSelectorEntry.ofIcon(Items.BOW)
                    .withName(new TranslatableText("destroythemonument.class.archer"))
                    .onUse(p -> {
                        changeKit(game, player, dtmPlayer, DTMKits.Kit.ARCHER);
                    }));
            ui.add(ClassSelectorEntry.ofIcon(Items.DIAMOND_CHESTPLATE)
                    .withName(new TranslatableText("destroythemonument.class.tank"))
                    .onUse(p -> {
                        changeKit(game, player, dtmPlayer, DTMKits.Kit.TANK);
                    }));
            ui.add(ClassSelectorEntry.ofIcon(Items.IRON_AXE)
                    .withName(new TranslatableText("destroythemonument.class.constructor"))
                    .onUse(p -> {
                        changeKit(game, player, dtmPlayer, DTMKits.Kit.CONSTRUCTOR);
                    }));
        });

        player.openHandledScreen(selector);
    }

    public static void changeKit(DTMActive game, ServerPlayerEntity player, DTMPlayer dtmPlayer, DTMKits.Kit kit) {
        dtmPlayer.selectedKit = kit;

        BlockBounds classChange = game.gameMap.teamRegions.get(dtmPlayer.team).classChange;

        MutableText text = new LiteralText("» ").formatted(Formatting.GRAY)
                .append(new TranslatableText("destroythemonument.text.selectedclass",
                        new TranslatableText("destroythemonument.class." + kit.name).formatted(Formatting.GOLD)
                ).formatted(Formatting.WHITE));


        if (classChange.contains(player.getBlockPos())) {
            dtmPlayer.activeKit = kit;
            dtmPlayer.resetTimers();
            game.setInventory(player, dtmPlayer);
        } else {
            text.append(new TranslatableText("destroythemonument.text.classrespawn").formatted(Formatting.WHITE));
        }

        player.sendMessage(text, false);
    }

}
