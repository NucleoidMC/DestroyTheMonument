package eu.pb4.destroythemonument.ui;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class BlockSelectorUI extends SimpleGui {
    private final PlayerData playerData;

    public BlockSelectorUI(ServerPlayer player, PlayerData data, BaseGameLogic game) {
        super(MenuType.GENERIC_9x1, player, false);
        this.playerData = data;
        this.setTitle(DtmUtil.getText("ui", "select_block"));

        int pos = 0;
        for (var block : BuiltInRegistries.BLOCK.getOrThrow(DTM.BUILDING_BLOCKS)) {
            GuiElementBuilder icon = new GuiElementBuilder(block.value().asItem(), 1);
            icon.setCallback(() -> {
                this.playerData.selectedBlock = block.value();
                this.close();
            });

            this.setSlot(pos, icon);
            pos++;
        }
    }

    public static void openSelector(ServerPlayer player, BaseGameLogic logic) {
        if (SguiUtils.getCurrentGui(player) instanceof BlockSelectorUI) {
            return;
        }

        new BlockSelectorUI(player, logic.participants.get(PlayerRef.of(player)), logic).open();
    }
}
