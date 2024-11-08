package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.map.GameMap;
import net.minecraft.block.MapColor;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        var pos = new BlockPos.Mutable();

        var min = this.map.mapBounds.min();

        for (int x = fromX; x < toX; x++) {
            int iX = x - min.getX();

            for (int z = fromZ; z < toZ; z++) {
                int iZ = z - min.getZ();


                int index = iX + 1 + (iZ + 1) * this.xSize;

                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;

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
            byte val = (byte) (MapColor.GRAY.id * 4 + (x % 2) * 2);
            this.mapData[x] = val;
            this.mapData[x + (this.zSize - 1) * this.xSize] = val;
        }

        for (int z = 0; z < this.zSize; z++) {
            byte val = (byte) (MapColor.GRAY.id * 4 + (z % 2) * 2);
            this.mapData[z * this.xSize] = val;
            this.mapData[this.xSize - 1 + z * this.xSize] = val;
        }
    }

    public void updateMap(ServerPlayerEntity player, @Nullable PlayerData playerData) {
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

        int playerX = player.getBlockPos().getX();
        int playerZ = player.getBlockPos().getZ();

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

                int rX = playerX + x - 64 + this.halfXSize - (int) this.map.mapBounds.center().getX();
                int rZ = playerZ + z - 64 + this.halfZSize - (int) this.map.mapBounds.center().getZ();

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
                        (byte) MathHelper.clamp(mX, -127, 127 ), (byte) MathHelper.clamp(mZ, -127, 127 ), (byte) 8, Optional.ofNullable(text)));

            } else if (!isOff) {
                icons.add(new MapDecoration(MapDecorationTypes.RED_X, (byte) mX, (byte) mZ, (byte) 8,Optional.empty()));
            }
        }

        for (TeamData data : logic.teams) {
            if (playerData == null || playerData.teamData == data) {
                for (ServerPlayerEntity entity : logic.teams.getManager().playersIn(data.team)) {
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

                    icons.add(new MapDecoration(MapDecorationTypes.BLUE_MARKER, (byte) mX, (byte) mZ, (byte) Math.round((entity.getYaw() + rotationEntity) / 360 * 16), Optional.ofNullable(entity.getDisplayName())));
                }
            }

            icons.add(new MapDecoration(MapDecorationTypes.PLAYER, (byte) 0, (byte) 0, (byte) Math.round((player.getYaw() + rotationEntity) / 360 * 16), Optional.empty()));
        }
        player.networkHandler.sendPacket(new MapUpdateS2CPacket(new MapIdComponent(0), (byte) 0, false, icons, new MapState.UpdateData(0, 0, 128, 128, bytes)));
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

    private static RegistryEntry<MapDecorationType> getDecorationType(DyeColor color) {
        return switch (color) {
            case WHITE -> MapDecorationTypes.BANNER_WHITE;
            case ORANGE -> MapDecorationTypes.BANNER_ORANGE;
            case MAGENTA -> MapDecorationTypes.BANNER_MAGENTA;
            case LIGHT_BLUE -> MapDecorationTypes.BANNER_LIGHT_BLUE;
            case YELLOW -> MapDecorationTypes.BANNER_YELLOW;
            case LIME -> MapDecorationTypes.BANNER_LIME;
            case PINK -> MapDecorationTypes.BANNER_PINK;
            case GRAY -> MapDecorationTypes.BANNER_GRAY;
            case LIGHT_GRAY -> MapDecorationTypes.BANNER_LIGHT_GRAY;
            case CYAN -> MapDecorationTypes.BANNER_CYAN;
            case PURPLE -> MapDecorationTypes.BANNER_PURPLE;
            case BLUE -> MapDecorationTypes.BANNER_BLUE;
            case BROWN -> MapDecorationTypes.BANNER_BROWN;
            case GREEN -> MapDecorationTypes.BANNER_GREEN;
            case RED -> MapDecorationTypes.BANNER_RED;
            case BLACK -> MapDecorationTypes.BANNER_BLACK;
        };
    }
}
