package eu.pb4.destroythemonument.entities;

import com.google.common.collect.Sets;
import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.EntityExplosionBehavior;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public class DtmTntEntity extends Entity implements PolymerEntity {
    public int fuse = 80;
    private boolean hitBlock = false;
    private final boolean hitEntity = true;

    @Nullable
    private GameTeamKey team;
    @Nullable
    public LivingEntity causingEntity;

    public DtmTntEntity(EntityType<DtmTntEntity> entityType, World world) {
        super(entityType, world);
    }

    public static void createThrown(LivingEntity player) {
        var tnt = new DtmTntEntity(DtmEntities.TNT, player.getWorld());
        tnt.causingEntity = player;
        tnt.hitBlock = true;
        tnt.fuse = 40;

        var game = DtmUtil.getGame(player);
        if (game != null && player instanceof ServerPlayerEntity serverPlayerEntity) {
            var pData = game.participants.get(PlayerRef.of(serverPlayerEntity));

            if (pData != null) {
                tnt.team = pData.teamData.team;
            }
        }

        double pitchRad = Math.toRadians(-player.getPitch());
        double yawRad = Math.toRadians(player.getYaw() - 180);

        double horizontal = Math.cos(pitchRad);
        tnt.setVelocity(new Vec3d(
                Math.sin(yawRad) * horizontal,
                Math.sin(pitchRad),
                -Math.cos(yawRad) * horizontal
        ).multiply(0.8));
        tnt.setPosition(player.getX() + Math.sin(yawRad) * horizontal * 0.3, player.getEyeY() + Math.sin(pitchRad) * 0.3, player.getZ() + -Math.cos(yawRad) * horizontal * 0.3);

        player.getWorld().spawnEntity(tnt);
    }

    public static boolean createPlaced(LivingEntity player, BlockPos pos) {
        var tnt = new DtmTntEntity(DtmEntities.TNT, player.getWorld());
        tnt.setPosition(Vec3d.ofBottomCenter(pos));
        tnt.causingEntity = player;
        tnt.fuse = 20;

        var game = DtmUtil.getGame(player);
        if (game != null && player instanceof ServerPlayerEntity serverPlayerEntity) {
            var pData = game.participants.get(PlayerRef.of(serverPlayerEntity));

            if (pData != null) {
                tnt.team = pData.teamData.team;
            }
        }

        player.getWorld().spawnEntity(tnt);
        return true;
    }

    @Override
    public boolean handleAttack(Entity attacker) {
        double pitchRad = Math.toRadians(-attacker.getPitch());
        double yawRad = Math.toRadians(attacker.getYaw() - 180);
        double horizontal = Math.cos(pitchRad);

        this.setVelocity(this.getVelocity().add(new Vec3d(
                Math.sin(yawRad) * horizontal,
                Math.sin(pitchRad),
                -Math.cos(yawRad) * horizontal
        ).multiply(attacker == this.causingEntity ? 0.12 : 0.15)));

        this.fuse += 5;

        return false;
    }

    protected MoveEffect getMoveEffect() {
        return MoveEffect.NONE;
    }

    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {

    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {

    }

    @Override
    protected void initDataTracker() {

    }

    public void tick() {
        if (!this.hasNoGravity()) {
            this.setVelocity(this.getVelocity().add(0.0D, -0.04D, 0.0D));
        }

        if (this.age > 1 && this.hitBlock) {
            var bb = this.getBoundingBox().stretch(this.getVelocity());

            for (var blockPos : BlockPos.iterateOutwards(this.getBlockPos(), 1, 1, 1)) {
                BlockState blockState = this.getWorld().getBlockState(blockPos);

                if (!blockState.isAir()) {
                    var voxelShape = blockState.getCollisionShape(this.getWorld(), blockPos);
                    if (!voxelShape.isEmpty()) {
                        for (var box : voxelShape.getBoundingBoxes()) {
                            if (box.offset(blockPos).intersects(bb)) {
                                this.onBlockHit();
                                return;
                            }
                        }
                    }
                }
            }
        }


        this.move(MovementType.SELF, this.getVelocity());
        this.setVelocity(this.getVelocity().multiply(0.98D));
        if (this.isOnGround()) {
            this.setVelocity(this.getVelocity().multiply(0.7D, -0.5D, 0.7D));
        }

        int i = this.fuse - 1;
        this.fuse = i;

        if (i <= 0) {
            this.explode();
            return;
        } else {
            this.updateWaterState();
        }

        if (this.age > 1 && this.hitEntity) {
            for (var entity : this.getWorld().getOtherEntities(this, this.getBoundingBox())) {
                this.onEntityHit(entity);
                return;
            }
        }
    }

    public void onEntityHit(Entity entity) {
        if (this.causingEntity == entity) {
            return;
        }

        if (this.team == null) {
            this.explode();
            return;
        } else if (entity instanceof ProjectileEntity projectile) {
            entity = projectile.getOwner();
        }

        if (entity instanceof PlayerEntity player) {
            var game = DtmUtil.getGame(player);

            if (game != null) {
                var data = game.participants.get(PlayerRef.of(player));

                if (data != null && data.teamData.team != this.team) {
                    this.explode();
                }
            }

        }
    }

    protected void onBlockHit() {
        if (!this.isRemoved()) {
            this.explode();
        }
    }

    private void explode() {
        this.discard();

        var explosion = new CustomExplosion(this.getWorld(), this, this.getWorld().getDamageSources().explosion(this, this.causingEntity), new EntityExplosionBehavior(this), this.getX(), this.getBodyY(0.0625D), this.getZ(), 2.8f, false, Explosion.DestructionType.DESTROY);
        explosion.collectBlocksAndDamageEntities();
        explosion.affectWorld(true);

        if (!explosion.shouldDestroy()) {
            explosion.clearAffectedBlocks();
        }


        for (var player : this.getWorld().getPlayers()) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)player;
            if (serverPlayerEntity.squaredDistanceTo(this.getX(), this.getBodyY(0.0625D), this.getZ()) < 4096.0D) {
                serverPlayerEntity.networkHandler.sendPacket(new ExplosionS2CPacket(this.getX(), this.getBodyY(0.0625D), this.getZ(), 3, explosion.getAffectedBlocks(), explosion.getAffectedPlayers().get(serverPlayerEntity), explosion.getDestructionType(), explosion.getEmitterParticle(), explosion.getParticle(), explosion.getSoundEvent()));
            }
        }
    }

    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.15F;
    }

    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.TNT;
    }


    public static class CustomExplosion extends Explosion {
        private final World world;
        private final Entity entity;
        private final double z;
        private final double x;
        private final double y;
        private final float power;
        private final ExplosionBehavior behavior;
        private final DamageSource damageSource;

        public CustomExplosion(World world, @Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, DestructionType destructionType) {
            super(world, entity, damageSource, behavior, x, y, z, power, createFire, destructionType, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.ENTITY_GENERIC_EXPLODE);
            this.world = world;
            this.entity = entity;
            this.x = x;
            this.y = y;
            this.z = z;
            this.power = power;
            this.behavior = behavior;
            this.damageSource = damageSource;
        }

        @Override
        public void collectBlocksAndDamageEntities() {
            this.world.emitGameEvent(this.entity, GameEvent.EXPLODE, new Vec3d(this.x, this.y, this.z));
            Set<BlockPos> set = Sets.newHashSet();
            boolean i = true;

            int k;
            int l;
            for (int j = 0; j < 16; ++j) {
                for (k = 0; k < 16; ++k) {
                    for (l = 0; l < 16; ++l) {
                        if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                            double d = (float) j / 15.0F * 2.0F - 1.0F;
                            double e = (float) k / 15.0F * 2.0F - 1.0F;
                            double f = (float) l / 15.0F * 2.0F - 1.0F;
                            double g = Math.sqrt(d * d + e * e + f * f);
                            d /= g;
                            e /= g;
                            f /= g;
                            float h = this.power * (0.7F + this.world.random.nextFloat() * 0.6F);
                            double m = this.x;
                            double n = this.y;
                            double o = this.z;

                            for (float var21 = 0.3F; h > 0.0F; h -= 0.22500001F) {
                                BlockPos blockPos = BlockPos.ofFloored(m, n, o);
                                BlockState blockState = this.world.getBlockState(blockPos);
                                FluidState fluidState = this.world.getFluidState(blockPos);
                                if (!this.world.isInBuildLimit(blockPos)) {
                                    break;
                                }

                                Optional<Float> optional = this.behavior.getBlastResistance(this, this.world, blockPos, blockState, fluidState);

                                if (optional.isPresent()) {
                                    h -= (blockState.isIn(DTM.BUILDING_BLOCKS) ? 0.8 : Math.max(2.6f, optional.get()));
                                }

                                if (h > 0.0F && this.behavior.canDestroyBlock(this, this.world, blockPos, blockState, h)) {
                                    set.add(blockPos);
                                }

                                m += d * 0.30000001192092896D;
                                n += e * 0.30000001192092896D;
                                o += f * 0.30000001192092896D;
                            }
                        }
                    }
                }
            }

            this.getAffectedBlocks().addAll(set);
            float q = this.power * 2.0F;
            k = MathHelper.floor(this.x - (double) q - 1.0D);
            l = MathHelper.floor(this.x + (double) q + 1.0D);
            int r = MathHelper.floor(this.y - (double) q - 1.0D);
            int s = MathHelper.floor(this.y + (double) q + 1.0D);
            int t = MathHelper.floor(this.z - (double) q - 1.0D);
            int u = MathHelper.floor(this.z + (double) q + 1.0D);
            List<Entity> list = this.world.getOtherEntities(this.entity, new Box(k, r, t, l, s, u));
            Vec3d vec3d = new Vec3d(this.x, this.y, this.z);
            for (int v = 0; v < list.size(); ++v) {
                Entity entity = list.get(v);
                if (!entity.isImmuneToExplosion(this)) {
                    double w = Math.sqrt(entity.squaredDistanceTo(vec3d)) / (double) q;
                    if (w <= 1.0D) {
                        double x = entity.getX() - this.x;
                        double y = (entity instanceof DtmTntEntity ? entity.getY() : entity.getEyeY()) - this.y;
                        double z = entity.getZ() - this.z;
                        double aa = Math.sqrt(x * x + y * y + z * z);
                        if (aa != 0.0D) {
                            x /= aa;
                            y /= aa;
                            z /= aa;
                            double ab = getExposure(vec3d, entity);
                            double ac = (1.0D - w) * ab;
                            var damaged = entity.damage(damageSource, (float) ((int) ((ac * ac + ac) / 2.0D * 3.2D * (double) q + 1.0D) * (damageSource.getAttacker() == entity ? 0.25 : 1)));
                            if (damaged) {
                                double ad = ac * 1.4;

                                entity.setVelocity(entity.getVelocity().add(x * ad, y * ad, z * ad));
                                if (entity instanceof PlayerEntity) {
                                    PlayerEntity playerEntity = (PlayerEntity) entity;
                                    if (!playerEntity.isSpectator() && (!playerEntity.isCreative() || !playerEntity.getAbilities().flying)) {
                                        this.getAffectedPlayers().put(playerEntity, new Vec3d(x * ac, y * ac, z * ac));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
