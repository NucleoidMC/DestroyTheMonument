package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.game.ui.ClassSelectorEntry;
import eu.pb4.destroythemonument.game.ui.ClassSelectorUI;
import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.destroythemonument.kit.KitsRegistry;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class DTMClassSelector {
    public static void openSelector(ServerPlayerEntity player, DTMPlayer dtmPlayer, DTMActive game) {
        ClassSelectorUI selector = ClassSelectorUI.create(new TranslatableText("destroythemonument.text.selectclass"), ui -> {
            for (Identifier id : game.config.kits) {
                Kit kit = KitsRegistry.get(id);

                if (kit != null) {
                    ui.add(ClassSelectorEntry.ofIcon(kit.icon)
                            .withName(new TranslatableText("destroythemonument.class." + kit.name))
                            .onUse(p -> {
                                changeKit(game, player, dtmPlayer, kit);
                            }));
                }
            }
        });

        player.openHandledScreen(selector);
    }

    public static void changeKit(DTMActive game, ServerPlayerEntity player, DTMPlayer dtmPlayer, Kit kit) {
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
