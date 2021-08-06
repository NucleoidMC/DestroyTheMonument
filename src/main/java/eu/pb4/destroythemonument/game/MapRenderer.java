package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.map.GameMap;
import net.minecraft.block.MapColor;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MapRenderer {
    private final BaseGameLogic logic;
    private final GameMap map;
    private final byte[] mapData;
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

        int chunkX = Integer.MAX_VALUE;
        int chunkZ = Integer.MAX_VALUE;
        WorldChunk chunk = null;

        for (int x = fromX; x < toX; x++) {
            int iX = x - min.getX();
            int tmpChunkX = ChunkSectionPos.getSectionCoord(x);

            for (int z = fromZ; z < toZ; z++) {
                int iZ = z - min.getZ();

                int tmpChunkZ = ChunkSectionPos.getSectionCoord(z);

                if (chunkX != tmpChunkX || chunkZ != tmpChunkZ) {
                    chunkX = tmpChunkX;
                    chunkZ = tmpChunkZ;
                    chunk = (WorldChunk) world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);

                    if (chunk != null) {
                        int count = 0;
                        for (var section : chunk.getSectionArray()) {
                            if (section == null || section.isEmpty()) {
                                count++;
                            }
                        }
                        if (count == chunk.getSectionArray().length) {
                            chunk = null;
                        }
                    }
                }

                int index = iX + 1 + (iZ + 1) * this.xSize;

                if (chunk != null) {
                    int y = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x & 15, z & 15) - 1;

                    if (this.map.mapBounds.contains(x, y, z)) {
                        var blockState = chunk.getBlockState(pos.set(x, y, z));
                        int color = blockState.getMapColor(world, pos).id;
                        this.mapData[index] = (byte) (color == 0 ? 0 : color * 4 + (y % 3));
                    } else {
                        this.mapData[index] = (byte) 0;
                    }
                } else {
                    this.mapData[index] = (byte) 0;
                }
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
        List<MapIcon> icons = new ArrayList<>();
        var bytes = new byte[128 * 128];

        int rotationAddX = 0;
        int rotationAddZ = 0;
        int rotationSymX = 1;
        int rotationSymZ = 1;

        int rotationEntity = 0;

        boolean replaceXZ = false;

        if (playerData != null) {
            TeamData teamData = logic.teams.teamData.get(playerData.team);

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

                int rX = playerX + x - 64 + this.halfXSize;
                int rZ = playerZ + z - 64 + this.halfZSize;

                if (rX >= this.xSize || rX < 0 || rZ >= this.zSize || rZ < 0) {
                    continue;
                }

                bytes[tX + tZ * 128] = this.mapData[rX + rZ * this.xSize];

            }
        }

        for (TeamData data : logic.teams.teamData.values()) {
            MapIcon.Type type = MapIcon.Type.byId((byte) (data.team.blockDyeColor().getId() + 10));

            for (BlockPos monument : data.monuments) {
                int mX = rotationSymX * (monument.getX() - playerX) * 2 - 1;
                int mZ = rotationSymZ * (monument.getZ() - playerZ) * 2 - 1;

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

                    icons.add(new MapIcon(MapIcon.Type.BLUE_MARKER, (byte) mX, (byte) mZ, (byte) Math.round((entity.getYaw() + rotationEntity) / 360 * 16), entity.getDisplayName()));
                }
            }

            icons.add(new MapIcon(MapIcon.Type.PLAYER, (byte) 0, (byte) 0, (byte) Math.round((player.getYaw() + rotationEntity) / 360 * 16), null));
        }
        player.networkHandler.sendPacket(new MapUpdateS2CPacket(0, (byte) 0, false, icons, new MapState.UpdateData(0, 0, 128, 128, bytes)));
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
}
