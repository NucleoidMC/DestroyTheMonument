package eu.pb4.destroythemonument.entities;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.Optional;


public class DtmTntEntity extends Entity implements PolymerEntity {
    public int fuse = 80;
    private boolean hitBlock = false;
    private final boolean hitEntity = true;

    @Nullable
    private GameTeamKey team;
    @Nullable
    public LivingEntity causingEntity;

    public DtmTntEntity(EntityType<DtmTntEntity> entityType, Level world) {
        super(entityType, world);
    }

    public static void createThrown(LivingEntity player) {
        var tnt = new DtmTntEntity(DtmEntities.TNT, player.level());
        tnt.causingEntity = player;
        tnt.hitBlock = true;
        tnt.fuse = 40;

        var game = DtmUtil.getGame(player);
        if (game != null && player instanceof ServerPlayer serverPlayerEntity) {
            var pData = game.participants.get(PlayerRef.of(serverPlayerEntity));

            if (pData != null) {
                tnt.team = pData.teamData.team;
            }
        }

        double pitchRad = Math.toRadians(-player.getXRot());
        double yawRad = Math.toRadians(player.getYRot() - 180);

        double horizontal = Math.cos(pitchRad);
        tnt.setDeltaMovement(new Vec3(
                Math.sin(yawRad) * horizontal,
                Math.sin(pitchRad),
                -Math.cos(yawRad) * horizontal
        ).scale(0.8));
        tnt.setPos(player.getX() + Math.sin(yawRad) * horizontal * 0.3, player.getEyeY() + Math.sin(pitchRad) * 0.3, player.getZ() + -Math.cos(yawRad) * horizontal * 0.3);

        player.level().addFreshEntity(tnt);
    }

    public static boolean createPlaced(LivingEntity player, BlockPos pos) {
        var tnt = new DtmTntEntity(DtmEntities.TNT, player.level());
        tnt.setPos(Vec3.atBottomCenterOf(pos));
        tnt.causingEntity = player;
        tnt.fuse = 20;

        var game = DtmUtil.getGame(player);
        if (game != null && player instanceof ServerPlayer serverPlayerEntity) {
            var pData = game.participants.get(PlayerRef.of(serverPlayerEntity));

            if (pData != null) {
                tnt.team = pData.teamData.team;
            }
        }

        player.level().addFreshEntity(tnt);
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        double pitchRad = Math.toRadians(-attacker.getXRot());
        double yawRad = Math.toRadians(attacker.getYRot() - 180);
        double horizontal = Math.cos(pitchRad);

        this.setDeltaMovement(this.getDeltaMovement().add(new Vec3(
                Math.sin(yawRad) * horizontal,
                Math.sin(pitchRad),
                -Math.cos(yawRad) * horizontal
        ).scale(attacker == this.causingEntity ? 0.12 : 0.15)));

        this.fuse += 5;

        return false;
    }

    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        return false;
    }

    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    public void tick() {
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }

        if (this.tickCount > 1 && this.hitBlock) {
            var bb = this.getBoundingBox().expandTowards(this.getDeltaMovement());

            for (var blockPos : BlockPos.withinManhattan(this.blockPosition(), 1, 1, 1)) {
                BlockState blockState = this.level().getBlockState(blockPos);

                if (!blockState.isAir()) {
                    var voxelShape = blockState.getCollisionShape(this.level(), blockPos);
                    if (!voxelShape.isEmpty()) {
                        for (var box : voxelShape.toAabbs()) {
                            if (box.move(blockPos).intersects(bb)) {
                                this.onBlockHit();
                                return;
                            }
                        }
                    }
                }
            }
        }


        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
        }

        int i = this.fuse - 1;
        this.fuse = i;

        if (i <= 0) {
            this.explode();
            return;
        } else {
            //this.updateInWaterStateAndDoFluidPushing();
        }

        if (this.tickCount > 1 && this.hitEntity) {
            for (var entity : this.level().getEntities(this, this.getBoundingBox())) {
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
        } else if (entity instanceof Projectile projectile) {
            entity = projectile.getOwner();
        }

        if (entity instanceof Player player) {
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
        this.level().explode(this, this.damageSources().explosion(this, this.causingEntity), new CustomExplosionBehaviour(this), this.getBoundingBox().getCenter(), 2.8f, false, Level.ExplosionInteraction.TNT);
    }

    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.15F;
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityTypes.TNT;
    }


    public static class CustomExplosionBehaviour extends EntityBasedExplosionDamageCalculator {
        private final DtmTntEntity entity;

        public CustomExplosionBehaviour(DtmTntEntity entity) {
            super(entity);
            this.entity = entity;
        }

        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter world, BlockPos pos, BlockState blockState, FluidState fluidState) {
            var out = super.getBlockExplosionResistance(explosion, world, pos, blockState, fluidState);
            return out.map(x -> blockState.is(DTM.BUILDING_BLOCKS) ? 0.8f : Math.max(2.8f, x));
        }

        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return 1.4f;
        }

        @Override
        public float getEntityDamageAmount(Explosion explosion, Entity entity, float amount) {
            double x = entity.getX() - explosion.center().x;
            double y = (entity instanceof DtmTntEntity ? entity.getY() : entity.getEyeY()) - explosion.center().y;
            double z = entity.getZ() - explosion.center().z;
            double distance = Math.sqrt(x * x + y * y + z * z);

            float doublePower = explosion.radius() * 2.0F;
            double w = Math.sqrt(entity.distanceToSqr(explosion.center())) / (double) doublePower;

            if (distance != 0.0D) {
                double ac = (1.0D - w) * amount;
                return (float) ((int) ((ac * ac + ac) / 2.0D * 3.2D * (double) doublePower + 1.0D) * (this.entity.causingEntity == entity ? 0.25 : 1));
            }

            return 0;
        }
    }


    /*public static class CustomExplosion extends Explosion {
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
    }*/
}
