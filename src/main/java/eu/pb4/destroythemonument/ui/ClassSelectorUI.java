package eu.pb4.destroythemonument.ui;

import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.playerclass.PlayerClass;
import eu.pb4.destroythemonument.game.playerclass.ClassRegistry;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.destroythemonument.other.FormattingUtil;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.GuiLike;
import eu.pb4.sgui.api.gui.SimpleGui;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;

public class ClassSelectorUI extends SimpleGui {
    private final PlayerData playerData;
    private final BaseGameLogic game;
    private final List<PlayerClass> kits;
    @Nullable
    private final GuiLike previousUi;

    public ClassSelectorUI(ServerPlayer player, PlayerData data, BaseGameLogic game, List<PlayerClass> kits) {
        super(getType(kits.size()), player, kits.size() > 53);
        this.playerData = data;
        this.game = game;
        this.kits = kits;
        this.previousUi = SguiUtils.getCurrentGui(player);
        this.setTitle(DtmUtil.getText("ui", "select_class"));
        this.updateIcons();
    }

    private static MenuType<?> getType(int size) {
        if (size <= 8) {
            return MenuType.GENERIC_9x1;
        } else if (size <= 17) {
            return MenuType.GENERIC_9x2;
        } else if (size <= 26) {
            return MenuType.GENERIC_9x3;
        } else if (size <= 35) {
            return MenuType.GENERIC_9x4;
        } else if (size <= 44) {
            return MenuType.GENERIC_9x5;
        } else {
            return MenuType.GENERIC_9x6;
        }
    }

    @Override
    public void afterRemoval() {
        super.afterRemoval();
        if (this.previousUi != null) {
            this.previousUi.open();
        }
    }

    public static void openSelector(ServerPlayer player, BaseGameLogic logic) {
        new ClassSelectorUI(player, logic.participants.get(PlayerRef.of(player)), logic, logic.kits).open();
    }

    public static void openSelector(ServerPlayer player, PlayerData data, List<Identifier> kits) {
        ArrayList<PlayerClass> kitsList = new ArrayList<>();

        for (Identifier id : kits) {
            PlayerClass kit = ClassRegistry.get(id);
            if (kit != null) {
                kitsList.add(kit);
            }
        }

        new ClassSelectorUI(player, data, null, kitsList).open();
    }

    public void updateIcons() {
        int pos = 0;

        for (PlayerClass kit : this.kits) {
            GuiElementBuilder icon = new GuiElementBuilder(kit.icon());
            icon.setName(DtmUtil.getText("class", kit.name()));
            icon.hideDefaultTooltip();
            if (kit == this.playerData.selectedClass) {
                icon.glow();
            }
            icon.addLoreLine(DtmUtil.getText("class", kit.name() + "/description").withStyle(ChatFormatting.RED));
            icon.addLoreLine(Component.empty());
            icon.addLoreLine(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, DtmUtil.getText("ui", "click_select").withStyle(ChatFormatting.GRAY)));
            icon.addLoreLine(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, DtmUtil.getText("ui", "click_preview").withStyle(ChatFormatting.GRAY)));

            icon.setCallback((clickType) -> {
                if (clickType.isLeft) {
                    PlayerUtil.playSoundToPlayer(this.player, SoundEvents.BOOK_PAGE_TURN, SoundSource.UI, 0.5f, 1);
                    changeKit(this.game, this.player, this.playerData, kit);
                } else if (clickType.isRight) {
                    PlayerUtil.playSoundToPlayer(this.player, SoundEvents.BOOK_PAGE_TURN, SoundSource.UI, 0.5f, 1);
                    new ClassPreviewUI(this, kit).open();
                }
                this.updateIcons();
            });

            this.setSlot(pos, icon);
            pos++;
        }
    }

    public static void changeKit(BaseGameLogic game, ServerPlayer player, PlayerData playerData, PlayerClass kit) {
        playerData.selectedClass = kit;

        MutableComponent text = FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, FormattingUtil.GENERAL_STYLE, DtmUtil.getText("message", "selected_class",
                DtmUtil.getText("class", kit.name()).withStyle(ChatFormatting.GOLD)));

        player.sendSystemMessage(text, false);
        boolean isIn = false;
        if (game != null) {
            for (BlockBounds classChange : playerData.teamData.classChange) {
                if (classChange.contains(player.blockPosition())) {
                    isIn = true;
                    break;
                }
            }

            if (isIn && !game.deadPlayers.containsKey(PlayerRef.of(player))) {
                playerData.activeClass = kit;
                playerData.resetTimers();
                game.setupPlayerClass(player, playerData);
            } else {
                player.sendSystemMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, FormattingUtil.GENERAL_STYLE, DtmUtil.getText("message", "class_respawn")), false);
            }
        }
    }
}
