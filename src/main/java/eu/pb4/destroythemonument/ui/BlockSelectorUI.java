package eu.pb4.destroythemonument.ui;

import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.game.PlayerData;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.Block;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class BlockSelectorUI extends SimpleGui {
    private final PlayerData playerData;
    private final BaseGameLogic game;

    public BlockSelectorUI(ServerPlayerEntity player, PlayerData data, BaseGameLogic game) {
        super(ScreenHandlerType.GENERIC_9X1, player, false);
        this.playerData = data;
        this.game = game;
        this.setTitle(DtmUtil.getText("ui", "select_block"));
    }

    public static void openSelector(ServerPlayerEntity player, BaseGameLogic logic) {
        new BlockSelectorUI(player, logic.participants.get(PlayerRef.of(player)), logic).open();
    }


    @Override
    public void onUpdate(boolean firstUpdate) {
        int pos = 0;
        for (Block block : TagRegistry.block(DtmUtil.id("building_blocks")).values()) {
            GuiElementBuilder icon = new GuiElementBuilder(block.asItem(), 1);
            icon.setCallback((x, clickType, z) -> {
                this.playerData.selectedBlock = block;
                this.close();
                this.onUpdate(false);
            });

            this.setSlot(pos, icon);
            pos++;
        }

        super.onUpdate(firstUpdate);
    }
}
