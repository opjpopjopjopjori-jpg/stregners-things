package com.riftcompanions.hive.control;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.registry.ModSounds;
import com.riftcompanions.server.CompanionLifecycleService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Focus/release pipeline for Will's bounded hostile control. The target list,
 * heart budget, and visible selection are server-owned; a lost or newly
 * resistant target cancels before a full release cost is charged.
 */
public final class HiveChannelManager {
    private static final double CONTROL_RANGE = 32.0D;
    private static final int MAX_CANDIDATE_SCAN = 32;
    private static final Map<UUID, HiveChannel> CHANNELS = new HashMap<>();

    private HiveChannelManager() {}

    public static ChannelResult start(final ServerPlayer player, final CompanionEntity seer, final HiveControlMode mode) {
        return startInternal(player, seer, mode, false);
    }

    /**
     * Rare explicit emergency entry point. Surge expands only the temporary
     * heart budget; it never bypasses target eligibility, protected-area,
     * line-of-sight, policy, boss-resistance, or recovery rules.
     */
    public static ChannelResult startSurge(final ServerPlayer player, final CompanionEntity seer, final HiveControlMode mode) {
        if (!CompanionConfig.HIVE_SURGE_ENABLED.get() || !FeatureFlags.enabled(FeatureFlag.WILL_HIVE_SURGE)) {
            return ChannelResult.failure("HIVE_SURGE_DISABLED", "Will Surge is disabled by the world feature flags.");
        }
        if (isSensitiveArea(player) || protectedLivingNearby(player)) {
            return ChannelResult.failure("HIVE_SURGE_BLOCKED_NEAR_PROTECTED_AREA", "Will Surge is blocked near a protected area, villager, or animal boundary.");
        }
        return startInternal(player, seer, mode, true);
    }

    private static ChannelResult startInternal(final ServerPlayer player, final CompanionEntity seer,
                                                final HiveControlMode mode, final boolean surge) {
        if (player == null || seer == null || mode == null) {
            return ChannelResult.failure("HIVE_CHANNEL_CONTEXT_INVALID", "Will control needs a valid Seer and mode.");
        }
        if (CHANNELS.containsKey(player.getUUID())) {
            return ChannelResult.failure("HIVE_CHANNEL_ALREADY_ACTIVE", "Will is already focusing on a control link.");
        }
        if (!surge && isSensitiveArea(player) && (mode == HiveControlMode.REDIRECT || mode == HiveControlMode.SHATTER)) {
            return ChannelResult.failure("HIVE_CONTROL_BLOCKED_NEAR_SENSITIVE_AREA", "That control mode is unsafe near a protected or machine area.");
        }

        final List<Mob> candidates = findTargets(player, seer);
        if (surge && !isSurgeEmergency(player, candidates)) {
            return ChannelResult.failure("HIVE_SURGE_REQUIRES_CRITICAL_CONTEXT", "Will Surge requires critical danger, a visible hostile crowd, or a resisted boss threat.");
        }
        final WillControlBudget.Selection selection = WillControlBudget.select(candidates, mode, surge);
        if (!selection.hasTargets()) {
            if (selection.immuneCount() > 0) {
                seer.beginVisualAction(CompanionAction.SEER_NOTICE, 12);
                play(player, seer, ModSounds.HIVE_RESIST.get());
                HiveLinkStatusService.send(player, null, mode, 0, "The target resists Will's link.");
                return ChannelResult.failure("WILL_CONTROL_TARGET_IMMUNE", "The target resists Will's link.");
            }
            return ChannelResult.failure("NO_VALID_WILL_CONTROL_TARGET", "No eligible hostile target is visible.");
        }

        final float energy = selection.energyCost(baseEnergyCost(mode, surge));
        final float strain = selection.strainCost(baseStrainCost(mode, surge));
        if (seer.getEnergy() < energy || seer.getHiveStrain() + strain > 100.0F) {
            return ChannelResult.failure("HIVE_STRAIN_OR_ENERGY_LIMIT", "Will does not have enough energy or safe strain capacity.");
        }

        final long channelTicks = surge ? CompanionConfig.HIVE_SURGE_CHANNEL_TICKS.get() : CompanionConfig.HIVE_CHANNEL_TICKS.get();
        final long releaseAt = player.level().getGameTime() + channelTicks;
        final List<UUID> ids = selection.targets().stream().map(target -> target.target().getUUID()).toList();
        CHANNELS.put(player.getUUID(), new HiveChannel(player.getUUID(), seer.getUUID(), mode, ids, releaseAt,
                selection.budget().capacityHearts(), selection.budget().targetLimit(), surge));
        seer.beginVisualAction(CompanionAction.SEER_FOCUS, channelTicks + 4L);
        play(player, seer, ModSounds.HIVE_NOTICE.get());
        play(player, seer, ModSounds.HIVE_FOCUS.get());
        sendSelectionStatus(player, selection.targets().get(0).target(), mode, (int) channelTicks, selection,
                surge ? "Will Surge focusing. Keep the selected targets in sight." : "Will control focus started.");
        return ChannelResult.success(surge ? "WILL_SURGE_FOCUS_STARTED" : "WILL_CONTROL_FOCUS_STARTED",
                surge ? "Will is focusing a Surge control link." : "Will is focusing a control link.");
    }

    public static void tick(final ServerPlayer player) {
        final HiveChannel channel = CHANNELS.get(player.getUUID());
        if (channel == null) return;
        final CompanionEntity seer = CompanionLifecycleService.findForOwner(player, CompanionRole.SEER).orElse(null);
        if (!CompanionConfig.POWERS_ENABLED.get() || !FeatureFlags.enabled(FeatureFlag.WILL_HIVE_LINK)
                || (channel.surge() && (!CompanionConfig.HIVE_SURGE_ENABLED.get() || !FeatureFlags.enabled(FeatureFlag.WILL_HIVE_SURGE)))) {
            if (seer != null) cancel(player, seer, "Power access changed; focus cancelled safely.");
            else CHANNELS.remove(player.getUUID());
            return;
        }
        if (seer == null || !seer.getUUID().equals(channel.seerUuid())) {
            CHANNELS.remove(player.getUUID());
            return;
        }

        final long now = player.level().getGameTime();
        final List<Mob> valid = resolveValidTargets(player, seer, channel.targetUuids());
        final WillControlBudget.Selection selection = WillControlBudget.select(valid, channel.mode(), channel.capturedBudget());
        if (!selection.hasTargets()) {
            cancel(player, seer, selection.immuneCount() > 0
                    ? "The selected target resisted. Focus cancelled without full cost."
                    : "Target lost. Focus cancelled without full cost.");
            return;
        }
        if (now < channel.releaseAt()) {
            sendSelectionStatus(player, selection.targets().get(0).target(), channel.mode(), (int) (channel.releaseAt() - now),
                    selection, "Focusing.");
            return;
        }

        CHANNELS.remove(player.getUUID());
        final float energy = selection.energyCost(baseEnergyCost(channel.mode(), channel.surge()));
        final float strain = selection.strainCost(baseStrainCost(channel.mode(), channel.surge()));
        if (seer.getEnergy() < energy || seer.getHiveStrain() + strain > 100.0F) {
            seer.beginVisualAction(CompanionAction.SEER_NOTICE, 10);
            HiveLinkStatusService.send(player, null, channel.mode(), 0, "The link failed safely.");
            return;
        }

        final float energyBefore = seer.getEnergy();
        final float strainBefore = seer.getHiveStrain();
        if (!seer.consumeEnergy(energy) || !seer.addHiveStrain(strain)) {
            seer.setEnergy(energyBefore);
            seer.setHiveStrain(strainBefore);
            seer.beginVisualAction(CompanionAction.SEER_NOTICE, 10);
            HiveLinkStatusService.send(player, null, channel.mode(), 0, "The link failed safely.");
            return;
        }

        int applied = 0;
        int resisted = 0;
        long longestDuration = 0L;
        for (final WillControlBudget.ControlTarget target : selection.targets()) {
            final long duration = scaledDuration(channel.mode(), channel.surge(), selection.durationScaleFor(target));
            if (HiveControlManager.apply(target.target(), target.resolvedMode(), duration, seer.position())) {
                applied++;
                longestDuration = Math.max(longestDuration, duration);
                if (target.resisted()) resisted++;
            }
        }
        if (applied == 0) {
            seer.setEnergy(energyBefore);
            seer.setHiveStrain(strainBefore);
            seer.beginVisualAction(CompanionAction.SEER_NOTICE, 10);
            HiveLinkStatusService.send(player, null, channel.mode(), 0, "No selected target accepted the release.");
            return;
        }

        seer.setAbilityCooldown(abilityFor(channel.mode(), channel.surge()),
                channel.surge() ? CompanionConfig.HIVE_SURGE_COOLDOWN_TICKS.get() : cooldownFor(channel.mode()));
        seer.beginVisualAction(releaseActionFor(channel.mode()), 16);
        if (channel.surge()) {
            seer.scheduleRecovery(14L, CompanionConfig.HIVE_SURGE_RECOVERY_TICKS.get(), "HIVE_SURGE_RECOVERY");
            com.riftcompanions.behavior.RoleBehaviorService.markWillStressed(player, CompanionConfig.HIVE_SURGE_RECOVERY_TICKS.get());
        } else if (seer.getHiveStrain() >= 70.0F) {
            seer.scheduleRecovery(14L, 160L, "HIVE_SURGE_STRAIN_RECOVERY");
        }
        play(player, seer, resisted > 0 ? ModSounds.HIVE_RESIST.get() : ModSounds.HIVE_RELEASE.get());
        HiveTeamReactionService.onApplied(player, seer, channel.mode(), selection.targets().get(0).target(), applied);
        final String message = resisted > 0
                ? "Control applied with resistance. Use the short opening."
                : channel.surge() ? "Will Surge applied. Move before the opening closes." : "Will control applied.";
        sendSelectionStatus(player, selection.targets().get(0).target(), channel.mode(), (int) longestDuration, selection, message);
    }

    public static void interrupt(final CompanionEntity seer, final float damage) {
        if (damage < 2.0F) return;
        final ServerPlayer owner = seer.getOwnerPlayer().orElse(null);
        if (owner == null) return;
        final HiveChannel channel = CHANNELS.get(owner.getUUID());
        if (channel != null && channel.seerUuid().equals(seer.getUUID())) cancel(owner, seer, "Focus interrupted.");
    }

    /** Safe Mode and recovery can cancel a focus before release, so no deferred cost is applied. */
    public static void cancelForSafety(final ServerPlayer player, final String message) {
        if (player == null) return;
        final HiveChannel channel = CHANNELS.remove(player.getUUID());
        if (channel == null) return;
        final CompanionEntity seer = CompanionLifecycleService.findForOwner(player, CompanionRole.SEER).orElse(null);
        if (seer != null) {
            seer.beginVisualAction(CompanionAction.SEER_NOTICE, 8);
            play(player, seer, ModSounds.HIVE_RECOVERY.get());
        }
        HiveLinkStatusService.send(player, null, null, 0, message == null ? "Focus cancelled safely." : message);
    }

    private static void cancel(final ServerPlayer player, final CompanionEntity seer, final String message) {
        CHANNELS.remove(player.getUUID());
        seer.beginVisualAction(CompanionAction.SEER_NOTICE, 8);
        play(player, seer, ModSounds.HIVE_RECOVERY.get());
        HiveLinkStatusService.send(player, null, null, 0, message);
    }

    private static List<Mob> findTargets(final ServerPlayer player, final CompanionEntity seer) {
        return player.level().getEntitiesOfClass(Mob.class, seer.getBoundingBox().inflate(CONTROL_RANGE),
                        target -> WillControlEligibility.isActiveThreat(player, target) && seer.hasLineOfSight(target)
                                && player.serverLevel().hasChunkAt(target.blockPosition()))
                .stream().sorted(Comparator.comparingDouble(seer::distanceToSqr)).limit(MAX_CANDIDATE_SCAN).toList();
    }

    private static List<Mob> resolveValidTargets(final ServerPlayer player, final CompanionEntity seer, final List<UUID> ids) {
        final List<Mob> result = new ArrayList<>();
        for (final UUID id : ids) {
            final var entity = player.serverLevel().getEntity(id);
            if (entity instanceof Mob target && WillControlEligibility.isActiveThreat(player, target)
                    && seer.hasLineOfSight(target) && seer.distanceToSqr(target) <= CONTROL_RANGE * CONTROL_RANGE
                    && player.serverLevel().hasChunkAt(target.blockPosition())) {
                result.add(target);
            }
        }
        result.sort(Comparator.comparingDouble(seer::distanceToSqr));
        return result;
    }

    private static boolean isSurgeEmergency(final ServerPlayer player, final List<Mob> targets) {
        if (player.isInLava() || player.isOnFire() || player.fallDistance > 3.0F
                || player.getHealth() <= player.getMaxHealth() * 0.30F) return true;
        final int crowdThreshold = Math.max(CompanionConfig.HIVE_SURGE_MIN_LINKED_TARGETS.get(),
                CompanionConfig.WILL_SURGE_MIN_HOSTILE_TARGETS.get());
        if (targets.size() >= crowdThreshold) return true;
        return targets.stream().anyMatch(target -> BossInteractionRegistry.resistance(target) != BossResistanceLevel.VULNERABLE_WINDOW);
    }

    private static boolean protectedLivingNearby(final ServerPlayer player) {
        final boolean villager = !player.level().getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,
                player.getBoundingBox().inflate(10.0D)).isEmpty();
        final boolean animal = !player.level().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                player.getBoundingBox().inflate(8.0D)).isEmpty();
        return villager || animal;
    }

    private static boolean isSensitiveArea(final ServerPlayer player) {
        return com.riftcompanions.server.TeamSavedData.get(player.server).blackboard(player.getUUID())
                .hasSensitiveAnnotation(player.level().dimension().location(), player.blockPosition(), 10.0D);
    }

    private static float baseEnergyCost(final HiveControlMode mode, final boolean surge) {
        if (surge) return CompanionConfig.HIVE_SURGE_ENERGY_COST.get().floatValue();
        return mode == HiveControlMode.SWARM_FREEZE ? 34.0F : 22.0F;
    }

    private static float baseStrainCost(final HiveControlMode mode, final boolean surge) {
        if (surge) return CompanionConfig.HIVE_SURGE_STRAIN_COST.get().floatValue();
        return mode == HiveControlMode.SWARM_FREEZE ? 38.0F : 26.0F;
    }

    private static long scaledDuration(final HiveControlMode requestedMode, final boolean surge, final float scale) {
        final long base = surge ? switch (requestedMode) {
            case SUSPEND -> 64L;
            case REDIRECT -> 58L;
            case SHATTER -> 48L;
            case SWARM_FREEZE -> 56L;
            case STAGGER -> 20L;
        } : switch (requestedMode) {
            case SUSPEND -> 50L;
            case REDIRECT -> 45L;
            case SHATTER -> 40L;
            case SWARM_FREEZE -> 45L;
            case STAGGER -> 20L;
        };
        return Math.max(1L, Math.round(base * Math.max(0.05F, Math.min(1.0F, scale))));
    }

    private static long cooldownFor(final HiveControlMode mode) {
        return mode == HiveControlMode.SWARM_FREEZE ? 360L : 220L;
    }

    private static CompanionAction releaseActionFor(final HiveControlMode mode) {
        return switch (mode) {
            case REDIRECT -> CompanionAction.SEER_RELEASE_REDIRECT;
            case SHATTER -> CompanionAction.SEER_RELEASE_SHATTER;
            case SUSPEND, SWARM_FREEZE, STAGGER -> CompanionAction.SEER_RELEASE;
        };
    }

    private static com.riftcompanions.entity.CompanionAbility abilityFor(final HiveControlMode mode, final boolean surge) {
        if (surge) {
            return switch (mode) {
                case SUSPEND -> com.riftcompanions.entity.CompanionAbility.SEER_SURGE_SUSPEND;
                case REDIRECT -> com.riftcompanions.entity.CompanionAbility.SEER_SURGE_REDIRECT;
                case SHATTER -> com.riftcompanions.entity.CompanionAbility.SEER_SURGE_SHATTER;
                case SWARM_FREEZE -> com.riftcompanions.entity.CompanionAbility.SEER_SURGE_SWARM_FREEZE;
                case STAGGER -> com.riftcompanions.entity.CompanionAbility.SEER_SURGE_SUSPEND;
            };
        }
        return switch (mode) {
            case SUSPEND -> com.riftcompanions.entity.CompanionAbility.SEER_SUSPEND;
            case REDIRECT -> com.riftcompanions.entity.CompanionAbility.SEER_REDIRECT;
            case SHATTER -> com.riftcompanions.entity.CompanionAbility.SEER_SHATTER;
            case SWARM_FREEZE -> com.riftcompanions.entity.CompanionAbility.SEER_SWARM_FREEZE;
            case STAGGER -> com.riftcompanions.entity.CompanionAbility.SEER_SUSPEND;
        };
    }

    private static void sendSelectionStatus(final ServerPlayer player, final Mob target, final HiveControlMode mode,
                                            final int ticks, final WillControlBudget.Selection selection,
                                            final String message) {
        HiveLinkStatusService.send(player, target, mode, ticks, selection.targets().size(), selection.budget().targetLimit(),
                selection.usedHearts(), selection.budget().capacityHearts(), message);
    }

    private static void play(final ServerPlayer player, final CompanionEntity seer, final net.minecraft.sounds.SoundEvent sound) {
        if (!CompanionConfig.HIVE_AUDIO_ENABLED.get()) return;
        player.serverLevel().playSound(null, seer.getX(), seer.getY(), seer.getZ(), sound, SoundSource.PLAYERS, 0.55F, 1.0F);
    }

    public record ChannelResult(boolean successful, String code, String detail) {
        public static ChannelResult success(final String code, final String detail) { return new ChannelResult(true, code, detail); }
        public static ChannelResult failure(final String code, final String detail) { return new ChannelResult(false, code, detail); }
    }
}
