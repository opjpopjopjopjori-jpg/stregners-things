package com.riftcompanions.duo;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.safety.SafeModeService;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.world.BaseAnchorType;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Server-only pair awareness. It supplies readable context and cooldown-bound
 * synergy notes; it cannot auto-cast powers, change damage, or replace a plan.
 */
public final class DuoDynamicsService {
    private static final long SYNERGY_COOLDOWN = 240L;

    private DuoDynamicsService() {}

    public static Optional<TeamPair> activePair(final ServerPlayer player) {
        final List<CompanionRole> active = new ArrayList<>();
        for (final CompanionRole role : CompanionRole.values()) {
            if (CompanionLifecycleService.findForOwner(player, role).isPresent()) active.add(role);
        }
        return active.size() == 2 ? TeamPair.of(active.get(0), active.get(1)) : Optional.empty();
    }

    public static String statusSummary(final ServerPlayer player) {
        return activePair(player).map(pair -> pair.identity() + " — " + pair.recommendedContexts())
                .orElse("Choose up to two companions at a safe HOME or REST anchor.");
    }

    public static DuoResult selectAtBase(final ServerPlayer player, final CompanionRole first, final CompanionRole second) {
        if (player == null || player.server == null || !player.server.isSingleplayer()) {
            return DuoResult.failure("SINGLEPLAYER_ONLY", "Active duo selection is available only in the offline single-player edition.");
        }
        if (!FeatureFlags.enabled(FeatureFlag.DUO_DYNAMICS)) {
            return DuoResult.failure("DUO_DYNAMICS_DISABLED", "Duo Dynamics is disabled by the world feature flag.");
        }
        if (SafeModeService.enabled(player)) {
            return DuoResult.failure("SAFE_MODE_SQUAD_SWITCH_DISABLED", "Clear Safe Mode before changing the active duo.");
        }
        final TeamPair pair = TeamPair.of(first, second).orElse(null);
        if (pair == null) return DuoResult.failure("INVALID_DUO_SELECTION", "Choose two different valid companion roles.");
        final TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        if (board.plan().isActive() || board.plan().awaitsApproval()) {
            return DuoResult.failure("SQUAD_SWITCH_PLAN_ACTIVE", "Finish or cancel the active plan before changing the active duo.");
        }
        if (hasDownedCompanion(player)) {
            return DuoResult.failure("SQUAD_SWITCH_DOWNED", "A downed companion must be resolved before changing the active duo.");
        }
        if (!isSafeRestLocation(player, board)) {
            return DuoResult.failure("SQUAD_SWITCH_REQUIRES_SAFE_ANCHOR", "Choose the active duo at a safe HOME or REST anchor without nearby hostiles.");
        }
        final TeamSavedData data = TeamSavedData.get(player.server);
        // Never dismiss an entity that is active but unavailable in another
        // loaded dimension/chunk. That would turn a selection click into an
        // unsafe duplicate or item-state guess.
        for (final CompanionRole role : List.of(first, second)) {
            if (data.activeEntityUuid(player.getUUID(), role).isPresent()
                    && CompanionLifecycleService.findForOwner(player, role).isEmpty()) {
                return DuoResult.failure("SQUAD_SWITCH_SELECTED_ROLE_UNLOADED", "A selected companion is active but not loaded. Return to the same safe area before switching.");
            }
        }
        final long now = player.level().getGameTime();
        final var transactionBegin = board.actionLedger().begin(ActionType.SQUAD_SWITCH,
                "squad:" + pair.name() + ":" + now, pair.name(), now, 240L);
        if (transactionBegin.reused()) return DuoResult.failure("SQUAD_SWITCH_ALREADY_PENDING", "This duo switch is already pending confirmation.");
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("SAFE_BASE_AND_DUO_VALIDATED");

        // Add missing selected roles one at a time. An outgoing companion is
        // moved into a server-owned RESTING snapshot only when it is needed to
        // free a slot; it cannot keep a chunk loaded or think off-screen.
        for (final CompanionRole wanted : List.of(first, second)) {
            if (CompanionLifecycleService.findForOwner(player, wanted).isPresent()) continue;
            final CompanionRole outgoing = activeNonSelected(player, first, second);
            if (outgoing != null) {
                final CompanionLifecycleService.LifecycleResult resting = CompanionLifecycleService.restAtBase(player, outgoing);
                if (!resting.successful()) {
                    transaction.rollback(now, "SQUAD_SWITCH_REST_FAILED_" + resting.code());
                    data.markChanged();
                    return DuoResult.failure("SQUAD_SWITCH_SAFE_FALLBACK", "The switch stopped safely: " + resting.code() + ". No companion state was copied.");
                }
            }
            final CompanionLifecycleService.LifecycleResult result = CompanionLifecycleService.summonOrRecall(player, wanted);
            if (!result.successful()) {
                transaction.rollback(now, "SQUAD_SWITCH_SAFE_FALLBACK_" + result.code());
                data.markChanged();
                return DuoResult.failure("SQUAD_SWITCH_SAFE_FALLBACK", "The switch stopped safely: " + result.code() + ". No items were copied; review the current loaded team.");
            }
        }
        // Rest any remaining role outside the desired pair only after both
        // selected roles are confirmed loaded.
        for (final CompanionRole role : CompanionRole.values()) {
            if (!pair.includes(role) && CompanionLifecycleService.findForOwner(player, role).isPresent()) {
                final CompanionLifecycleService.LifecycleResult resting = CompanionLifecycleService.restAtBase(player, role);
                if (!resting.successful()) {
                    transaction.rollback(now, "SQUAD_SWITCH_FINAL_REST_FAILED_" + resting.code());
                    data.markChanged();
                    return DuoResult.failure("SQUAD_SWITCH_SAFE_FALLBACK", "The selected duo is loaded, but a non-selected companion could not rest safely.");
                }
            }
        }
        if (CompanionLifecycleService.findForOwner(player, first).isEmpty() || CompanionLifecycleService.findForOwner(player, second).isEmpty()) {
            transaction.rollback(now, "SQUAD_SWITCH_POSTCONDITION_FAILED");
            data.markChanged();
            return DuoResult.failure("SQUAD_SWITCH_SAFE_FALLBACK", "The selected pair was not fully loaded. The team remains in a safe, player-controlled state.");
        }
        board.setRequestedPair(pair);
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("SELECTED_PAIR_LOADED");
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, "SQUAD_SWITCH_COMPLETED");
        data.markChanged();
        CompanionLifecycleService.findForOwner(player, pair.first()).ifPresent(companion -> DialogueService.get().speak(companion, pair.dialogueTrigger(), 3));
        CompanionLifecycleService.findForOwner(player, pair.second()).ifPresent(companion -> DialogueService.get().speak(companion, pair.dialogueTrigger(), 3));
        return DuoResult.success("DUO_SWITCHED", pair.identity() + " is active. Non-selected companions are resting safely with their bounded personal state preserved.");
    }

    public static void noteSynergy(final ServerPlayer player, final DuoSynergy synergy) {
        if (!FeatureFlags.enabled(FeatureFlag.DUO_DYNAMICS) || synergy == null || activePair(player).orElse(null) != synergy.pair()) return;
        final TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        final long now = player.level().getGameTime();
        if (!board.tryRecordSynergy(synergy.name(), now, SYNERGY_COOLDOWN)) return;
        board.addMemory(new MemoryRecord(MemoryType.DUO, now / 24000L, synergy.summary(), 72));
        TeamSavedData.get(player.server).markChanged();
    }

    private static CompanionRole activeNonSelected(final ServerPlayer player, final CompanionRole first, final CompanionRole second) {
        for (final CompanionRole role : CompanionRole.values()) {
            if (role != first && role != second && CompanionLifecycleService.findForOwner(player, role).isPresent()) return role;
        }
        return null;
    }

    private static boolean hasDownedCompanion(final ServerPlayer player) {
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (companion != null && companion.getCompanionState() == CompanionState.DOWNED) return true;
        }
        return false;
    }

    private static boolean isSafeRestLocation(final ServerPlayer player, final TeamBlackboard board) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        final boolean atHome = board.anchor(BaseAnchorType.HOME)
                .filter(anchor -> anchor.dimension().equals(level.dimension().location())
                        && player.blockPosition().distSqr(anchor.position()) <= 26.0D * 26.0D)
                .isPresent();
        final boolean atRest = board.anchor(BaseAnchorType.REST)
                .filter(anchor -> anchor.dimension().equals(level.dimension().location())
                        && player.blockPosition().distSqr(anchor.position()) <= 12.0D * 12.0D)
                .isPresent();
        return (atHome || atRest) && level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(12.0D)).isEmpty();
    }

    public record DuoResult(boolean successful, String code, String detail) {
        public static DuoResult success(final String code, final String detail) { return new DuoResult(true, code, detail); }
        public static DuoResult failure(final String code, final String detail) { return new DuoResult(false, code, detail); }
    }
}
