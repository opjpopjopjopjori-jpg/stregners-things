package com.riftcompanions.entity;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.formation.FormationType;
import com.riftcompanions.resource.CompanionInventory;
import com.riftcompanions.relationship.RelationService;
import com.riftcompanions.registry.ModTags;
import com.riftcompanions.network.ModNetwork;
import com.riftcompanions.network.S2COpenConversationPacket;
import com.riftcompanions.server.SafeTeleport;
import com.riftcompanions.server.RescueService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.server.BaseMilestoneService;
import com.riftcompanions.server.CompanionPresentationSoundService;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.persistence.SaveMigrationService;
import com.riftcompanions.persistence.SaveVersions;
import com.riftcompanions.safety.CompanionFaultService;
import com.riftcompanions.navigation.CompanionNavigationService;
import com.riftcompanions.navigation.NavigationIntent;
import com.riftcompanions.navigation.NavigationOutcome;
import com.riftcompanions.navigation.NavigationResult;
import com.riftcompanions.navigation.FormationSlotReservationService;
import com.riftcompanions.performance.CompanionPerformanceMonitor;
import com.riftcompanions.performance.PerformanceWorkType;
import com.riftcompanions.presentation.CompanionGaitPresentationService;
import com.riftcompanions.animation.CompanionAnimationController;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumMap;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared, server-authoritative base for the four companions.
 *
 * This class deliberately owns only personal movement/state/energy.  Cross-team
 * decisions live in TeamSavedData and services, which prevents four entities
 * from each persisting conflicting copies of a plan.
 */
public abstract class CompanionEntity extends Mob implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> COMPANION_STATE = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VISUAL_ACTION = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LOOK_INTENT = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ENERGY = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HIVE_STRAIN = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FOCUS = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> COMBAT_ENABLED = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SHIELD_TICKS = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RESCUE_PROGRESS = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DOWNED_STATUS = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);

    private static final long SAFE_SAMPLE_INTERVAL = 40L;
    private static final long STUCK_RECALL_TIMEOUT = 160L;
    private static final long SCOUT_TIMEOUT = 240L;

    private final CompanionRole role;
    private final CompanionInventory personalInventory = new CompanionInventory();
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final EnumMap<CompanionAbility, Long> cooldowns = new EnumMap<>(CompanionAbility.class);

    private BlockPos lastSafeWaypoint;
    private BlockPos scoutDestination;
    private BlockPos returnHomeDestination;
    private BlockPos baseActivityTarget;
    private BlockPos formationTarget;
    private FormationType assignedFormation = FormationType.FOLLOW;
    private long formationExpiresAt;
    private long stateEnteredAt;
    private long separationStartedAt = -1L;
    private long actionExpiresAt;
    private long recoveryUntil;
    private long scheduledRecoveryAt;
    private long scheduledRecoveryDuration;
    private String scheduledRecoveryReason = "";
    private long lastSafeSampleAt;
    private long downedSince;
    private UUID rescuerUuid;
    private int failedPathAttempts;
    private String lastReasonCode = "INITIALIZED";
    private boolean saveRecoveryRequired;
    private String saveRecoveryReason = "";

    protected CompanionEntity(final EntityType<? extends CompanionEntity> type, final Level level, final CompanionRole role) {
        super(type, level);
        this.role = role;
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createCompanionAttributes(final double health, final double speed, final double attackDamage) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, attackDamage)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(COMPANION_STATE, CompanionState.IDLE.ordinal());
        this.entityData.define(VISUAL_ACTION, CompanionAction.NONE.ordinal());
        this.entityData.define(LOOK_INTENT, CompanionLookIntent.FORWARD.ordinal());
        this.entityData.define(ENERGY, 100.0F);
        this.entityData.define(HIVE_STRAIN, 0.0F);
        this.entityData.define(FOCUS, 100.0F);
        this.entityData.define(COMBAT_ENABLED, true);
        this.entityData.define(SHIELD_TICKS, 0);
        this.entityData.define(RESCUE_PROGRESS, 0);
        this.entityData.define(DOWNED_STATUS, DownedStatus.STABLE.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new com.riftcompanions.entity.ai.CompanionMeleeGoal(this, 1.05D));
        // RandomLookAroundGoal / LookAtPlayerGoal are intentionally omitted. They can
        // spin the full entity while it follows a path; CompanionGaitPresentationService
        // instead stabilizes body heading and drives a bounded head-only glance intent.
        // Mob is not a PathfinderMob in the official 1.20.1 API, so the
        // PathfinderMob-only HurtByTargetGoal is intentionally not used here.
        // Visible Monster targeting remains bounded by mayFight and combat policy.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, this::mayFight));
    }

    @Override
    public boolean removeWhenFarAway(final double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        // Dimensions are deliberately opt-in in a later milestone. The player
        // must travel first; an unsafe auto-portal is never allowed here.
        return false;
    }

    @Override
    public boolean isPushable() {
        // Collision is still present, but a reduced push response avoids the
        // classic NPC problem of shoving a player off a ledge.
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            final long performanceStartedAt = CompanionPerformanceMonitor.start();
            try {
                serverBrainTick();
            } catch (final RuntimeException exception) {
                // A companion-side fault becomes a bounded circuit-breaker event
                // instead of repeatedly crashing or silently continuing AI.
                CompanionFaultService.recordException(this, exception);
            } finally {
                CompanionPerformanceMonitor.record(getOwnerUuid().orElse(null), PerformanceWorkType.COMPANION_AI, performanceStartedAt);
            }
        }
    }

    private void serverBrainTick() {
        final long now = this.level().getGameTime();
        expireVisualAction(now);
        if (scheduledRecoveryAt > 0L && now >= scheduledRecoveryAt) {
            long duration = scheduledRecoveryDuration;
            String reason = scheduledRecoveryReason;
            scheduledRecoveryAt = 0L;
            scheduledRecoveryDuration = 0L;
            scheduledRecoveryReason = "";
            beginRecovery(duration, reason);
        }
        regenerateEnergy(now);

        if (getShieldTicks() > 0) {
            if (this.role == CompanionRole.GIFTED && (!CompanionConfig.POWERS_ENABLED.get() || !FeatureFlags.enabled(FeatureFlag.ELEVEN_POWERS))) {
                disableActiveProtection("POWER_ACCESS_CHANGED");
            } else {
                final int remainingShield = getShieldTicks() - 1;
                this.entityData.set(SHIELD_TICKS, remainingShield);
                if (remainingShield == 0 && this.role == CompanionRole.GIFTED) {
                    DialogueService.get().speak(this, "shield_end", 2);
                }
            }
        }

        if (getCompanionState() == CompanionState.DOWNED) {
            setLookIntent(CompanionLookIntent.FORWARD);
            this.getNavigation().stop();
            this.setTarget(null);
            tickDownedRecovery(now);
            return;
        }

        final Optional<ServerPlayer> ownerOptional = getOwnerPlayer();
        if (ownerOptional.isEmpty()) {
            setLookIntent(CompanionLookIntent.FORWARD);
            this.getNavigation().stop();
            return;
        }
        final ServerPlayer owner = ownerOptional.get();
        if (!owner.server.isSingleplayer()) {
            // This source edition is intentionally offline/single-player only.
            // If a companion somehow exists on another server type, isolate it
            // instead of attempting unsupported ownership or autonomous work.
            this.getNavigation().stop();
            this.setTarget(null);
            this.setCombatEnabled(false);
            if (getCompanionState() != CompanionState.DOWNED) {
                setCompanionState(CompanionState.HOLDING, "SINGLEPLAYER_ONLY_MODE");
            }
            return;
        }

        if (this.role == CompanionRole.SCOUT && getCompanionState() == CompanionState.SCOUTING
                && !FeatureFlags.enabled(FeatureFlag.MAX_SCOUT)) {
            cancelScout("SCOUT_FEATURE_FLAG_DISABLED");
        }
        if (owner.level() != this.level()) {
            setCompanionState(CompanionState.RESTING, "OWNER_CHANGED_DIMENSION");
            this.getNavigation().stop();
            return;
        }

        // Keep body travel heading stable; any lateral/rear attention becomes a
        // synchronized head-only intent consumed by the face animation layer.
        CompanionGaitPresentationService.tick(this, owner, now);

        // A target acquired by the bounded vanilla target selector must move the
        // companion into a real combat state before follow/formation logic can
        // overwrite its navigation order.
        if (hasActiveCombatTarget() && (getCompanionState() == CompanionState.FOLLOWING
                || getCompanionState() == CompanionState.IDLE || getCompanionState() == CompanionState.OBSERVING)) {
            clearFormationSlot("VISIBLE_HOSTILE_ENGAGED");
            setCompanionState(CompanionState.FIGHTING, "VISIBLE_HOSTILE_ENGAGED");
        }

        if (now >= lastSafeSampleAt + SAFE_SAMPLE_INTERVAL && SafeTeleport.isSafeStanding((ServerLevel) this.level(), this, this.blockPosition())) {
            this.lastSafeWaypoint = this.blockPosition();
            this.lastSafeSampleAt = now;
        }

        switch (getCompanionState()) {
            case FOLLOWING -> tickFollow(owner, now);
            case GUARDING -> tickGuard(owner, now);
            case HOLDING, RESTING -> this.getNavigation().stop();
            case RETREATING -> tickRetreat(owner, now);
            case RETURNING_HOME -> tickReturnHome(owner, now);
            case BASE_ACTIVITY -> tickBaseActivity(owner, now);
            case SCOUTING -> tickScout(owner, now);
            case STUCK_RECOVERY -> tickStuckRecovery(owner, now);
            case RECOVERING, EXHAUSTED, OBSERVING, IDLE, FIGHTING -> tickPassive(owner, now);
            default -> { }
        }

        applyStateTimeout(owner, now);
    }

    /**
     * A rescue is a short, interruptible transaction rather than an instant
     * heal. The rescuer must remain nearby and the area must be reasonably
     * secure; otherwise progress is reset instead of forcing a suicide run.
     */
    private void tickDownedRecovery(final long now) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.rescuerUuid == null) {
            updateDownedStatus(serverLevel);
            if (now - this.downedSince >= CompanionConfig.DOWNED_TIMEOUT_TICKS.get()) {
                getOwnerPlayer().ifPresent(owner -> {
                    if (SafeTeleport.recallNearPlayer(this, owner, 5)) {
                        reviveFromDowned();
                        this.lastReasonCode = "STORY_DOWNED_TIMEOUT_SAFE_RECOVERY";
                    } else {
                        this.lastReasonCode = "DOWNED_TIMEOUT_NO_SAFE_DESTINATION";
                    }
                });
            }
            return;
        }
        setDownedStatus(DownedStatus.RESCUING);
        final ServerPlayer rescuer = serverLevel.getServer().getPlayerList().getPlayer(this.rescuerUuid);
        if (rescuer == null || rescuer.level() != this.level() || rescuer.distanceToSqr(this) > 4.5D * 4.5D
                || !RescueService.isRescueAreaSafe(rescuer, this)) {
            this.entityData.set(RESCUE_PROGRESS, 0);
            this.rescuerUuid = null;
            getOwnerUuid().ifPresent(owner -> TeamSavedData.get(serverLevel.getServer()).blackboard(owner).releaseRescue(this.getUUID()));
            this.lastReasonCode = "RESCUE_INTERRUPTED_OR_UNSAFE";
            DialogueService.get().speak(this, "rescue_failed", 1);
            return;
        }
        final int progress = Math.min(60, getRescueProgress() + 1);
        this.entityData.set(RESCUE_PROGRESS, progress);
        if (progress < 60) {
            return;
        }
        reviveFromDowned();
        getOwnerUuid().ifPresent(owner -> {
            final TeamSavedData data = TeamSavedData.get(serverLevel.getServer());
            data.blackboard(owner).releaseRescue(this.getUUID());
            data.blackboard(owner).addMemory(new MemoryRecord(MemoryType.TEAM_RESCUE, now / 24000L,
                    this.role.personalName() + " was safely rescued after going down.", 92));
            data.markChanged();
        });
        DialogueService.get().speak(this, "team_rescue", 1);
        RelationService.recordRescue(rescuer, this.role);
        RescueService.recordCompletion(rescuer, this);
        com.riftcompanions.arc.ArcService.observe(rescuer, com.riftcompanions.arc.ArcMilestone.RESCUE_SUCCEEDED);
    }

    private void updateDownedStatus(final ServerLevel level) {
        final boolean immediateDanger = this.isInLava() || this.isOnFire()
                || !level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(3.0D)).isEmpty();
        if (immediateDanger) {
            setDownedStatus(DownedStatus.DANGER);
            return;
        }
        final boolean reachable = SafeTeleport.findSafeSpot(level, this, this.blockPosition(), 2).isPresent();
        setDownedStatus(reachable ? DownedStatus.STABLE : DownedStatus.UNREACHABLE);
    }

    private void tickPassive(final ServerPlayer owner, final long now) {
        if (getCompanionState() == CompanionState.FIGHTING) {
            // CompanionMeleeGoal owns movement toward the confirmed target. A
            // formation path must never overwrite it and make combat look frozen.
            if (hasActiveCombatTarget()) {
                // Role-specific combat intelligence: each companion performs
                // their unique tactical role instead of just standing still.
                tickCombatRole(owner, now);
                return;
            }
            this.setTarget(null);
            setCompanionState(CompanionState.FOLLOWING, "COMBAT_TARGET_CLEARED");
            return;
        }
        if (getCompanionState() == CompanionState.OBSERVING && tickFormationSlot(owner, now)) {
            return;
        }
        if (getCompanionState() == CompanionState.RECOVERING || getCompanionState() == CompanionState.EXHAUSTED) {
            this.getNavigation().stop();
            return;
        }
        // In IDLE/OBSERVING state with no active combat target:
        // Only follow if owner is far; otherwise hold position for potential threats
        if (distanceToSqr(owner) > 14.0D * 14.0D) {
            setCompanionState(CompanionState.FOLLOWING, "OWNER_MOVED_AWAY");
            tickFollow(owner, now);
        }
    }

    /** Uses a Team-assigned safe slot without changing this entity's authoritative state. */
    private boolean tickFormationSlot(final ServerPlayer owner, final long now) {
        if (this.formationTarget == null || now > this.formationExpiresAt) {
            return false;
        }
        if (!this.level().hasChunkAt(this.formationTarget) || this.formationTarget.distSqr(owner.blockPosition()) < 1.6D * 1.6D) {
            clearFormationSlot("FORMATION_SLOT_EXPIRED_OR_UNSAFE");
            return false;
        }
        if (this.blockPosition().distSqr(this.formationTarget) <= 2.25D) {
            this.getNavigation().stop();
            return true;
        }
        requestPath(this.formationTarget, movementSpeedForState(), now);
        return true;
    }

    private void tickFollow(final ServerPlayer owner, final long now) {
        if (hasActiveCombatTarget()) {
            setCompanionState(CompanionState.FIGHTING, "FOLLOW_INTERRUPTED_BY_VISIBLE_HOSTILE");
            return;
        }
        if (tickFormationSlot(owner, now)) {
            return;
        }
        final double distanceSq = distanceToSqr(owner);
        if (distanceSq <= 3.2D * 3.2D) {
            this.getNavigation().stop();
            this.separationStartedAt = -1L;
            return;
        }
        if (distanceSq > 46.0D * 46.0D) {
            if (this.separationStartedAt < 0L) {
                this.separationStartedAt = now;
            }
            setCompanionState(CompanionState.STUCK_RECOVERY, "OWNER_OUT_OF_SAFE_FOLLOW_RANGE");
            return;
        }
        this.separationStartedAt = -1L;
        requestPath(owner.blockPosition(), movementSpeedForState(), now);
    }

    private void tickGuard(final ServerPlayer owner, final long now) {
        final Monster threat = nearestThreat(owner, 14.0D);
        if (threat == null) {
            tickFollow(owner, now);
            return;
        }
        final Vec3 towardThreat = threat.position().subtract(owner.position());
        if (towardThreat.lengthSqr() < 0.001D) {
            return;
        }
        final Vec3 guardPoint = owner.position().add(towardThreat.normalize().scale(2.15D));
        requestPath(BlockPos.containing(guardPoint), movementSpeedForState() + 0.03D, now);
        this.setTarget(threat);
        beginVisualAction(CompanionAction.GUARD, 24);
    }

    /**
     * Role-specific combat intelligence. Each companion has a unique tactical
     * role during combat instead of just passively following the player:
     * <ul>
     *   <li><b>Guardian (Hopper)</b>: Lures mobs AWAY from the player/Eleven.
     *       Moves to intercept threats between them and the owner, then taunts
     *       to pull aggro. If a mob is targeting the owner, Hopper steps into
     *       its path and attacks, creating space for Eleven to use powers.</li>
     *   <li><b>Gifted (Eleven)</b>: Stays at range from the melee target,
     *       maintaining optimal distance for telekinetic push/shield. Retreats
     *       if the target gets within 3 blocks, advances if beyond 8 blocks.</li>
     *   <li><b>Scout (Max)</b>: Flanks the target — circles to the side to
     *       create cross-fire pressure and spot escape routes. Uses speed to
     *       harass from angles the Guardian can't cover.</li>
     *   <li><b>Seer (Will)</b>: Holds position near the owner to maintain
     *       hive sense range. Prioritizes anomaly detection and will redirect
     *       or disrupt hive-linked threats from safety.</li>
     * </ul>
     */
    private void tickCombatRole(final ServerPlayer owner, final long now) {
        final LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        switch (this.role) {
            case GUARDIAN -> tickGuardianCombatRole(owner, target, now);
            case GIFTED   -> tickGiftedCombatRole(owner, target, now);
            case SCOUT     -> tickScoutCombatRole(owner, target, now);
            case SEER      -> tickSeerCombatRole(owner, target, now);
        }
    }

    /**
     * Hopper's Guardian combat role: INTERCEPT and LURE.
     * He positions himself between the threat and the owner, then attacks.
     * If a mob is targeting the owner, Hopper actively moves to intercept
     * its path — pulling aggro away from Eleven and creating tactical space.
     */
    private void tickGuardianCombatRole(final ServerPlayer owner, final LivingEntity target, final long now) {
        // If the target is heading toward the owner, intercept between them
        final double distToOwner = distanceToSqr(owner);
        final double distTargetToOwner = target.distanceToSqr(owner);

        if (distTargetToOwner < distToOwner) {
            // Target is closer to owner than we are — move to intercept
            final Vec3 interceptPoint = owner.position().add(
                    target.position().subtract(owner.position()).normalize().scale(2.5D));
            requestPath(BlockPos.containing(interceptPoint), movementSpeedForState() + 0.04D, now);
            // Taunt: force the target to look at us by attacking
            if (distanceToSqr(target) <= (getBbWidth() * 2.0D + target.getBbWidth()) * (getBbWidth() * 2.0D + target.getBbWidth())) {
                // Already in melee range — CompanionMeleeGoal handles the attack
            } else if (distanceToSqr(target) < 6.0D * 6.0D) {
                // Close enough to close in fast
                getNavigation().moveTo(target, movementSpeedForState() + 0.06D);
            }
        } else {
            // We're between the target and owner — hold position, melee goal handles combat
        }
    }

    /**
     * Eleven's Gifted combat role: RANGE CONTROL.
     * She maintains 5-8 blocks distance from the target for optimal push/shield.
     * Retreats if the target gets too close, advances if too far.
     */
    private void tickGiftedCombatRole(final ServerPlayer owner, final LivingEntity target, final long now) {
        final double distSq = distanceToSqr(target);
        final double minRange = 5.0D;
        final double maxRange = 8.0D;

        if (distSq < minRange * minRange) {
            // Too close! Retreat away from target, toward owner if possible
            final Vec3 retreatDir = position().subtract(target.position()).normalize();
            final Vec3 retreatPoint = position().add(retreatDir.scale(3.0D));
            requestPath(BlockPos.containing(retreatPoint), movementSpeedForState() + 0.06D, now);
        } else if (distSq > maxRange * maxRange) {
            // Too far, advance toward target to get into push range
            getNavigation().moveTo(target, movementSpeedForState());
        }
        // In optimal range — stay put, let melee goal or powers handle it
    }

    /**
     * Max's Scout combat role: FLANKING.
     * She circles to the side of the target (perpendicular to the owner-target line)
     * to create cross-pressure and spot escape routes.
     */
    private void tickScoutCombatRole(final ServerPlayer owner, final LivingEntity target, final long now) {
        // Flank to the right side of the target (relative to owner→target line)
        final Vec3 ownerToTarget = target.position().subtract(owner.position());
        if (ownerToTarget.lengthSqr() < 0.01D) return;
        // Perpendicular direction (right flank)
        final Vec3 flankDir = new Vec3(-ownerToTarget.z, 0, ownerToTarget.x).normalize();
        final Vec3 flankPoint = target.position().add(flankDir.scale(4.0D));
        final double distSq = distanceToSqr(flankPoint);
        if (distSq > 2.25D) {
            requestPath(BlockPos.containing(flankPoint), movementSpeedForState() + 0.03D, now);
        }
    }

    /**
     * Will's Seer combat role: HOLD and SENSE.
     * He stays near the owner (within 4 blocks) to maintain hive sense range,
     * prioritizing anomaly detection and hive disruption from safety.
     */
    private void tickSeerCombatRole(final ServerPlayer owner, final LivingEntity target, final long now) {
        // Stay near owner for hive sense range
        if (distanceToSqr(owner) > 4.0D * 4.0D) {
            requestPath(owner.blockPosition(), movementSpeedForState(), now);
        }
        // Will's combat contribution is via powers (disrupt/redirect/shatter),
        // not melee. If the target gets too close, retreat.
        if (distanceToSqr(target) < 3.0D * 3.0D) {
            final Vec3 retreatDir = position().subtract(target.position()).normalize();
            requestPath(BlockPos.containing(position().add(retreatDir.scale(2.0D))),
                    movementSpeedForState() + 0.04D, now);
        }
    }

    private void tickRetreat(final ServerPlayer owner, final long now) {
        if (tickFormationSlot(owner, now)) {
            return;
        }
        final BlockPos target = this.lastSafeWaypoint != null ? this.lastSafeWaypoint : owner.blockPosition();
        if (this.blockPosition().distSqr(target) <= 9.0D) {
            setCompanionState(CompanionState.FOLLOWING, "RETREAT_WAYPOINT_REACHED");
            DialogueService.get().speak(this, "retreat_arrived", 2);
            return;
        }
        requestPath(target, movementSpeedForState() + 0.04D, now);
    }

    private void tickReturnHome(final ServerPlayer owner, final long now) {
        if (this.returnHomeDestination == null || !this.level().hasChunkAt(this.returnHomeDestination)) {
            setCompanionState(CompanionState.HOLDING, "HOME_ANCHOR_INVALID_OR_UNLOADED");
            return;
        }
        if (this.blockPosition().distSqr(this.returnHomeDestination) <= 9.0D) {
            setCompanionState(CompanionState.RESTING, "HOME_ANCHOR_REACHED");
            this.getNavigation().stop();
            DialogueService.get().speak(this, "base_return", 3);
            BaseMilestoneService.onCompanionReachedHome(owner, this.role);
            return;
        }
        requestPath(this.returnHomeDestination, movementSpeedForState(), now);
    }

    private void tickBaseActivity(final ServerPlayer owner, final long now) {
        if (this.baseActivityTarget == null || !this.level().hasChunkAt(this.baseActivityTarget)) {
            setCompanionState(CompanionState.FOLLOWING, "BASE_ANCHOR_INVALID_OR_UNLOADED");
            return;
        }
        if (owner.blockPosition().distSqr(this.baseActivityTarget) > 34.0D * 34.0D) {
            setCompanionState(CompanionState.FOLLOWING, "OWNER_LEFT_BASE_RADIUS");
            return;
        }
        if (this.blockPosition().distSqr(this.baseActivityTarget) <= 2.25D) {
            this.getNavigation().stop();
            return;
        }
        requestPath(this.baseActivityTarget, 0.85D, now);
    }

    private void tickScout(final ServerPlayer owner, final long now) {
        if (this.scoutDestination == null) {
            DialogueService.get().speak(this, "scout_failed", 2);
            setCompanionState(CompanionState.FOLLOWING, "SCOUT_DESTINATION_MISSING");
            return;
        }
        if (!this.level().hasChunkAt(this.scoutDestination) || owner.blockPosition().distSqr(this.scoutDestination) > CompanionConfig.MAX_SCOUT_HARD_LIMIT.get() * CompanionConfig.MAX_SCOUT_HARD_LIMIT.get()) {
            DialogueService.get().speak(this, "scout_failed", 2);
            cancelScout("SCOUT_TARGET_OUTSIDE_LOADED_RANGE");
            return;
        }
        if (this.blockPosition().distSqr(this.scoutDestination) <= 6.0D) {
            beginVisualAction(CompanionAction.SCOUT_LOOKOUT, 48);
            DialogueService.get().speak(this, "scout_result", 2);
            com.riftcompanions.behavior.RoleBehaviorService.recordScoutReturn(owner, this);
            cancelScout("SCOUT_REPORT_COMPLETE");
            return;
        }
        requestPath(this.scoutDestination, movementSpeedForState() + 0.06D, now);
    }

    private void tickStuckRecovery(final ServerPlayer owner, final long now) {
        if (this.lastSafeWaypoint != null && this.blockPosition().distSqr(this.lastSafeWaypoint) > 4.0D) {
            requestPath(this.lastSafeWaypoint, movementSpeedForState(), now);
        }
        if (now - stateEnteredAt >= STUCK_RECALL_TIMEOUT) {
            if (!CompanionConfig.SAFE_RECALL_ENABLED.get()
                    || CompanionConfig.RECALL_POLICY.get() == com.riftcompanions.policy.RecallPolicy.STRICT_NAVIGATION) {
                setCompanionState(CompanionState.HOLDING, "SAFE_RECALL_DISABLED_BY_POLICY");
                return;
            }
            final boolean recalled = SafeTeleport.recallNearPlayer(this, owner, 5);
            if (recalled) {
                this.failedPathAttempts = 0;
                this.separationStartedAt = -1L;
                setCompanionState(CompanionState.FOLLOWING, "SAFE_RECALL_SUCCEEDED");
                DialogueService.get().speak(this, "unstuck", 1);
            } else {
                setCompanionState(CompanionState.HOLDING, "SAFE_RECALL_NO_VALID_DESTINATION");
            }
        }
    }

    private void applyStateTimeout(final ServerPlayer owner, final long now) {
        if (getCompanionState() == CompanionState.SCOUTING && now - stateEnteredAt >= SCOUT_TIMEOUT) {
            DialogueService.get().speak(this, "scout_failed", 2);
            cancelScout("SCOUT_TIMEOUT");
        }
        if (getCompanionState() == CompanionState.RETREATING && now - stateEnteredAt >= 400L) {
            setCompanionState(CompanionState.STUCK_RECOVERY, "RETREAT_PATH_TIMEOUT");
        }
        if (getCompanionState() == CompanionState.RETURNING_HOME && now - stateEnteredAt >= 600L) {
            setCompanionState(CompanionState.STUCK_RECOVERY, "RETURN_HOME_PATH_TIMEOUT");
        }
        if (getCompanionState() == CompanionState.RECOVERING && recoveryUntil > 0L && now >= recoveryUntil) {
            recoveryUntil = 0L;
            setCompanionState(CompanionState.FOLLOWING, "RECOVERY_COMPLETE");
        }
        if (getCompanionState() == CompanionState.RESTING && owner.level() == this.level()
                && "OWNER_CHANGED_DIMENSION".equals(this.lastReasonCode)) {
            setCompanionState(CompanionState.FOLLOWING, "OWNER_RETURNED_TO_DIMENSION");
        }
    }

    /**
     * Delegates all companion movement requests to the bounded navigation
     * safety layer. Vanilla still builds the actual path, but this call owns
     * replan cadence, path-node safety checks, alternate local goals, progress
     * detection, formation spacing, and Stuck Recovery escalation.
     */
    private void requestPath(final BlockPos target, final double speed, final long now) {
        final ServerPlayer owner = getOwnerPlayer().orElse(null);
        if (owner == null || target == null) return;
        final var board = TeamSavedData.get(owner.server).blackboard(owner.getUUID());
        final long navigationStartedAt = CompanionPerformanceMonitor.start();
        final NavigationResult result = CompanionNavigationService.moveTo(this, owner, target, speed,
                navigationIntent(now), board, now);
        CompanionPerformanceMonitor.record(owner.getUUID(), PerformanceWorkType.NAVIGATION, navigationStartedAt);
        CompanionNavigationService.recordOutcome(this.getUUID(), result);
        switch (result.outcome()) {
            case MOVING, ARRIVED -> {
                this.failedPathAttempts = 0;
            }
            case WAITING_FOR_REPATH -> { }
            case RETRYING_ALTERNATE, TARGET_REJECTED -> {
                this.failedPathAttempts = Math.max(this.failedPathAttempts, result.recoveryAttempts());
                this.lastReasonCode = result.reasonCode();
                if (this.role == CompanionRole.SCOUT && result.outcome() == NavigationOutcome.RETRYING_ALTERNATE) {
                    DialogueService.get().speak(this, "route_blocked", 2);
                }
            }
            case STUCK -> {
                this.failedPathAttempts = result.recoveryAttempts();
                if (getCompanionState() != CompanionState.STUCK_RECOVERY) {
                    setCompanionState(CompanionState.STUCK_RECOVERY, result.reasonCode());
                }
            }
        }
    }

    private NavigationIntent navigationIntent(final long now) {
        if (this.formationTarget != null && now <= this.formationExpiresAt) return NavigationIntent.FORMATION;
        return switch (getCompanionState()) {
            case GUARDING -> NavigationIntent.GUARD;
            case RETREATING -> NavigationIntent.RETREAT;
            case RETURNING_HOME -> NavigationIntent.RETURN_HOME;
            case BASE_ACTIVITY -> NavigationIntent.BASE_ACTIVITY;
            case SCOUTING -> NavigationIntent.SCOUT;
            case STUCK_RECOVERY -> NavigationIntent.STUCK_RECOVERY;
            default -> NavigationIntent.FOLLOW;
        };
    }

    private Monster nearestThreat(final ServerPlayer owner, final double radius) {
        Monster best = null;
        double bestScore = Double.MAX_VALUE;
        for (final Monster candidate : this.level().getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(radius), this::mayFight)) {
            final double score = candidate.distanceToSqr(owner) + candidate.distanceToSqr(this) * 0.25D;
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private boolean mayFight(final LivingEntity candidate) {
        return allowsCombatAction() && !candidate.getType().is(ModTags.PROTECTED_FROM_COMPANIONS);
    }

    private boolean hasActiveCombatTarget() {
        final LivingEntity target = getTarget();
        final double pursuit = CompanionConfig.COMBAT_PROFILE.get().pursuitRadius();
        return target != null && target.isAlive() && target.level() == this.level() && mayFight(target)
                && this.distanceToSqr(target) <= pursuit * pursuit;
    }

    /** Will is deliberately a warning/support role rather than an autonomous melee attacker. */
    public boolean allowsCombatAction() {
        return this.entityData.get(COMBAT_ENABLED)
                && this.role != CompanionRole.SEER
                && getCompanionState() != CompanionState.HOLDING
                && getCompanionState() != CompanionState.RESTING
                && getCompanionState() != CompanionState.RETREATING
                && getCompanionState() != CompanionState.RETURNING_HOME
                && getCompanionState() != CompanionState.BASE_ACTIVITY
                && getCompanionState() != CompanionState.DOWNED
                && getCompanionState() != CompanionState.EXHAUSTED;
    }

    /** A melee action may turn the body toward its confirmed target; normal route look remains head-only. */
    public void orientCombatBodyToward(final LivingEntity target) {
        if (target == null || target.level() != this.level()) return;
        final Vec3 toward = target.position().subtract(this.position());
        if (toward.horizontalDistanceSqr() < 0.04D) return;
        final float desired = (float) (Math.atan2(toward.z, toward.x) * (180.0D / Math.PI)) - 90.0F;
        final float delta = Mth.wrapDegrees(desired - this.getYRot());
        final float yaw = this.getYRot() + Mth.clamp(delta, -26.0F, 26.0F);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        this.setYHeadRot(yaw);
    }

    private double movementSpeedForState() {
        return switch (getCompanionState()) {
            case RETREATING, STUCK_RECOVERY -> 1.18D;
            case SCOUTING -> 1.12D;
            default -> 1.0D;
        };
    }

    private void regenerateEnergy(final long now) {
        if (now % 20L != 0L) {
            return;
        }
        final float refill = getCompanionState() == CompanionState.RESTING || getCompanionState() == CompanionState.RECOVERING || getCompanionState() == CompanionState.BASE_ACTIVITY ? 1.4F : 0.45F;
        if (getEnergy() < 100.0F) {
            setEnergy(Math.min(100.0F, getEnergy() + refill));
        }
        if (this.role == CompanionRole.SEER && getHiveStrain() > 0.0F) {
            final float reduction = getCompanionState() == CompanionState.RESTING || getCompanionState() == CompanionState.RECOVERING || getCompanionState() == CompanionState.BASE_ACTIVITY ? 1.8F : 0.55F;
            setHiveStrain(Math.max(0.0F, getHiveStrain() - reduction));
        }
        if (this.role == CompanionRole.SCOUT && getFocus() < 100.0F) {
            final float focusRefill = getCompanionState() == CompanionState.RESTING || getCompanionState() == CompanionState.BASE_ACTIVITY ? 1.5F : 0.5F;
            setFocus(Math.min(100.0F, getFocus() + focusRefill));
        }
    }

    private void expireVisualAction(final long now) {
        if (this.actionExpiresAt > 0L && now >= this.actionExpiresAt) {
            this.entityData.set(VISUAL_ACTION, CompanionAction.NONE.ordinal());
            this.actionExpiresAt = 0L;
        }
    }

    public CompanionRole getRole() {
        return this.role;
    }

    public CompanionInventory getPersonalInventory() {
        return this.personalInventory;
    }

    /**
     * Creates the bounded personal state stored only by TeamSavedData while a
     * companion is deliberately RESTING at a safe base. It excludes targets,
     * navigation, action state, rescue progress, and any world position.
     */
    public CompoundTag createRestingSnapshot() {
        final CompoundTag snapshot = new CompoundTag();
        snapshot.putInt("RestingSnapshotDataVersion", SaveVersions.RESTING_SNAPSHOT_DATA);
        snapshot.putString("RestingDimension", this.level().dimension().location().toString());
        snapshot.putFloat("Energy", getEnergy());
        snapshot.putFloat("HiveStrain", getHiveStrain());
        snapshot.putFloat("Focus", getFocus());
        snapshot.putFloat("Health", getHealth());
        snapshot.putBoolean("CombatEnabled", isCombatEnabled());
        this.personalInventory.save(snapshot);
        for (final CompanionAbility ability : CompanionAbility.values()) {
            final long cooldown = this.cooldowns.getOrDefault(ability, 0L);
            if (cooldown > 0L) snapshot.putLong("Cooldown_" + ability.name(), cooldown);
        }
        return snapshot;
    }

    /** Restores a previously saved bounded personal snapshot after a live entity is safely spawned. */
    public void restoreRestingSnapshot(final CompoundTag snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return;
        setEnergy(snapshot.contains("Energy") ? snapshot.getFloat("Energy") : getEnergy());
        setHiveStrain(snapshot.contains("HiveStrain") ? snapshot.getFloat("HiveStrain") : getHiveStrain());
        setFocus(snapshot.contains("Focus") ? snapshot.getFloat("Focus") : getFocus());
        if (snapshot.contains("Health")) {
            this.setHealth(Mth.clamp(snapshot.getFloat("Health"), 1.0F, this.getMaxHealth()));
        }
        setCombatEnabled(!snapshot.contains("CombatEnabled") || snapshot.getBoolean("CombatEnabled"));
        this.personalInventory.load(snapshot);
        this.cooldowns.clear();
        for (final CompanionAbility ability : CompanionAbility.values()) {
            final String key = "Cooldown_" + ability.name();
            if (snapshot.contains(key)) this.cooldowns.put(ability, Math.max(0L, snapshot.getLong(key)));
        }
        setCompanionState(CompanionState.FOLLOWING, "RESTING_SNAPSHOT_RESTORED");
    }

    public CompanionState getCompanionState() {
        return CompanionState.values()[Mth.clamp(this.entityData.get(COMPANION_STATE), 0, CompanionState.values().length - 1)];
    }

    public void setCompanionState(final CompanionState state, final String reasonCode) {
        final CompanionState previous = getCompanionState();
        if (previous == state && this.lastReasonCode.equals(reasonCode)) {
            return;
        }
        this.entityData.set(COMPANION_STATE, state.ordinal());
        this.stateEnteredAt = this.level().getGameTime();
        this.lastReasonCode = reasonCode;
        if (state == CompanionState.DOWNED) {
            if (previous != CompanionState.DOWNED) {
                this.downedSince = this.level().getGameTime();
                this.rescuerUuid = null;
                this.entityData.set(RESCUE_PROGRESS, 0);
                setDownedStatus(DownedStatus.STABLE);
            }
            beginVisualAction(CompanionAction.DOWNED, 24000);
            this.setTarget(null);
            this.getNavigation().stop();
            CompanionNavigationService.clearEntity(this.getUUID());
        }
        if (state == CompanionState.RETREATING) {
            beginVisualAction(CompanionAction.RETREAT_SIGNAL, 18);
        }
        if (state == CompanionState.STUCK_RECOVERY && previous != CompanionState.STUCK_RECOVERY && !this.level().isClientSide) {
            getOwnerPlayer().ifPresent(com.riftcompanions.onboarding.OnboardingService::onStuckRecovery);
        }
    }

    public String getLastReasonCode() {
        return this.lastReasonCode;
    }

    public boolean requiresSaveRecovery() { return saveRecoveryRequired; }
    public String saveRecoveryReason() { return saveRecoveryReason; }

    /**
     * Called by SaveRecoveryService after the logical player exists. Any stale
     * navigation/rescue state is cancelled rather than guessed complete.
     */
    public String recoverAfterLoad(final ServerPlayer owner) {
        if (!(this.level() instanceof ServerLevel level) || owner == null) return "RECOVERY_OWNER_UNAVAILABLE";
        String issue = "";
        if (saveRecoveryRequired) issue = saveRecoveryReason.isBlank() ? "COMPANION_DATA_REQUIRES_REVIEW" : saveRecoveryReason;
        if (getCompanionState() == CompanionState.SCOUTING || getCompanionState() == CompanionState.RETURNING_HOME
                || getCompanionState() == CompanionState.BASE_ACTIVITY) {
            this.scoutDestination = null;
            this.returnHomeDestination = null;
            this.baseActivityTarget = null;
            setCompanionState(CompanionState.FOLLOWING, "LOAD_CANCELLED_STALE_NAVIGATION_TASK");
            issue = appendRecoveryIssue(issue, "STALE_NAVIGATION_CANCELLED");
        }
        if (getCompanionState() == CompanionState.DOWNED && this.rescuerUuid != null) {
            this.rescuerUuid = null;
            this.entityData.set(RESCUE_PROGRESS, 0);
            getOwnerUuid().ifPresent(id -> TeamSavedData.get(level.getServer()).blackboard(id).releaseRescue(this.getUUID()));
            issue = appendRecoveryIssue(issue, "STALE_RESCUE_RESET");
        }
        if (owner.level() == this.level() && !SafeTeleport.isSafeStanding(level, this, this.blockPosition())) {
            if (SafeTeleport.recallNearPlayer(this, owner, 5)) {
                setCompanionState(CompanionState.FOLLOWING, "LOAD_SAFE_RELOCATION");
                issue = appendRecoveryIssue(issue, "SAFE_RELOCATED");
            } else {
                setCompanionState(CompanionState.HOLDING, "LOAD_NO_SAFE_RELOCATION");
                CompanionFaultService.recordInvalidWorldState(owner, this.role, "NO_SAFE_RELOCATION");
                issue = appendRecoveryIssue(issue, "NO_SAFE_RELOCATION");
            }
        }
        if (this.personalInventory.lastLoadWarningCount() > 0) {
            issue = appendRecoveryIssue(issue, "INVALID_INVENTORY_REFERENCES_IGNORED_" + this.personalInventory.lastLoadWarningCount());
        }
        saveRecoveryRequired = false;
        saveRecoveryReason = "";
        return issue.isBlank() ? "RECOVERY_CLEAR" : issue;
    }

    private static String appendRecoveryIssue(final String existing, final String addition) {
        return existing == null || existing.isBlank() ? addition : existing + "," + addition;
    }

    public float getEnergy() {
        return this.entityData.get(ENERGY);
    }

    public void setEnergy(final float value) {
        this.entityData.set(ENERGY, Mth.clamp(value, 0.0F, 100.0F));
    }

    public boolean consumeEnergy(final float cost) {
        if (getEnergy() < cost) {
            return false;
        }
        setEnergy(getEnergy() - cost);
        return true;
    }

    public float getHiveStrain() {
        return this.entityData.get(HIVE_STRAIN);
    }

    public void setHiveStrain(final float value) {
        this.entityData.set(HIVE_STRAIN, Mth.clamp(value, 0.0F, 100.0F));
    }

    public boolean addHiveStrain(final float cost) {
        if (getHiveStrain() + cost > 100.0F) {
            return false;
        }
        setHiveStrain(getHiveStrain() + cost);
        return true;
    }

    public float getFocus() {
        return this.entityData.get(FOCUS);
    }

    public void setFocus(final float value) {
        this.entityData.set(FOCUS, Mth.clamp(value, 0.0F, 100.0F));
    }

    public boolean consumeFocus(final float cost) {
        if (getFocus() < cost) {
            return false;
        }
        setFocus(getFocus() - cost);
        return true;
    }

    public boolean isAbilityReady(final CompanionAbility ability) {
        return this.level().getGameTime() >= this.cooldowns.getOrDefault(ability, 0L)
                && getCompanionState() != CompanionState.DOWNED
                && getCompanionState() != CompanionState.EXHAUSTED;
    }

    public long abilityRemainingTicks(final CompanionAbility ability) {
        return Math.max(0L, this.cooldowns.getOrDefault(ability, 0L) - this.level().getGameTime());
    }

    public void setAbilityCooldown(final CompanionAbility ability, final long ticks) {
        this.cooldowns.put(ability, this.level().getGameTime() + Math.max(0L, ticks));
    }

    public CompanionAction getVisualAction() {
        return CompanionAction.byId(this.entityData.get(VISUAL_ACTION));
    }

    /** Server-derived gaze intent; clients use it only for the face animation layer. */
    public CompanionLookIntent getLookIntent() {
        return CompanionLookIntent.byId(this.entityData.get(LOOK_INTENT));
    }

    public void setLookIntent(final CompanionLookIntent intent) {
        this.entityData.set(LOOK_INTENT, (intent == null ? CompanionLookIntent.FORWARD : intent).ordinal());
    }

    /** Derives a visual state from server-owned AI state; clients only render it. */
    public CompanionVisualState getVisualState() {
        if (getCompanionState() == CompanionState.DOWNED) return CompanionVisualState.DOWNED;
        final CompanionAction action = getVisualAction();
        if (action == CompanionAction.SEER_FOCUS || action == CompanionAction.SEER_DANGER_MODE
                || action == CompanionAction.GIFTED_FOCUS || action == CompanionAction.GIFTED_SHIELD
                || action == CompanionAction.GUARDIAN_BRACE) {
            return CompanionVisualState.POWER_FOCUS;
        }
        if (action == CompanionAction.SCOUT_POINT || action == CompanionAction.SCOUT_LOOKOUT || action == CompanionAction.SCOUT_ANCHOR
                || action == CompanionAction.SCOUT_SIGNAL) {
            return CompanionVisualState.POINT_ROUTE;
        }
        if (getCompanionState() == CompanionState.RECOVERING || getCompanionState() == CompanionState.EXHAUSTED) return CompanionVisualState.RECOVERY;
        if (getCompanionState() == CompanionState.RETREATING || getCompanionState() == CompanionState.STUCK_RECOVERY) return CompanionVisualState.RETREAT;
        if (getCompanionState() == CompanionState.GUARDING) return CompanionVisualState.GUARD;
        if (getCompanionState() == CompanionState.OBSERVING) return CompanionVisualState.OBSERVE;
        if (getCompanionState() == CompanionState.FIGHTING) return role == CompanionRole.SCOUT ? CompanionVisualState.COMBAT_RANGED : CompanionVisualState.COMBAT_MELEE;
        if (getTarget() != null) return CompanionVisualState.ALERT;
        if (getCompanionState() == CompanionState.BASE_ACTIVITY) return CompanionVisualState.INTERACT_ANCHOR;
        if (getCompanionState() == CompanionState.FOLLOWING) return CompanionVisualState.FOLLOW;
        return CompanionVisualState.IDLE_CALM;
    }

    public void beginVisualAction(final CompanionAction action, final long durationTicks) {
        final CompanionAction safeAction = action == null ? CompanionAction.NONE : action;
        this.entityData.set(VISUAL_ACTION, safeAction.ordinal());
        this.actionExpiresAt = this.level().getGameTime() + Math.max(1L, durationTicks);
        // Audio is a confirmed presentation cue only. It does not control the
        // action result and only runs on the logical server.
        CompanionPresentationSoundService.playForAction(this, safeAction);
    }

    public boolean hasShieldActive() {
        return getShieldTicks() > 0;
    }

    public int getShieldTicks() {
        return this.entityData.get(SHIELD_TICKS);
    }

    public void enableShield(final int durationTicks) {
        this.entityData.set(SHIELD_TICKS, Math.max(0, durationTicks));
        beginVisualAction(CompanionAction.GIFTED_SHIELD, durationTicks);
    }

    /** Used by Safe Mode/config recovery to stop a temporary protection effect without a delayed cost. */
    public void disableActiveProtection(final String reason) {
        if (getShieldTicks() <= 0) return;
        this.entityData.set(SHIELD_TICKS, 0);
        this.entityData.set(VISUAL_ACTION, CompanionAction.NONE.ordinal());
        this.actionExpiresAt = 0L;
        this.lastReasonCode = reason == null ? "PROTECTION_CANCELLED" : reason;
    }

    public Optional<UUID> getOwnerUuid() {
        return this.entityData.get(OWNER_UUID);
    }

    public Optional<ServerPlayer> getOwnerPlayer() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return getOwnerUuid().map(uuid -> serverLevel.getServer().getPlayerList().getPlayer(uuid));
    }

    public boolean isOwnedBy(final ServerPlayer player) {
        return getOwnerUuid().map(player.getUUID()::equals).orElse(false);
    }

    public void setOwner(final ServerPlayer owner) {
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        this.setCustomName(Component.literal(this.role.personalName()));
        this.setCustomNameVisible(false);
        this.lastSafeWaypoint = owner.blockPosition().immutable();
        setCompanionState(CompanionState.FOLLOWING, "OWNER_ASSIGNED");
    }

    public BlockPos getLastSafeWaypoint() {
        return this.lastSafeWaypoint;
    }

    public void setLastSafeWaypoint(final BlockPos lastSafeWaypoint) {
        this.lastSafeWaypoint = lastSafeWaypoint == null ? null : lastSafeWaypoint.immutable();
    }

    public boolean beginScout(final BlockPos destination) {
        if (this.role != CompanionRole.SCOUT || destination == null || !this.level().hasChunkAt(destination)) {
            return false;
        }
        this.scoutDestination = destination.immutable();
        setCompanionState(CompanionState.SCOUTING, "PLAYER_APPROVED_SHORT_SCOUT");
        beginVisualAction(CompanionAction.SCOUT_POINT, 18);
        return true;
    }

    public void cancelScout(final String reason) {
        this.scoutDestination = null;
        setCompanionState(CompanionState.FOLLOWING, reason);
    }

    /** Called only by BaseAnchorService after dimension, chunk and anchor validation. */
    public boolean beginReturnHome(final BlockPos destination) {
        if (destination == null || !this.level().hasChunkAt(destination) || getCompanionState() == CompanionState.DOWNED) {
            return false;
        }
        this.returnHomeDestination = destination.immutable();
        setCompanionState(CompanionState.RETURNING_HOME, "PLAYER_RETURN_HOME_ORDER");
        return true;
    }

    public void assignFormationSlot(final BlockPos target, final FormationType formation, final long expiresAt) {
        if (target == null || getCompanionState() == CompanionState.DOWNED || getCompanionState() == CompanionState.SCOUTING
                || getCompanionState() == CompanionState.RETURNING_HOME || getCompanionState() == CompanionState.STUCK_RECOVERY) {
            return;
        }
        this.formationTarget = target.immutable();
        this.assignedFormation = formation == null ? FormationType.FOLLOW : formation;
        this.formationExpiresAt = Math.max(this.level().getGameTime() + 1L, expiresAt);
    }

    public void clearFormationSlot(final String reason) {
        getOwnerUuid().ifPresent(owner -> FormationSlotReservationService.release(owner, this.getUUID()));
        this.formationTarget = null;
        this.formationExpiresAt = 0L;
        this.assignedFormation = FormationType.FOLLOW;
    }

    public FormationType getAssignedFormation() {
        return this.assignedFormation;
    }

    /** Safe base-life motion only; no chest, bed, redstone, farming or block interaction is implied. */
    public boolean beginBaseActivity(final BlockPos destination) {
        if (destination == null || !this.level().hasChunkAt(destination)
                || getCompanionState() == CompanionState.DOWNED || getCompanionState() == CompanionState.SCOUTING
                || getCompanionState() == CompanionState.RETREATING) {
            return false;
        }
        this.baseActivityTarget = destination.immutable();
        setCompanionState(CompanionState.BASE_ACTIVITY, "BASE_ANCHOR_ACTIVITY");
        return true;
    }

    public boolean isCombatEnabled() {
        return this.entityData.get(COMBAT_ENABLED);
    }

    public void setCombatEnabled(final boolean enabled) {
        this.entityData.set(COMBAT_ENABLED, enabled);
        if (!enabled) {
            this.setTarget(null);
        }
    }

    public int getRescueProgress() {
        return this.entityData.get(RESCUE_PROGRESS);
    }

    public DownedStatus getDownedStatus() {
        return DownedStatus.values()[Mth.clamp(this.entityData.get(DOWNED_STATUS), 0, DownedStatus.values().length - 1)];
    }

    private void setDownedStatus(final DownedStatus status) {
        this.entityData.set(DOWNED_STATUS, status.ordinal());
    }

    public boolean isBeingRescued() {
        return this.rescuerUuid != null;
    }

    /** Starts a 3-second player rescue only after RescueService validated proximity and safety. */
    public boolean beginPlayerRescue(final ServerPlayer rescuer) {
        if (getCompanionState() != CompanionState.DOWNED || rescuer == null || !isOwnedBy(rescuer)) {
            return false;
        }
        this.rescuerUuid = rescuer.getUUID();
        this.entityData.set(RESCUE_PROGRESS, 0);
        this.lastReasonCode = "PLAYER_RESCUE_IN_PROGRESS";
        return true;
    }

    public void enterDownedState() {
        if (getCompanionState() == CompanionState.DOWNED) {
            return;
        }
        this.setHealth(Math.max(1.0F, this.getMaxHealth() * 0.12F));
        setCompanionState(CompanionState.DOWNED, "HEALTH_DEPLETED_STORY_DOWNED");
        if (this.level() instanceof ServerLevel level) {
            TeamSavedData.get(level.getServer()).setLifecycle(getOwnerUuid().orElse(null), this.role, CompanionLifecycle.DOWNED, this.getUUID());
        }
    }

    public void beginRecovery(final long durationTicks, final String reason) {
        recoveryUntil = this.level().getGameTime() + Math.max(20L, durationTicks);
        setCompanionState(CompanionState.RECOVERING, reason);
        beginVisualAction(CompanionAction.RECOVER, Math.min(40L, Math.max(8L, durationTicks)));
    }

    public void scheduleRecovery(final long delayTicks, final long durationTicks, final String reason) {
        scheduledRecoveryAt = this.level().getGameTime() + Math.max(1L, delayTicks);
        scheduledRecoveryDuration = Math.max(20L, durationTicks);
        scheduledRecoveryReason = reason == null ? "SCHEDULED_RECOVERY" : reason;
    }

    public void reviveFromDowned() {
        if (getCompanionState() != CompanionState.DOWNED) {
            return;
        }
        this.setHealth(Math.max(2.0F, this.getMaxHealth() * 0.35F));
        setEnergy(Math.max(20.0F, Math.min(getEnergy(), 35.0F)));
        this.rescuerUuid = null;
        this.entityData.set(RESCUE_PROGRESS, 0);
        setDownedStatus(DownedStatus.RECOVERING);
        beginRecovery(200L, "REVIVED_NEEDS_RECOVERY");
        DialogueService.get().speak(this, "recovery", 2);
        if (this.level() instanceof ServerLevel level) {
            TeamSavedData.get(level.getServer()).setLifecycle(getOwnerUuid().orElse(null), this.role, CompanionLifecycle.ACTIVE, this.getUUID());
        }
    }

    @Override
    protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer && isOwnedBy(serverPlayer)) {
            if (getCompanionState() == CompanionState.DOWNED) {
                final RescueService.RescueResult result = RescueService.beginPlayerRescue(serverPlayer, this);
                serverPlayer.sendSystemMessage(Component.literal("[Rift Companions] " + result.detail()));
                return InteractionResult.CONSUME;
            }
            if (serverPlayer.isShiftKeyDown()) {
                final CompanionState next = getCompanionState() == CompanionState.HOLDING ? CompanionState.FOLLOWING : CompanionState.HOLDING;
                setCompanionState(next, "PLAYER_INTERACTION_TOGGLE");
                serverPlayer.sendSystemMessage(Component.translatable("message.riftcompanions.state_changed", role.personalName(), next.name()));
            } else {
                beginVisualAction(CompanionAction.TALK, 20L);
                ModNetwork.sendToPlayer(new S2COpenConversationPacket(this.role.ordinal()), serverPlayer);
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CompanionDataVersion", SaveVersions.COMPANION_DATA);
        getOwnerUuid().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putString("CompanionState", getCompanionState().name());
        tag.putString("Reason", this.lastReasonCode);
        tag.putFloat("Energy", getEnergy());
        tag.putFloat("HiveStrain", getHiveStrain());
        tag.putFloat("Focus", getFocus());
        tag.putBoolean("CombatEnabled", isCombatEnabled());
        tag.putLong("StateEnteredAt", this.stateEnteredAt);
        tag.putInt("ShieldTicks", getShieldTicks());
        tag.putLong("RecoveryUntil", this.recoveryUntil);
        tag.putLong("ScheduledRecoveryAt", this.scheduledRecoveryAt);
        tag.putLong("ScheduledRecoveryDuration", this.scheduledRecoveryDuration);
        tag.putString("ScheduledRecoveryReason", this.scheduledRecoveryReason);
        tag.putLong("DownedSince", this.downedSince);
        tag.putInt("RescueProgress", getRescueProgress());
        tag.putInt("DownedStatus", getDownedStatus().ordinal());
        if (this.rescuerUuid != null) {
            tag.putUUID("Rescuer", this.rescuerUuid);
        }
        if (this.lastSafeWaypoint != null) {
            tag.putLong("LastSafeWaypoint", this.lastSafeWaypoint.asLong());
        }
        if (this.returnHomeDestination != null) {
            tag.putLong("ReturnHomeDestination", this.returnHomeDestination.asLong());
        }
        if (this.baseActivityTarget != null) {
            tag.putLong("BaseActivityTarget", this.baseActivityTarget.asLong());
        }
        this.personalInventory.save(tag);
        for (final CompanionAbility ability : CompanionAbility.values()) {
            final long cooldown = this.cooldowns.getOrDefault(ability, 0L);
            if (cooldown > 0L) {
                tag.putLong("Cooldown_" + ability.name(), cooldown);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        final var migration = SaveMigrationService.migrateCompanion(tag);
        final CompoundTag data = migration.tag();
        this.saveRecoveryRequired = !migration.successful();
        this.saveRecoveryReason = migration.successful() ? "" : migration.summary();
        if (data.hasUUID("Owner")) {
            this.entityData.set(OWNER_UUID, Optional.of(data.getUUID("Owner")));
        }
        try {
            setCompanionState(CompanionState.valueOf(data.getString("CompanionState")), data.getString("Reason"));
        } catch (final IllegalArgumentException ignored) {
            setCompanionState(CompanionState.IDLE, "INVALID_SAVED_STATE_FALLBACK");
        }
        setEnergy(data.contains("Energy") ? data.getFloat("Energy") : 100.0F);
        setHiveStrain(data.contains("HiveStrain") ? data.getFloat("HiveStrain") : 0.0F);
        setFocus(data.contains("Focus") ? data.getFloat("Focus") : 100.0F);
        setCombatEnabled(!data.contains("CombatEnabled") || data.getBoolean("CombatEnabled"));
        this.stateEnteredAt = data.getLong("StateEnteredAt");
        this.recoveryUntil = data.getLong("RecoveryUntil");
        this.scheduledRecoveryAt = data.getLong("ScheduledRecoveryAt");
        this.scheduledRecoveryDuration = data.getLong("ScheduledRecoveryDuration");
        this.scheduledRecoveryReason = data.getString("ScheduledRecoveryReason");
        this.entityData.set(SHIELD_TICKS, Math.max(0, data.getInt("ShieldTicks")));
        this.downedSince = data.getLong("DownedSince");
        this.entityData.set(RESCUE_PROGRESS, Math.max(0, Math.min(60, data.getInt("RescueProgress"))));
        this.entityData.set(DOWNED_STATUS, Mth.clamp(data.getInt("DownedStatus"), 0, DownedStatus.values().length - 1));
        this.rescuerUuid = data.hasUUID("Rescuer") ? data.getUUID("Rescuer") : null;
        if (data.contains("LastSafeWaypoint")) {
            this.lastSafeWaypoint = BlockPos.of(data.getLong("LastSafeWaypoint"));
        }
        if (data.contains("ReturnHomeDestination")) {
            this.returnHomeDestination = BlockPos.of(data.getLong("ReturnHomeDestination"));
        }
        if (data.contains("BaseActivityTarget")) {
            this.baseActivityTarget = BlockPos.of(data.getLong("BaseActivityTarget"));
        }
        this.personalInventory.load(data);
        for (final CompanionAbility ability : CompanionAbility.values()) {
            final String key = "Cooldown_" + ability.name();
            if (data.contains(key)) {
                this.cooldowns.put(ability, data.getLong(key));
            }
        }
    }

    // ------------------------------ GeckoLib ---------------------------------

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        // Separate controllers keep the face and hair alive beneath a body
        // action. Every controller remains client presentation only.
        controllers.add(new AnimationController<>(this, "companion_body", 0, this::animationPredicate));
        controllers.add(new AnimationController<>(this, "companion_face", 0, this::faceAnimationPredicate));
        controllers.add(new AnimationController<>(this, "companion_secondary", 0, this::secondaryAnimationPredicate));
    }

    private PlayState animationPredicate(final AnimationState<CompanionEntity> state) {
        return CompanionAnimationController.apply(this, state);
    }

    private PlayState faceAnimationPredicate(final AnimationState<CompanionEntity> state) {
        return CompanionAnimationController.applyFace(this, state);
    }

    private PlayState secondaryAnimationPredicate(final AnimationState<CompanionEntity> state) {
        return CompanionAnimationController.applySecondary(this, state);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
