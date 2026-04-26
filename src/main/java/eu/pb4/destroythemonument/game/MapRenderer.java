package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.map.GameMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapRenderer {
    private final BaseGameLogic logic;
    private final GameMap map;
    private final byte[] mapData;
    private short[] mapHeight;
    private final int xSize;
    private final int zSize;
    private final int halfXSize;
    private final int halfZSize;

    private int currentPosX;
    private int currentPosZ;

    public MapRenderer(BaseGameLogic logic) {
        this.logic = logic;
        this.map = logic.gameMap;
        this.xSize = this.map.mapBounds.max().getX() - this.map.mapBounds.min().getX() + 4;
        this.zSize = this.map.mapBounds.max().getZ() - this.map.mapBounds.min().getZ() + 4;
        this.mapData = new byte[this.xSize * this.zSize];
        this.mapHeight = new short[this.xSize * this.zSize];

        this.halfXSize = this.xSize / 2;
        this.halfZSize = this.zSize / 2;

        this.renderWorld(this.map.mapBounds.min().getX(), this.map.mapBounds.min().getZ(), this.map.mapBounds.max().getX(), this.map.mapBounds.max().getZ());
        this.currentPosX = this.map.mapBounds.min().getX();
        this.currentPosZ = this.map.mapBounds.min().getZ();
    }

    public void renderWorld(int fromX, int fromZ, int toX, int toZ) {
        var world = this.map.world;
        var pos = new BlockPos.MutableBlockPos();

        var min = this.map.mapBounds.min();

        for (int x = fromX; x < toX; x++) {
            int iX = x - min.getX();

            for (int z = fromZ; z < toZ; z++) {
                int iZ = z - min.getZ();


                int index = iX + 1 + (iZ + 1) * this.xSize;

                int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;

                if (this.map.mapBounds.contains(x, y, z)) {
                    var blockState = world.getBlockState(pos.set(x, y, z));
                    int color = blockState.getMapColor(world, pos).id;
                    if (color == 0) {
                        this.mapData[index] = (byte) 0;
                    } else {
                        this.mapData[index] = (byte) (color * 4);
                    }
                } else {
                    this.mapData[index] = (byte) 0;
                }
                this.mapHeight[index] = (short) y;
            }
        }

        for (int x = 0; x < this.xSize; x++) {
            byte val = (byte) (MapColor.COLOR_GRAY.id * 4 + (x % 2) * 2);
            this.mapData[x] = val;
            this.mapData[x + (this.zSize - 1) * this.xSize] = val;
        }

        for (int z = 0; z < this.zSize; z++) {
            byte val = (byte) (MapColor.COLOR_GRAY.id * 4 + (z % 2) * 2);
            this.mapData[z * this.xSize] = val;
            this.mapData[this.xSize - 1 + z * this.xSize] = val;
        }
    }

    public void updateMap(ServerPlayer player, @Nullable PlayerData playerData) {
        List<MapDecoration> icons = new ArrayList<>();
        var bytes = new byte[128 * 128];

        int rotationAddX = 0;
        int rotationAddZ = 0;
        int rotationSymX = 1;
        int rotationSymZ = 1;

        int rotationEntity = 0;

        boolean replaceXZ = false;

        if (playerData != null) {
            TeamData teamData = playerData.teamData;

            int side = Math.round(teamData.spawnYaw / 90);
            switch (side) {
                case 0 -> {
                    rotationAddX = 127;
                    rotationAddZ = 127;
                    rotationSymX = -1;
                    rotationSymZ = -1;
                    rotationEntity = 180;
                }
                case -1 -> {
                    replaceXZ = true;
                    rotationEntity = -90;
                    rotationAddX = 127;
                    rotationSymX = -1;
                    break;
                }
                case 1 -> {
                    replaceXZ = true;
                    rotationAddZ = 127;
                    rotationSymZ = -1;
                    rotationEntity = 90;
                    break;
                }
            }
        }

        int playerX = player.blockPosition().getX();
        int playerZ = player.blockPosition().getZ();

        for (int x = 0; x < 127; x++) {
            for (int z = 0; z < 127; z++) {
                int tX = rotationSymX * (x) + rotationAddX;
                int tZ = rotationSymZ * (z) + rotationAddZ;

                if (replaceXZ) {
                    int tmp = tX;
                    tX = tZ;
                    tZ = tmp;
                }

                if (tX >= 128 || tX < 0 || tZ >= 128 || tZ < 0) {
                    continue;
                }

                int rX = playerX + x - 64 + this.halfXSize - (int) this.map.mapBounds.center().x();
                int rZ = playerZ + z - 64 + this.halfZSize - (int) this.map.mapBounds.center().z();

                if (rX >= this.xSize || rX < 0 || rZ >= this.zSize || rZ < 0) {
                    continue;
                }

                int height;
                var index = rX + rZ * this.xSize;
                var y = this.mapHeight[index];
                if (rX - rotationSymX >= this.xSize || rX - rotationSymX < 0 || rZ - rotationSymZ >= this.zSize || rZ - rotationSymZ < 0) {
                    height = 0;
                } else {
                    height = this.mapHeight[(rX - rotationSymX) + (rZ - rotationSymZ) * this.xSize];
                }

                var extra = MapColor.Brightness.LOWEST.id;

                if (y == height) {
                    extra = MapColor.Brightness.NORMAL.id;
                } else if (y > height) {
                    extra = MapColor.Brightness.HIGH.id;
                } else if (height - y == 1) {
                    extra = MapColor.Brightness.LOW.id;
                }

                bytes[tX + tZ * 128] = (byte) (this.mapData[index] + extra);
            }
        }

        for (var monument : this.map.monuments) {
            int mX = rotationSymX * (monument.pos.getX() - playerX) * 2 - 1;
            int mZ = rotationSymZ * (monument.pos.getZ() - playerZ) * 2 - 1;

            if (replaceXZ) {
                int tmp = mX;
                mX = mZ;
                mZ = tmp;
            }

            var isOff = mX >= 128 || mX <= -128 || mZ >= 128 || mZ <= -128;

            if (monument.isAlive()) {
                var type = getDecorationType(monument.teamData.getConfig().blockDyeColor());
                var text = isOff ? null : monument.getName();
                icons.add(new MapDecoration(type,
                        (byte) Mth.clamp(mX, -127, 127 ), (byte) Mth.clamp(mZ, -127, 127 ), (byte) 8, Optional.ofNullable(text)));

            } else if (!isOff) {
                icons.add(new MapDecoration(MapDecorationTypes.RED_X, (byte) mX, (byte) mZ, (byte) 8,Optional.empty()));
            }
        }

        for (TeamData data : logic.teams) {
            if (playerData == null || playerData.teamData == data) {
                for (ServerPlayer entity : logic.teams.getManager().playersIn(data.team)) {
                    if (entity == player) {
                        continue;
                    }

                    int mX = rotationSymX * (entity.getBlockX() - playerX) * 2 - 1;
                    int mZ = rotationSymZ * (entity.getBlockZ() - playerZ) * 2 - 1;

                    if (replaceXZ) {
                        int tmp = mX;
                        mX = mZ;
                        mZ = tmp;
                    }

                    if (mX >= 128 || mX <= -128 || mZ >= 128 || mZ < -128) {
                        continue;
                    }

                    icons.add(new MapDecoration(MapDecorationTypes.BLUE_MARKER, (byte) mX, (byte) mZ, (byte) Math.round((entity.getYRot() + rotationEntity) / 360 * 16), Optional.ofNullable(entity.getDisplayName())));
                }
            }

            icons.add(new MapDecoration(MapDecorationTypes.PLAYER, (byte) 0, (byte) 0, (byte) Math.round((player.getYRot() + rotationEntity) / 360 * 16), Optional.empty()));
        }
        player.connection.send(new ClientboundMapItemDataPacket(new MapId(0), (byte) 0, false, icons, new MapItemSavedData.MapPatch(0, 0, 128, 128, bytes)));
    }

    public void tick() {
        int nextPosX = Math.min(this.currentPosX + 48, this.map.mapBounds.max().getX());
        int nextPosZ = Math.min(this.currentPosZ + 48, this.map.mapBounds.max().getZ());

        this.renderWorld(this.currentPosX, this.currentPosZ, nextPosX, nextPosZ);

        this.currentPosX += 48;

        if (this.currentPosX >= this.map.mapBounds.max().getX()) {
            this.currentPosX = this.map.mapBounds.min().getX();
            this.currentPosZ = nextPosZ;
        }

        if (this.currentPosZ >= this.map.mapBounds.max().getZ()) {
            this.currentPosX = this.map.mapBounds.min().getX();
            this.currentPosZ = this.map.mapBounds.min().getZ();
        }
    }

    private static Holder<MapDecorationType> getDecorationType(DyeColor color) {
        return switch (color) {
            case WHITE -> MapDecorationTypes.WHITE_BANNER;
            case ORANGE -> MapDecorationTypes.ORANGE_BANNER;
            case MAGENTA -> MapDecorationTypes.MAGENTA_BANNER;
            case LIGHT_BLUE -> MapDecorationTypes.LIGHT_BLUE_BANNER;
            case YELLOW -> MapDecorationTypes.YELLOW_BANNER;
            case LIME -> MapDecorationTypes.LIME_BANNER;
            case PINK -> MapDecorationTypes.PINK_BANNER;
            case GRAY -> MapDecorationTypes.GRAY_BANNER;
            case LIGHT_GRAY -> MapDecorationTypes.LIGHT_GRAY_BANNER;
            case CYAN -> MapDecorationTypes.CYAN_BANNER;
            case PURPLE -> MapDecorationTypes.PURPLE_BANNER;
            case BLUE -> MapDecorationTypes.BLUE_BANNER;
            case BROWN -> MapDecorationTypes.BROWN_BANNER;
            case GREEN -> MapDecorationTypes.GREEN_BANNER;
            case RED -> MapDecorationTypes.RED_BANNER;
            case BLACK -> MapDecorationTypes.BLACK_BANNER;
        };
    }
}
