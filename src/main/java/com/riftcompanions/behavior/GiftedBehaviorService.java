package com.riftcompanions.behavior;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionAbility;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.mode.PerformancePolicy;
import com.riftcompanions.policy.PlayerPolicyService;
import com.riftcompanions.power.PowerPolicy;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

/**
 * Policy-aware Gifted readiness and suggestion layer. This service can describe
 * a safe opportunity, but it never casts Push, Shield, or Rescue by itself.
 */
public final class GiftedBehaviorService {
    private static final long SUGGESTION_COOLDOWN_TICKS = 800L;

    private GiftedBehaviorService() {}

    public static void tick(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (player == null || board == null || player.server == null || !FeatureFlags.enabled(FeatureFlag.ELEVEN_POWERS)) return;
        if (now % PerformancePolicy.scaledInterval(40L) != 0L) return;
        final CompanionEntity gifted = CompanionLifecycleService.findForOwner(player, CompanionRole.GIFTED).orElse(null);
        if (gifted == null) return;

        final GiftedBehaviorState state = board.giftedBehavior();
        final GiftedReadinessBand readiness = readinessFor(gifted);
        final String reason = reasonFor(gifted, readiness);
        final boolean changed = state.update(readiness, reason, now);
        boolean changedPersistentState = changed;

        if (readiness == GiftedReadinessBand.DOWNED || readiness == GiftedReadinessBand.RECOVERING
                || readiness == GiftedReadinessBand.EXHAUSTED) {
            if (changed && state.canSuggest(now, SUGGESTION_COOLDOWN_TICKS)) {
                DialogueService.get().speak(gifted, "gifted_power_recovery", 2);
                state.markSuggestion("gifted_power_recovery", now);
                changedPersistentState = true;
            }
            if (changedPersistentState) TeamSavedData.get(player.server).markChanged();
            return;
        }

        final PowerPolicy policy = PlayerPolicyService.effectivePowerPolicy(player, CompanionRole.GIFTED);
        if (policy == PowerPolicy.OFF) {
            if (changedPersistentState) TeamSavedData.get(player.server).markChanged();
            return;
        }

        final boolean rescueOpening = hasDownedTeammate(player);
        final int nearbyHostiles = player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(32.0D), Monster::isAlive).size();
        final boolean immediateEmergency = player.isInLava() || player.isOnFire() || player.fallDistance > 3.0F
                || player.getHealth() <= player.getMaxHealth() * 0.30F || nearbyHostiles >= 4;
        final boolean shieldOpening = nearbyHostiles >= 3 || immediateEmergency;

        if (rescueOpening && policy.permitsEmergency() && gifted.isAbilityReady(CompanionAbility.GIFTED_RESCUE)
                && state.canSuggest(now, SUGGESTION_COOLDOWN_TICKS)) {
            DialogueService.get().speak(gifted, "gifted_ready_rescue", 1);
            state.markSuggestion("gifted_ready_rescue", now);
            changedPersistentState = true;
        } else if (shieldOpening && permitsShieldSuggestion(policy, immediateEmergency)
                && gifted.isAbilityReady(CompanionAbility.GIFTED_SHIELD)
                && state.canSuggest(now, SUGGESTION_COOLDOWN_TICKS)) {
            DialogueService.get().speak(gifted, "gifted_ready_shield", immediateEmergency ? 1 : 2);
            state.markSuggestion("gifted_ready_shield", now);
            changedPersistentState = true;
        }

        if (changedPersistentState) TeamSavedData.get(player.server).markChanged();
    }

    private static GiftedReadinessBand readinessFor(final CompanionEntity gifted) {
        if (gifted.getCompanionState() == CompanionState.DOWNED) return GiftedReadinessBand.DOWNED;
        if (gifted.getCompanionState() == CompanionState.RECOVERING || gifted.getCompanionState() == CompanionState.EXHAUSTED) {
            return GiftedReadinessBand.RECOVERING;
        }
        if (gifted.getEnergy() < 20.0F) return GiftedReadinessBand.EXHAUSTED;
        if (gifted.getEnergy() < 45.0F) return GiftedReadinessBand.LIMITED;
        if (!gifted.isAbilityReady(CompanionAbility.GIFTED_SHIELD) || !gifted.isAbilityReady(CompanionAbility.GIFTED_RESCUE)) {
            return GiftedReadinessBand.CAUTION;
        }
        return GiftedReadinessBand.READY;
    }

    private static String reasonFor(final CompanionEntity gifted, final GiftedReadinessBand band) {
        return switch (band) {
            case DOWNED -> "GIFTED_DOWNED";
            case RECOVERING -> "GIFTED_RECOVERY_STATE";
            case EXHAUSTED -> "GIFTED_ENERGY_CRITICAL";
            case LIMITED -> "GIFTED_ENERGY_LIMITED";
            case CAUTION -> "GIFTED_COOLDOWN_ACTIVE";
            case READY -> "GIFTED_READY_WITH_POLICY";
        };
    }

    private static boolean permitsShieldSuggestion(final PowerPolicy policy, final boolean immediateEmergency) {
        if (policy == PowerPolicy.RESCUE_ONLY || policy == PowerPolicy.OFF || policy == PowerPolicy.SENSE_ONLY) return false;
        return policy.permitsExplicitNonEmergency() || (policy == PowerPolicy.EMERGENCY_ONLY && immediateEmergency);
    }

    private static boolean hasDownedTeammate(final ServerPlayer player) {
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (companion != null && companion.getCompanionState() == CompanionState.DOWNED) return true;
        }
        return false;
    }
}
