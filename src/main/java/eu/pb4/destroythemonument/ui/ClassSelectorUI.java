package eu.pb4.destroythemonument.ui;

import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.destroythemonument.kit.KitsRegistry;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.destroythemonument.other.FormattingUtil;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.ArrayList;
import java.util.List;

public class ClassSelectorUI extends SimpleGui {
    private final PlayerData playerData;
    private final BaseGameLogic game;
    private final List<Kit> kits;

    public ClassSelectorUI(ServerPlayerEntity player, PlayerData data, BaseGameLogic game, List<Kit> kits) {
        super(getType(kits.size()), player, kits.size() > 53);
        this.playerData = data;
        this.game = game;
        this.kits = kits;
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
        new ClassSelectorUI(player, logic.participants.get(PlayerRef.of(player)), logic, logic.kits).open();
    }

    public static void openSelector(ServerPlayerEntity player, PlayerData data, List<Identifier> kits) {
        ArrayList<Kit> kitsList = new ArrayList<>();

        for (Identifier id : kits) {
            Kit kit = KitsRegistry.get(id);
            if (kit != null) {
                kitsList.add(kit);
            }
        }

        new ClassSelectorUI(player, data, null, kitsList).open();
    }

    @Override
    public void onUpdate(boolean firstUpdate) {
        int pos = 0;

        for (Kit kit : this.kits) {
            GuiElementBuilder icon = new GuiElementBuilder(Registry.ITEM.get(kit.icon));
            icon.setName(DtmUtil.getText("class", kit.name));
            icon.hideFlags();
            if (kit == this.playerData.selectedKit) {
                icon.glow();
            }
            icon.addLoreLine(DtmUtil.getText("class", kit.name + "/description").formatted(Formatting.RED));
            icon.addLoreLine(Text.empty());
            icon.addLoreLine(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, DtmUtil.getText("ui", "click_select").formatted(Formatting.GRAY)));
            icon.addLoreLine(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, DtmUtil.getText("ui", "click_preview").formatted(Formatting.GRAY)));

            icon.setCallback((x, clickType, z) -> {
                if (clickType.isLeft) {
                    this.player.playSound(SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1);
                    changeKit(this.game, this.player, this.playerData, kit);
                } else if (clickType.isRight) {
                    this.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.5f, 1);
                    this.close();
                    new ClassPreviewUI(this, kit).open();
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

        MutableText text = FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, FormattingUtil.GENERAL_STYLE, DtmUtil.getText("message", "selected_class",
                DtmUtil.getText("class", kit.name).formatted(Formatting.GOLD)));

        player.sendMessage(text, false);
        boolean isIn = false;
        if (game != null) {
            for (BlockBounds classChange : playerData.teamData.classChange) {
                if (classChange.contains(player.getBlockPos())) {
                    isIn = true;
                    break;
                }
            }

            if (isIn && !game.deadPlayers.containsKey(PlayerRef.of(player))) {
                playerData.activeKit = kit;
                player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(playerData.activeKit.health);
                playerData.resetTimers();
                game.setInventory(player, playerData);
            } else {
                player.sendMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, FormattingUtil.GENERAL_STYLE, DtmUtil.getText("message", "class_respawn")), false);
            }
        }
    }
}
