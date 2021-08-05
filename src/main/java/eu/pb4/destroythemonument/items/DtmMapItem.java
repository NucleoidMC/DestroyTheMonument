package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.game.PlayerData;
import eu.pb4.destroythemonument.game.TeamData;
import eu.pb4.polymer.item.VirtualItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DtmMapItem extends Item implements VirtualItem {
    public DtmMapItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getVirtualItem() {
        return Items.FILLED_MAP;
    }

    @Override
    public ItemStack getVirtualItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        ItemStack stack = VirtualItem.super.getVirtualItemStack(itemStack, player);
        stack.getOrCreateTag().putInt("map", 0);
        return stack;
    }

    public static void updateMap(ServerPlayerEntity player, @Nullable PlayerData playerData, BaseGameLogic logic) {
        List<MapIcon> icons = new ArrayList<>();
        var bytes = new byte[128 * 128];
        var pos = new BlockPos.Mutable();

        int rotationAdd = 0;
        int rotationSym = 1;
        int rotationEntity = 0;

        boolean replaceXZ = false;

        if (playerData != null) {
            TeamData teamData = logic.teams.teamData.get(playerData.team);

            int side = Math.round(teamData.spawnYaw / 90);
            switch (side) {
                case 0 -> {
                    rotationAdd = 127;
                    rotationSym = -1;
                    rotationEntity = 180;
                }
                case 1 -> {
                    replaceXZ = true;
                    rotationEntity = 90;
                    break;
                }
                case -1 -> {
                    replaceXZ = true;
                    rotationAdd = 127;
                    rotationSym = -1;
                    rotationEntity = -90;
                    break;
                }
            }
        }

        int playerX = player.getBlockPos().getX();
        int playerXC = playerX / 16;
        int playerZ = player.getBlockPos().getZ();
        int playerZC = playerZ / 16;

        int offsetX = playerX - playerXC * 16;
        int offsetZ = playerZ - playerZC * 16;

        for (int xC = -5; xC < 5; xC++) {
            for (int zC = -5; zC < 5; zC++) {
                var chunk = player.world.getChunk(pos.set((playerXC + xC) * 16, 0, (playerZC + zC) * 16));
                var heightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE);

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int tX = rotationSym * ((xC + 4) * 16 + x - offsetX) + rotationAdd;
                        int tZ = rotationSym * ((zC + 4) * 16 + z - offsetZ) + rotationAdd;

                        if (replaceXZ) {
                            int tmp = tX;
                            tX = tZ;
                            tZ = tmp;
                        }

                        if (tX >= 128 || tX < 0 || tZ >= 128 || tZ < 0) {
                            continue;
                        }

                        int rX = (xC + playerXC) * 16 + x;
                        int rZ = (zC + playerZC) * 16 + z;

                        int y = heightmap.get(x, z) - 1;
                        var blockState = chunk.getBlockState(pos.set(rX, y, rZ));


                        int color = blockState.getMapColor(player.world, pos).id;
                        bytes[tX + tZ * 128] = (byte) (color == 0 ? color : color * 4 + (y % 3));
                    }
                }
            }
        }

        for (TeamData data : logic.teams.teamData.values()) {
            MapIcon.Type type = MapIcon.Type.byId((byte) (data.team.blockDyeColor().getId() + 10));

            for (BlockPos monument : data.monuments) {
                int mX = rotationSym * (monument.getX() - playerX) * 2;
                int mZ = rotationSym * (monument.getZ() - playerZ) * 2;

                if (replaceXZ) {
                    int tmp = mX;
                    mX = mZ;
                    mZ = tmp;
                }

                if (mX >= 128 || mX <= -128 || mZ >= 128 || mZ < -128) {
                    continue;
                }

                icons.add(new MapIcon( type, (byte) mX, (byte) mZ, (byte) 8, null));
            }

            if (playerData == null || playerData.team == data.team) {
                for (ServerPlayerEntity entity : logic.teams.manager.playersIn(data.team)) {
                    if (entity == player) {
                        continue;
                    }

                    int mX = rotationSym * (entity.getBlockX() - playerX) * 2;
                    int mZ = rotationSym * (entity.getBlockZ() - playerZ) * 2;

                    if (replaceXZ) {
                        int tmp = mX;
                        mX = mZ;
                        mZ = tmp;
                    }

                    if (mX >= 128 || mX <= -128 || mZ >= 128 || mZ < -128) {
                        continue;
                    }

                    icons.add(new MapIcon(MapIcon.Type.BLUE_MARKER, (byte) mX, (byte) mZ, (byte) Math.round((entity.getYaw() + rotationEntity) / 360 * 16), entity.getDisplayName()));
                }
            }

            icons.add(new MapIcon(MapIcon.Type.PLAYER, (byte) 0, (byte) 0, (byte) Math.round((player.getYaw() + rotationEntity) / 360 * 16), null));
        }
        player.networkHandler.sendPacket(new MapUpdateS2CPacket(0, (byte) 0, false, icons, new MapState.UpdateData(0, 0, 128, 128, bytes)));
    }
}