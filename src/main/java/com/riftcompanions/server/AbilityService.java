package com.riftcompanions.server;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionAbility;
import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.power.PowerPolicy;
import com.riftcompanions.policy.PlayerPolicyService;
import com.riftcompanions.registry.ModTags;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.safety.SafeModeService;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import com.riftcompanions.duo.DuoDynamicsService;
import com.riftcompanions.duo.DuoSynergy;
import com.riftcompanions.intention.IntentionService;
import com.riftcompanions.arc.ArcMilestone;
import com.riftcompanions.arc.ArcService;
import com.riftcompanions.mental.MindAnchorService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Explicit player-commanded abilities. No animation applies gameplay effects;
 * every effect is validated here on the logical server before it is performed.
 */
public final class AbilityService {
    private AbilityService() {}

    public static AbilityResult cast(final ServerPlayer owner, final CompanionAbility ability) {
        if (owner == null || ability == null) return AbilityResult.failure("INVALID_ABILITY_REQUEST");
        if (SafeModeService.enabled(owner)) return AbilityResult.failure("SAFE_MODE_POWER_ACTIONS_DISABLED");
        if (!CompanionConfig.POWERS_ENABLED.get()) {
            return AbilityResult.failure("POWERS_DISABLED");
        }
        if (!featureAllows(ability)) return AbilityResult.failure("ABILITY_FEATURE_FLAG_DISABLED");
        if (isAdvancedMindAnchor(ability) && (!CompanionConfig.MIND_ANCHOR_ENABLED.get() || !FeatureFlags.enabled(FeatureFlag.MAX_MIND_ANCHOR))) {
            return AbilityResult.failure("MIND_ANCHOR_ADVANCED_DISABLED");
        }
        final Optional<CompanionEntity> companionOptional = CompanionLifecycleService.findForOwner(owner, ability.role());
        if (companionOptional.isEmpty()) {
            return AbilityResult.failure("ROLE_NOT_ACTIVE");
        }
        final CompanionEntity companion = companionOptional.get();
        if (!companion.isAbilityReady(ability)) {
            notifyPowerUnavailable(companion);
            return AbilityResult.failure("ABILITY_COOLDOWN_OR_RECOVERY");
        }
        if (!policyAllows(owner, ability)) {
            notifyPowerUnavailable(companion);
            return AbilityResult.failure("POWER_POLICY_RESTRICTED");
        }
        final long now = owner.level().getGameTime();
        final var board = TeamSavedData.get(owner.server).blackboard(owner.getUUID());
        final var transactionBegin = board.actionLedger().begin(ActionType.POWER_CAST,
                "power:" + ability.id() + ":" + now, ability.role().id(), now, 160L);
        if (transactionBegin.reused()) return AbilityResult.failure("POWER_ACTION_ALREADY_PENDING");
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("POWER_REQUEST_VALIDATED");
        final AbilityResult result = switch (ability) {
            case SEER_SENSE -> castSeerSense(owner, companion);
            case SEER_DISRUPT -> castSeerDisrupt(owner, companion);
            case SEER_SUSPEND -> castHiveChannel(owner, companion, com.riftcompanions.hive.control.HiveControlMode.SUSPEND);
            case SEER_REDIRECT -> castHiveChannel(owner, companion, com.riftcompanions.hive.control.HiveControlMode.REDIRECT);
            case SEER_SHATTER -> castHiveChannel(owner, companion, com.riftcompanions.hive.control.HiveControlMode.SHATTER);
            case SEER_SWARM_FREEZE -> castHiveChannel(owner, companion, com.riftcompanions.hive.control.HiveControlMode.SWARM_FREEZE);
            case SEER_SURGE_SUSPEND -> castHiveSurge(owner, companion, com.riftcompanions.hive.control.HiveControlMode.SUSPEND);
            case SEER_SURGE_REDIRECT -> castHiveSurge(owner, companion, com.riftcompanions.hive.control.HiveControlMode.REDIRECT);
            case SEER_SURGE_SHATTER -> castHiveSurge(owner, companion, com.riftcompanions.hive.control.HiveControlMode.SHATTER);
            case SEER_SURGE_SWARM_FREEZE -> castHiveSurge(owner, companion, com.riftcompanions.hive.control.HiveControlMode.SWARM_FREEZE);
            case GIFTED_PUSH -> castGiftedPush(owner, companion);
            case GIFTED_SHIELD -> castGiftedShield(owner, companion);
            case GIFTED_RESCUE -> castGiftedRescue(owner, companion);
            case GUARDIAN_BRACE -> castGuardianBrace(owner, companion);
            case SCOUT_ROUTE -> castScoutRoute(owner, companion);
            case SCOUT_SIGNAL -> castScoutSignal(owner, companion);
            case SCOUT_GROUNDING -> castScoutGrounding(owner, companion);
            case SCOUT_ANCHOR_POINT -> castMindAnchorPoint(owner, companion);
            case SCOUT_BREAK_FREE -> castMindAnchorBreakFree(owner, companion);
            case SCOUT_ESCAPE_WINDOW -> castMindAnchorEscapeWindow(owner, companion);
        };
        if (result.successful()) {
            if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("POWER_EFFECT_CONFIRMED");
            if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, result.code());
            recordOptionalMilestones(owner, ability, result.code());
        } else {
            transaction.rollback(now, result.code());
        }
        TeamSavedData.get(owner.server).markChanged();
        return result;
    }

    private static boolean isAdvancedMindAnchor(final CompanionAbility ability) {
        return ability == CompanionAbility.SCOUT_ANCHOR_POINT || ability == CompanionAbility.SCOUT_BREAK_FREE
                || ability == CompanionAbility.SCOUT_ESCAPE_WINDOW;
    }

    private static boolean featureAllows(final CompanionAbility ability) {
        return switch (ability.role()) {
            case SEER -> FeatureFlags.enabled(FeatureFlag.WILL_HIVE_LINK);
            case GIFTED -> FeatureFlags.enabled(FeatureFlag.ELEVEN_POWERS);
            case SCOUT -> FeatureFlags.enabled(FeatureFlag.MAX_SCOUT);
            case GUARDIAN -> FeatureFlags.enabled(FeatureFlag.HOPPER_CORE);
        };
    }

    private static void recordOptionalMilestones(final ServerPlayer owner, final CompanionAbility ability, final String code) {
        switch (ability) {
            case SEER_SENSE -> {
                if (code.startsWith("ANOMALY_VISIBLE")) ArcService.observe(owner, ArcMilestone.ANOMALY_DOCUMENTED);
            }
            case SEER_DISRUPT -> DuoDynamicsService.noteSynergy(owner, DuoSynergy.DISRUPT_ROUTE_WINDOW);
            case GIFTED_SHIELD -> {
                IntentionService.observeCompletion(owner, "POWER_GIFTED_SHIELD");
                com.riftcompanions.story.StoryService.completePromiseByKey(owner, "ABILITY_GIFTED_SHIELD");
                DuoDynamicsService.noteSynergy(owner, DuoSynergy.GUARD_SHIELD_WINDOW);
            }
            case GIFTED_RESCUE -> ArcService.observe(owner, ArcMilestone.RESCUE_SUCCEEDED);
            case GUARDIAN_BRACE -> DuoDynamicsService.noteSynergy(owner, DuoSynergy.GUARD_SHIELD_WINDOW);
            case SCOUT_ROUTE -> ArcService.observe(owner, ArcMilestone.SCOUT_SUCCEEDED);
            case SCOUT_SIGNAL -> ArcService.observe(owner, ArcMilestone.CAUTIOUS_OBSERVATION);
            default -> { }
        }
    }

    public static boolean hasShieldFor(final ServerPlayer player) {
        return CompanionLifecycleService.findForOwner(player, com.riftcompanions.entity.CompanionRole.GIFTED)
                .filter(companion -> companion.hasShieldActive() && companion.distanceToSqr(player) <= 10.0D * 10.0D)
                .isPresent();
    }

    private static boolean policyAllows(final ServerPlayer owner, final CompanionAbility ability) {
        final boolean emergency = isEmergency(owner);
        return switch (ability) {
            case SEER_SENSE -> PlayerPolicyService.effectivePowerPolicy(owner, com.riftcompanions.entity.CompanionRole.SEER) != PowerPolicy.OFF;
            case SEER_DISRUPT, SEER_SUSPEND, SEER_REDIRECT, SEER_SHATTER, SEER_SWARM_FREEZE,
                    SEER_SURGE_SUSPEND, SEER_SURGE_REDIRECT, SEER_SURGE_SHATTER, SEER_SURGE_SWARM_FREEZE -> {
                final PowerPolicy policy = PlayerPolicyService.effectivePowerPolicy(owner, com.riftcompanions.entity.CompanionRole.SEER);
                yield policy != PowerPolicy.OFF && policy != PowerPolicy.SENSE_ONLY
                        && (policy.permitsExplicitNonEmergency() || (policy == PowerPolicy.EMERGENCY_ONLY && emergency));
            }
            case GIFTED_RESCUE -> PlayerPolicyService.effectivePowerPolicy(owner, com.riftcompanions.entity.CompanionRole.GIFTED).permitsEmergency();
            case GIFTED_PUSH, GIFTED_SHIELD -> {
                final PowerPolicy policy = PlayerPolicyService.effectivePowerPolicy(owner, com.riftcompanions.entity.CompanionRole.GIFTED);
                yield policy.permitsExplicitNonEmergency() || (policy == PowerPolicy.EMERGENCY_ONLY && emergency);
            }
            case GUARDIAN_BRACE -> true;
            case SCOUT_ROUTE -> true;
            case SCOUT_SIGNAL, SCOUT_GROUNDING, SCOUT_ANCHOR_POINT, SCOUT_BREAK_FREE, SCOUT_ESCAPE_WINDOW -> {
                final PowerPolicy policy = PlayerPolicyService.effectivePowerPolicy(owner, com.riftcompanions.entity.CompanionRole.SCOUT);
                yield policy.permitsExplicitNonEmergency() || (policy == PowerPolicy.EMERGENCY_ONLY && emergency);
            }
        };
    }

    private static boolean isEmergency(final ServerPlayer owner) {
        return owner.isInLava() || owner.isOnFire() || owner.fallDistance > 3.0F
                || owner.getHealth() <= owner.getMaxHealth() * 0.30F
                || !owner.level().getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(6.0D)).isEmpty();
    }

    private static void notifyPowerUnavailable(final CompanionEntity companion) {
        if (companion.getRole() == com.riftcompanions.entity.CompanionRole.GIFTED) {
            DialogueService.get().speak(companion, "power_low", 2);
        }
    }

    /** Seer Sense reads only visible nearby hostile pressure in the normal world; it never scans caves, chunks, or hidden structures. */
    private static AbilityResult castSeerSense(final ServerPlayer owner, final CompanionEntity seer) {
        if (!seer.consumeEnergy(10.0F)) {
            return AbilityResult.failure("SEER_ENERGY_LOW");
        }
        final List<Monster> visibleHostiles = owner.level().getEntitiesOfClass(Monster.class,
                seer.getBoundingBox().inflate(20.0D), entity -> entity.isAlive() && seer.hasLineOfSight(entity));
        seer.setAbilityCooldown(CompanionAbility.SEER_SENSE, 100L);
        seer.beginVisualAction(CompanionAction.SEER_NOTICE, 18);
        if (visibleHostiles.isEmpty()) {
            DialogueService.get().speak(seer, "sense_clear", 2);
            return AbilityResult.success("NO_VISIBLE_HOSTILE_SIGNAL", seer);
        }
        final Monster nearest = visibleHostiles.stream().min(Comparator.comparingDouble(seer::distanceToSqr)).orElseThrow();
        final String direction = cardinalDirection(nearest.position().subtract(owner.position()));
        final ServerLevel level = owner.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, nearest.getX(), nearest.getY() + nearest.getBbHeight() * 0.5D, nearest.getZ(), effectCount(10), 0.25D, 0.35D, 0.25D, 0.01D);
        if (nearest.getType().is(ModTags.HIVE_LINKED)) {
            com.riftcompanions.story.StoryService.onHiveObserved(owner);
        }
        DialogueService.get().speak(seer, "sense_direction", 1);
        return AbilityResult.success("HOSTILE_VISIBLE_" + direction, seer);
    }

    /**
     * Temporary single-target disruption for any eligible hostile Mob. The
     * same heart-budget and boss-resistance model as Will's channel powers
     * determines cost and duration; it never grants pet ownership or loot.
     */
    private static AbilityResult castSeerDisrupt(final ServerPlayer owner, final CompanionEntity seer) {
        final List<net.minecraft.world.entity.Mob> candidates = owner.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                seer.getBoundingBox().inflate(10.0D), target -> com.riftcompanions.hive.control.WillControlEligibility.isActiveThreat(owner, target)
                        && seer.hasLineOfSight(target) && owner.serverLevel().hasChunkAt(target.blockPosition()))
                .stream().sorted(Comparator.comparingDouble(seer::distanceToSqr)).toList();
        final com.riftcompanions.hive.control.WillControlBudget.Selection selection =
                com.riftcompanions.hive.control.WillControlBudget.select(candidates,
                        com.riftcompanions.hive.control.HiveControlMode.STAGGER, false);
        if (!selection.hasTargets()) {
            return AbilityResult.failure(selection.immuneCount() > 0 ? "WILL_DISRUPT_TARGET_IMMUNE" : "NO_VISIBLE_WILL_CONTROL_TARGET");
        }
        final com.riftcompanions.hive.control.WillControlBudget.ControlTarget selected = selection.targets().get(0);
        final net.minecraft.world.entity.Mob target = selected.target();
        if (!isActiveCombatContext(owner, target)) {
            return AbilityResult.failure("WILL_DISRUPT_REQUIRES_COMBAT_CONTEXT");
        }
        if (isSensitiveArea(owner)) {
            return AbilityResult.failure("WILL_DISRUPT_BLOCKED_NEAR_SENSITIVE_AREA");
        }
        final float energy = selection.energyCost(18.0F);
        final float strain = selection.strainCost(25.0F);
        if (seer.getEnergy() < energy || seer.getHiveStrain() + strain > 100.0F) {
            DialogueService.get().speak(seer, "hive_strain", 2);
            return AbilityResult.failure("SEER_ENERGY_OR_STRAIN_LIMIT");
        }
        final com.riftcompanions.combat.EncounterProfile profile = com.riftcompanions.combat.EncounterAdapterRegistry.profileFor(target);
        final int baseDuration = profile.known() ? Math.max(1, profile.disruptTicks()) : 60;
        final int duration = Math.max(8, Math.round(baseDuration * selection.durationScaleFor(selected)));
        final int amplifier = selected.resisted() ? 0 : (profile.known() ? profile.disruptAmplifier() : 2);
        if (!seer.consumeEnergy(energy) || !seer.addHiveStrain(strain)) {
            return AbilityResult.failure("SEER_ENERGY_OR_STRAIN_LIMIT");
        }
        seer.setAbilityCooldown(CompanionAbility.SEER_DISRUPT, 200L);
        seer.beginVisualAction(CompanionAction.SEER_RELEASE, 14);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier, false, true, true));
        target.setDeltaMovement(Vec3.ZERO);
        owner.serverLevel().sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(), effectCount(selected.resisted() ? 8 : 18), 0.25D, 0.35D, 0.25D, 0.02D);
        DialogueService.get().speak(seer, "hive_disrupt", 1);
        return AbilityResult.success(selected.resisted() ? "WILL_DISRUPT_PARTIAL_RESISTANCE" : "WILL_DISRUPT_APPLIED", seer);
    }

    private static AbilityResult castHiveChannel(final ServerPlayer owner, final CompanionEntity seer, final com.riftcompanions.hive.control.HiveControlMode mode) {
        com.riftcompanions.hive.control.HiveChannelManager.ChannelResult result =
                com.riftcompanions.hive.control.HiveChannelManager.start(owner, seer, mode);
        return result.successful() ? AbilityResult.success(result.code(), seer) : AbilityResult.failure(result.code());
    }

    private static AbilityResult castHiveSurge(final ServerPlayer owner, final CompanionEntity seer, final com.riftcompanions.hive.control.HiveControlMode mode) {
        com.riftcompanions.hive.control.HiveChannelManager.ChannelResult result =
                com.riftcompanions.hive.control.HiveChannelManager.startSurge(owner, seer, mode);
        return result.successful() ? AbilityResult.success(result.code(), seer) : AbilityResult.failure(result.code());
    }

    /** Guardian Brace is an explicit close-range mitigation window, never a taunt or invulnerability effect. */
    private static AbilityResult castGuardianBrace(final ServerPlayer owner, final CompanionEntity guardian) {
        final GuardianBraceService.BraceResult result = GuardianBraceService.activate(owner, guardian);
        if (!result.successful()) return AbilityResult.failure(result.code());
        DialogueService.get().speak(guardian, "guardian_brace", 1);
        return AbilityResult.success(result.code(), guardian);
    }

    private static AbilityResult castGiftedPush(final ServerPlayer owner, final CompanionEntity gifted) {
        if (gifted.getEnergy() < 24.0F) {
            return AbilityResult.failure("GIFTED_ENERGY_LOW");
        }
        final ServerLevel level = owner.serverLevel();
        final List<Monster> targets = level.getEntitiesOfClass(Monster.class, gifted.getBoundingBox().inflate(32.0D),
                target -> !target.getType().is(ModTags.PROTECTED_FROM_COMPANIONS))
                .stream().sorted(Comparator.comparingDouble(gifted::distanceToSqr)).limit(16).toList();
        if (targets.isEmpty()) {
            return AbilityResult.failure("NO_SAFE_PUSH_TARGET");
        }
        if (isSensitiveArea(owner)) {
            return AbilityResult.failure("PUSH_BLOCKED_NEAR_SENSITIVE_AREA");
        }
        if (targets.stream().noneMatch(target -> isActiveCombatContext(owner, target))) {
            return AbilityResult.failure("PUSH_REQUIRES_COMBAT_CONTEXT");
        }
        // Push changes monster velocity, so it is rejected when a protected
        // non-target is in the immediate impact boundary. No player, villager,
        // animal, pet, or companion can become accidental push collateral.
        if (hasProtectedBystanderNear(level, owner, gifted, targets)) {
            return AbilityResult.failure("PUSH_BLOCKED_NEAR_PROTECTED_BYSTANDER");
        }
        if (!gifted.consumeEnergy(24.0F)) {
            return AbilityResult.failure("GIFTED_ENERGY_LOW");
        }
        gifted.setAbilityCooldown(CompanionAbility.GIFTED_PUSH, 110L);
        gifted.beginVisualAction(CompanionAction.GIFTED_PUSH, 12);
        for (final Monster target : targets) {
            Vec3 impulse = target.position().subtract(owner.position());
            if (impulse.lengthSqr() < 0.01D) {
                impulse = target.position().subtract(gifted.position());
            }
            final float pushMultiplier = com.riftcompanions.combat.EncounterAdapterRegistry.profileFor(target).pushMultiplier();
            impulse = impulse.normalize().scale(1.30D * pushMultiplier).add(0.0D, 1.45D * pushMultiplier, 0.0D);
            if (pushMultiplier > 0.0F) {
                target.setDeltaMovement(target.getDeltaMovement().add(impulse));
                target.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.5D, target.getZ(), effectCount(5), 0.20D, 0.35D, 0.20D, 0.02D);
        }
        DialogueService.get().speak(gifted, "push_success", 1);
        return AbilityResult.success("PUSH_APPLIED_" + targets.size(), gifted);
    }

    private static AbilityResult castGiftedShield(final ServerPlayer owner, final CompanionEntity gifted) {
        if (gifted.distanceToSqr(owner) > 12.0D * 12.0D) {
            return AbilityResult.failure("SHIELD_TARGET_OUT_OF_RANGE");
        }
        final ServerLevel level = owner.serverLevel();
        // Shield remains owner-only. A protected boundary only changes the
        // diagnostic result; it never spreads the effect to bystanders.
        final boolean protectedBoundary = hasProtectedBystanderNear(level, owner, gifted, List.of(owner));
        if (!gifted.consumeEnergy(20.0F)) {
            return AbilityResult.failure("GIFTED_ENERGY_LOW");
        }
        gifted.setAbilityCooldown(CompanionAbility.GIFTED_SHIELD, 220L);
        gifted.enableShield(90);
        level.sendParticles(ParticleTypes.ENCHANT, owner.getX(), owner.getY() + 1.0D, owner.getZ(), effectCount(30), 0.7D, 0.9D, 0.7D, 0.05D);
        DialogueService.get().speak(gifted, "shield_active", 1);
        return AbilityResult.success(protectedBoundary ? "SHIELD_ACTIVE_OWNER_ONLY_PROTECTED_BOUNDARY" : "SHIELD_ACTIVE", gifted);
    }

    private static AbilityResult castGiftedRescue(final ServerPlayer owner, final CompanionEntity gifted) {
        final boolean playerEmergency = owner.isInLava() || owner.isOnFire() || owner.fallDistance > 3.0F || owner.getHealth() <= 6.0F;
        final Optional<CompanionEntity> downedCompanion = java.util.Arrays.stream(com.riftcompanions.entity.CompanionRole.values())
                .map(role -> CompanionLifecycleService.findForOwner(owner, role))
                .flatMap(Optional::stream)
                .filter(companion -> companion.getCompanionState() == com.riftcompanions.entity.CompanionState.DOWNED)
                .min(Comparator.comparingDouble(gifted::distanceToSqr));
        if (!playerEmergency && downedCompanion.isEmpty()) {
            return AbilityResult.failure("RESCUE_REQUIRES_IMMEDIATE_DANGER");
        }
        final Entity target = playerEmergency ? owner : downedCompanion.orElseThrow();
        if (gifted.distanceToSqr(target) > 12.0D * 12.0D) {
            return AbilityResult.failure("RESCUE_TARGET_TOO_FAR");
        }
        if (!gifted.consumeEnergy(32.0F)) {
            return AbilityResult.failure("GIFTED_ENERGY_LOW");
        }
        final boolean rescued = SafeTeleport.rescueToCompanion(target, gifted, 4);
        if (!rescued) {
            gifted.setEnergy(Math.min(100.0F, gifted.getEnergy() + 24.0F));
            return AbilityResult.failure("NO_SAFE_RESCUE_DESTINATION");
        }
        if (target instanceof ServerPlayer playerTarget) {
            playerTarget.fallDistance = 0.0F;
            playerTarget.setDeltaMovement(Vec3.ZERO);
        }
        gifted.setAbilityCooldown(CompanionAbility.GIFTED_RESCUE, 320L);
        gifted.beginVisualAction(CompanionAction.GIFTED_RESCUE, 16);
        owner.serverLevel().sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0D, target.getZ(), effectCount(24), 0.35D, 0.65D, 0.35D, 0.08D);
        DialogueService.get().speak(gifted, "rescue_success", 1);
        return AbilityResult.success(playerEmergency ? "EMERGENCY_RESCUE_SUCCEEDED" : "DOWNED_COMPANION_REPOSITIONED", gifted);
    }

    private static AbilityResult castScoutRoute(final ServerPlayer owner, final CompanionEntity scout) {
        final com.riftcompanions.behavior.RoleBehaviorService.ScoutDecision decision = com.riftcompanions.behavior.RoleBehaviorService.evaluateScout(owner);
        if (!decision.allowed()) {
            DialogueService.get().speak(scout, "scout_refused", 2);
            return AbilityResult.failure(decision.code());
        }
        if (!scout.consumeEnergy(12.0F)) {
            return AbilityResult.failure("SCOUT_ENERGY_LOW");
        }
        final Optional<BlockPos> viewpoint = findLoadedSafeViewpoint(owner.serverLevel(), scout, owner.blockPosition());
        if (viewpoint.isEmpty()) {
            scout.setEnergy(Math.min(100.0F, scout.getEnergy() + 10.0F));
            return AbilityResult.failure("NO_LOADED_SAFE_SCOUT_ROUTE");
        }
        if (!scout.beginScout(viewpoint.get())) {
            scout.setEnergy(Math.min(100.0F, scout.getEnergy() + 10.0F));
            return AbilityResult.failure("SCOUT_START_REJECTED");
        }
        scout.setAbilityCooldown(CompanionAbility.SCOUT_ROUTE, 180L);
        DialogueService.get().speak(scout, "scout_start", 1);
        return AbilityResult.success("SCOUT_ROUTE_STARTED", scout);
    }

    /**
     * Scout Signal is a non-damaging, temporary visual mark for up to three
     * active visible hostile targets. It does not reveal through walls, affect
     * protected entities, add loot, or create a projectile/item farm.
     */
    private static AbilityResult castScoutSignal(final ServerPlayer owner, final CompanionEntity scout) {
        final List<Monster> targets = owner.serverLevel().getEntitiesOfClass(Monster.class, scout.getBoundingBox().inflate(16.0D),
                target -> target.isAlive() && !target.getType().is(ModTags.PROTECTED_FROM_COMPANIONS)
                        && scout.hasLineOfSight(target) && isActiveCombatContext(owner, target))
                .stream().sorted(Comparator.comparingDouble(scout::distanceToSqr)).limit(3).toList();
        if (targets.isEmpty()) return AbilityResult.failure("SCOUT_SIGNAL_REQUIRES_VISIBLE_THREAT");
        if (!scout.consumeFocus(18.0F)) return AbilityResult.failure("SCOUT_FOCUS_LOW");
        scout.setAbilityCooldown(CompanionAbility.SCOUT_SIGNAL, 180L);
        scout.beginVisualAction(CompanionAction.SCOUT_SIGNAL, 14L);
        for (final Monster target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false, true));
            owner.serverLevel().sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(), effectCount(6), 0.20D, 0.35D, 0.20D, 0.01D);
        }
        DialogueService.get().speak(scout, "scout_signal", 1);
        return AbilityResult.success("SCOUT_SIGNAL_MARKED_" + targets.size(), scout);
    }

    /**
     * Max's Mind Anchor is deliberately grounded: it clears supported mental
     * effects from the player, has no monster control, and is useless for farms.
     */
    /** Max Grounding remains the low-cost supported-effect action. */
    private static AbilityResult castScoutGrounding(final ServerPlayer owner, final CompanionEntity scout) {
        final MindAnchorService.Result result = MindAnchorService.grounding(owner, scout);
        if (!result.successful()) {
            if ("SCOUT_FOCUS_LOW".equals(result.code())) DialogueService.get().speak(scout, "focus_low", 2);
            else if ("NO_SUPPORTED_MENTAL_EFFECT".equals(result.code())) DialogueService.get().speak(scout, "mind_anchor_no_effect", 3);
            return AbilityResult.failure(result.code());
        }
        DialogueService.get().speak(scout, "grounding_success", 1);
        return AbilityResult.success(result.code(), scout);
    }

    private static AbilityResult castMindAnchorPoint(final ServerPlayer owner, final CompanionEntity scout) {
        final MindAnchorService.Result result = MindAnchorService.createAnchor(owner, scout);
        if (!result.successful()) return AbilityResult.failure(result.code());
        DialogueService.get().speak(scout, "mind_anchor_point", 1);
        return AbilityResult.success(result.code(), scout);
    }

    private static AbilityResult castMindAnchorBreakFree(final ServerPlayer owner, final CompanionEntity scout) {
        final MindAnchorService.Result result = MindAnchorService.breakFree(owner, scout);
        if (!result.successful()) return AbilityResult.failure(result.code());
        DialogueService.get().speak(scout, "mind_anchor_break_free", 1);
        return AbilityResult.success(result.code(), scout);
    }

    private static AbilityResult castMindAnchorEscapeWindow(final ServerPlayer owner, final CompanionEntity scout) {
        final MindAnchorService.Result result = MindAnchorService.escapeWindow(owner, scout);
        if (!result.successful()) return AbilityResult.failure(result.code());
        DialogueService.get().speak(scout, "mind_anchor_escape", 1);
        return AbilityResult.success(result.code(), scout);
    }


    /** Only samples a small set of already loaded columns; it never forces a chunk load. */
    private static Optional<BlockPos> findLoadedSafeViewpoint(final ServerLevel level, final CompanionEntity scout, final BlockPos origin) {
        BlockPos best = null;
        int bestHeight = Integer.MIN_VALUE;
        final int[][] offsets = {{16,0}, {-16,0}, {0,16}, {0,-16}, {12,12}, {-12,12}, {12,-12}, {-12,-12}, {24,0}, {-24,0}, {0,24}, {0,-24}};
        final int radius = CompanionConfig.MAX_SCOUT_DEFAULT_RADIUS.get();
        for (final int[] offset : offsets) {
            if (offset[0] * offset[0] + offset[1] * offset[1] > radius * radius) continue;
            final BlockPos sample = origin.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(sample)) {
                continue;
            }
            final int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample.getX(), sample.getZ());
            final BlockPos candidate = new BlockPos(sample.getX(), groundY, sample.getZ());
            if (SafeTeleport.isSafeStanding(level, scout, candidate) && candidate.getY() > bestHeight) {
                best = candidate;
                bestHeight = candidate.getY();
            }
        }
        return Optional.ofNullable(best);
    }

    private static int effectCount(final int normal) {
        return com.riftcompanions.config.CompanionConfig.LOW_EFFECTS.get() ? Math.max(1, normal / 4) : normal;
    }

    private static boolean isSensitiveArea(final ServerPlayer owner) {
        return TeamSavedData.get(owner.server).blackboard(owner.getUUID())
                .hasSensitiveAnnotation(owner.level().dimension().location(), owner.blockPosition(), 10.0D);
    }

    /**
     * Bounded local bystander protection for velocity-changing powers. It uses
     * only loaded nearby entities and never treats a generic monster as a
     * protected target. The active owner and casting Gifted are excluded from
     * the boundary so a player can still ask for a defensive escape opening.
     */
    private static boolean hasProtectedBystanderNear(final ServerLevel level, final ServerPlayer owner,
                                                      final CompanionEntity actor,
                                                      final List<? extends LivingEntity> impactCenters) {
        if (level == null || owner == null || actor == null || impactCenters == null) return true;
        for (final LivingEntity center : impactCenters) {
            if (center == null) continue;
            final boolean protectedNearby = !level.getEntitiesOfClass(LivingEntity.class,
                    center.getBoundingBox().inflate(2.5D), entity -> entity.isAlive() && entity != center)
                    .stream().filter(entity -> entity != actor)
                    .filter(entity -> !(entity instanceof Player player && player.getUUID().equals(owner.getUUID())))
                    .filter(AbilityService::isProtectedBystander)
                    .toList().isEmpty();
            if (protectedNearby) return true;
        }
        return false;
    }

    private static boolean isProtectedBystander(final LivingEntity entity) {
        return entity instanceof CompanionEntity || entity instanceof Villager || entity instanceof Animal
                || entity.getType().is(ModTags.PROTECTED_FROM_COMPANIONS);
    }

    /** Prevents powers from becoming passive mob-farm automation. */
    private static boolean isActiveCombatContext(final ServerPlayer owner, final net.minecraft.world.entity.Mob target) {
        return com.riftcompanions.hive.control.WillControlEligibility.isActiveThreat(owner, target);
    }

    private static String cardinalDirection(final Vec3 direction) {
        if (Math.abs(direction.x) >= Math.abs(direction.z)) {
            return direction.x >= 0.0D ? "EAST" : "WEST";
        }
        return direction.z >= 0.0D ? "SOUTH" : "NORTH";
    }

    public record AbilityResult(boolean successful, String code, CompanionEntity companion) {
        public static AbilityResult success(final String code, final CompanionEntity companion) {
            return new AbilityResult(true, code, companion);
        }

        public static AbilityResult failure(final String code) {
            return new AbilityResult(false, code, null);
        }
    }
}
