package eu.pb4.destroythemonument.other;

import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.game.PlayerData;
import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.util.BlockBounds;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class ClassSelectorUI extends SimpleGui {
    private final PlayerData playerData;
    private final BaseGameLogic game;

    public ClassSelectorUI(ServerPlayerEntity player, PlayerData data, BaseGameLogic game) {
        super(getType(game.kits.size()), player, game.kits.size() > 53);
        this.playerData = data;
        this.game = game;
        this.setTitle(DtmUtil.getText("ui", "select_class"));
    }

    private static ScreenHandlerType<?> getType(int size) {
        if (size <= 8) {
            return ScreenHandlerType.GENERIC_9X1;
        } else if (size <= 17) {
            return ScreenHandlerType.GENERIC_9X2;
        } else if (size <= 26) {
            return ScreenHandlerType.GENERIC_9X3;
        } else if (size <= 35) {
            return ScreenHandlerType.GENERIC_9X4;
        } else if (size <= 44) {
            return ScreenHandlerType.GENERIC_9X5;
        } else {
            return ScreenHandlerType.GENERIC_9X6;
        }
    }

    public static void openSelector(ServerPlayerEntity player, BaseGameLogic logic) {
        new ClassSelectorUI(player, logic.participants.get(PlayerRef.of(player)), logic).open();
    }

    @Override
    public void onUpdate(boolean firstUpdate) {
        int pos = 0;

        for (Kit kit : this.game.kits) {
            GuiElementBuilder icon = new GuiElementBuilder(Registry.ITEM.get(kit.icon));
            icon.setName(DtmUtil.getText("class", kit.name).setStyle(Style.EMPTY.withItalic(false)));
            icon.hideFlags((byte) 127);
            if (kit == this.playerData.selectedKit) {
                icon.glow();
            }
            icon.addLoreLine(DtmUtil.getText("class", kit.name + "/description").formatted(Formatting.RED));
            icon.addLoreLine(new LiteralText(""));
            icon.addLoreLine(DtmUtil.getFormatted("»", DtmUtil.getText("ui", "click_select").formatted(Formatting.GRAY)));
            icon.addLoreLine(DtmUtil.getFormatted("»", DtmUtil.getText("ui", "click_preview").formatted(Formatting.GRAY)));

            icon.setCallback((x, clickType, z) -> {
                if (clickType.isLeft) {
                    this.player.playSound(SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1);
                    changeKit(this.game, this.player, this.playerData, kit);
                } else if (clickType.isRight) {
                    this.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.5f, 1);
                    this.close();
                    new ClassPreviewUI(this.player, this.game, kit).open();
                }
                this.onUpdate(false);
            });

            this.setSlot(pos, icon);
            pos++;
        }

        super.onUpdate(firstUpdate);
    }

    public static void changeKit(BaseGameLogic game, ServerPlayerEntity player, PlayerData playerData, Kit kit) {
        playerData.selectedKit = kit;

        BlockBounds classChange = game.gameMap.teamRegions.get(playerData.team).classChange;

        MutableText text = DtmUtil.getFormatted("»", DtmUtil.getText("message", "selected_class",
                DtmUtil.getText("class", kit.name).formatted(Formatting.GOLD)).formatted(Formatting.WHITE));

        player.sendMessage(text, false);

        if (classChange.contains(player.getBlockPos()) && !game.deadPlayers.containsKey(PlayerRef.of(player))) {
            playerData.activeKit = kit;
            player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(playerData.activeKit.health);
            playerData.resetTimers();
            game.setInventory(player, playerData);
        } else {
            player.sendMessage(DtmUtil.getFormatted("»", DtmUtil.getText("message", "class_respawn").formatted(Formatting.WHITE)), false);
        }

    }
}
