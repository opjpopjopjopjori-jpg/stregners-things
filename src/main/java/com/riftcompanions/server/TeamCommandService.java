package com.riftcompanions.server;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionAbility;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.network.CompanionCommand;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.team.TeamPlanService;
import com.riftcompanions.safety.SafeModeReason;
import com.riftcompanions.safety.SafeModeService;
import com.riftcompanions.intention.IntentionService;
import com.riftcompanions.policy.PlayerPolicyService;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Canonical server command gateway shared by HUD/UI networking. It validates
 * every enum ID and performs no arbitrary client-supplied world operation.
 */
public final class TeamCommandService {
    private TeamCommandService() {}

    public static CommandResult execute(final ServerPlayer player, final int commandId, final int roleId, final int abilityId) {
        if (player == null || player.server == null) {
            return CommandResult.failure("NO_SERVER", "No logical server is available to execute this command.");
        }
        if (!player.server.isSingleplayer()) {
            return CommandResult.failure("SINGLEPLAYER_ONLY", "This edition is designed for single-player worlds only.");
        }
        final Optional<CompanionCommand> command = CompanionCommand.byNetworkId(commandId);
        if (command.isEmpty()) {
            return CommandResult.failure("INVALID_COMMAND", "An unknown UI command was rejected.");
        }
        final Optional<CompanionRole> role = roleById(roleId);
        final Optional<CompanionAbility> ability = abilityById(abilityId);

        return switch (command.get()) {
            case CALL_ROLE -> role.map(value -> callRole(player, value)).orElseGet(() -> missing("ROLE_REQUIRED"));
            case FOLLOW_ALL -> followAll(player);
            case REGROUP -> regroup(player);
            case HOLD_ROLE -> role.map(value -> holdRole(player, value)).orElseGet(() -> missing("ROLE_REQUIRED"));
            case GUARD_ROLE -> role.map(value -> setRoleState(player, value, CompanionState.GUARDING, "UI_GUARD_ORDER")).orElseGet(() -> missing("ROLE_REQUIRED"));
            case FOCUS_TARGET -> focusTarget(player);
            case RETREAT -> TeamPlanService.beginRetreat(player, "PLAYER_UI_RETREAT")
                    ? CommandResult.success("RETREAT_PLAN_ACTIVE", "An organized team retreat has started.")
                    : CommandResult.failure("RETREAT_NOT_STARTED", "No loaded companion can start the retreat, or a retreat is already active.");
            case ACCEPT_PLAN -> planResult(TeamPlanService.acceptCurrentPlan(player));
            case DECLINE_PLAN -> planResult(TeamPlanService.declineCurrentPlan(player));
            case CHECK_STRUCTURE -> planResult(TeamDirector.requestStructureEntry(player));
            case CANCEL_PLAN -> {
                TeamPlanService.cancel(player);
                yield CommandResult.success("PLAN_CANCELLED", "The current plan was cancelled and the team returned to follow mode.");
            }
            case RECALL_ROLE -> role.map(value -> recallRole(player, value)).orElseGet(() -> missing("ROLE_REQUIRED"));
            case RECALL_ALL -> recallAll(player);
            case DISMISS_ROLE -> role.map(value -> dismissRole(player, value)).orElseGet(() -> missing("ROLE_REQUIRED"));
            case COMBAT_ON -> setCombat(player, true);
            case COMBAT_OFF -> setCombat(player, false);
            case SAFE_MODE -> safeMode(player);
            case CLEAR_SAFE_MODE -> clearSafeMode(player);
            case STOP_ALL_ACTIONS -> safetyResult(TeamSafetyControlService.stopAllActions(player));
            case RESET_TASK -> role.map(value -> safetyResult(TeamSafetyControlService.resetTask(player, value))).orElseGet(() -> missing("ROLE_REQUIRED"));
            case DISMISS_ALL -> safetyResult(TeamSafetyControlService.dismissAllExisting(player));
            case ACCEPT_INTENTION -> intentionResult(IntentionService.accept(player));
            case DEFER_INTENTION -> intentionResult(IntentionService.defer(player));
            case CYCLE_POWER_POLICY -> role.map(value -> policyResult(PlayerPolicyService.cyclePowerPolicy(player, value))).orElseGet(() -> missing("ROLE_REQUIRED"));
            case RESET_POWER_POLICY -> role.map(value -> policyResult(PlayerPolicyService.resetPowerPolicy(player, value))).orElseGet(() -> missing("ROLE_REQUIRED"));
            case SET_HOME_ANCHOR -> setHomeAnchor(player);
            case RETURN_HOME -> returnHome(player);
            case REVIVE_NEAREST -> reviveNearest(player);
            case USE_ABILITY -> ability.map(value -> useAbility(player, value)).orElseGet(() -> missing("ABILITY_REQUIRED"));
        };
    }

    private static CommandResult planResult(final TeamPlanService.PlanResult result) {
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult intentionResult(final IntentionService.IntentionResult result) {
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult policyResult(final PlayerPolicyService.PolicyResult result) {
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult safetyResult(final TeamSafetyControlService.Result result) {
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult focusTarget(final ServerPlayer player) {
        final FocusTargetService.FocusResult result = FocusTargetService.focusLookTarget(player);
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult callRole(final ServerPlayer player, final CompanionRole role) {
        final var requested = TeamSavedData.get(player.server).blackboard(player.getUUID()).requestedPair();
        if (requested.isPresent() && !requested.get().includes(role)) {
            return CommandResult.failure("ROLE_NOT_SELECTED_FOR_REQUESTED_DUO", "This role is not in the requested active duo. Change the duo at a safe anchor first.");
        }
        final CompanionLifecycleService.LifecycleResult result = CompanionLifecycleService.summonOrRecall(player, role);
        return result.successful()
                ? CommandResult.success(result.code(), role.personalName() + " is ready with the team.")
                : CommandResult.failure(result.code(), role.personalName() + ": call/recall could not be completed safely.");
    }

    private static CommandResult followAll(final ServerPlayer player) {
        int count = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final Optional<CompanionEntity> companion = CompanionLifecycleService.findForOwner(player, role);
            if (companion.isPresent()) {
                companion.get().setCompanionState(CompanionState.FOLLOWING, "UI_FOLLOW_ALL");
                count++;
            }
        }
        return count > 0 ? CommandResult.success("FOLLOW_ALL", "Returned " + count + " companions to follow mode.")
                : CommandResult.failure("NO_LOADED_COMPANIONS", "No companion is currently loaded.");
    }

    private static CommandResult regroup(final ServerPlayer player) {
        final CommandResult result = followAll(player);
        if (!result.success()) {
            return result;
        }
        CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                .ifPresent(companion -> com.riftcompanions.dialogue.DialogueService.get().speak(companion, "regroup", 1));
        return CommandResult.success("REGROUP", "The team is regrouping into a close, safe formation.");
    }

    private static CommandResult holdRole(final ServerPlayer player, final CompanionRole role) {
        final Optional<CompanionEntity> companion = CompanionLifecycleService.findForOwner(player, role);
        if (companion.isEmpty()) {
            return CommandResult.failure("NO_LOADED_COMPANION", role.personalName() + " is not currently loaded.");
        }
        if (!SafeTeleport.isSafeStanding(player.serverLevel(), companion.get(), companion.get().blockPosition())) {
            return CommandResult.failure("HOLD_LOCATION_UNSAFE", "A companion cannot be left in an unsafe location.");
        }
        companion.get().setCompanionState(CompanionState.HOLDING, "UI_HOLD_ORDER");
        return CommandResult.success("HOLDING", role.personalName() + " will hold a safe position.");
    }

    private static CommandResult setRoleState(final ServerPlayer player, final CompanionRole role, final CompanionState state, final String reason) {
        final Optional<CompanionEntity> companion = CompanionLifecycleService.findForOwner(player, role);
        if (companion.isEmpty()) {
            return CommandResult.failure("NO_LOADED_COMPANION", role.personalName() + " is not currently loaded.");
        }
        companion.get().setCompanionState(state, reason);
        return CommandResult.success(state.name(), role.personalName() + " changed state to " + state.name() + ".");
    }

    private static CommandResult recallRole(final ServerPlayer player, final CompanionRole role) {
        final CompanionLifecycleService.LifecycleResult result = CompanionLifecycleService.recall(player, role);
        return result.successful() ? CommandResult.success(result.code(), role.personalName() + " returned to a safe position.")
                : CommandResult.failure(result.code(), role.personalName() + ": no safe recall position is available.");
    }

    private static CommandResult recallAll(final ServerPlayer player) {
        if (!CompanionConfig.SAFE_RECALL_ENABLED.get()) {
            return CommandResult.failure("SAFE_RECALL_DISABLED", "Safe Recall is disabled in the world settings.");
        }
        int recalled = 0;
        int blocked = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionLifecycleService.LifecycleResult result = CompanionLifecycleService.recall(player, role);
            if (result.successful()) {
                recalled++;
            } else if (!"NO_LOADED_COMPANION".equals(result.code())) {
                blocked++;
            }
        }
        return recalled > 0 ? CommandResult.success("RECALL_ALL", "Safely recalled " + recalled + " companions" + (blocked > 0 ? "; rejected " + blocked + " unsafe location(s)." : "."))
                : CommandResult.failure("NO_SAFE_RECALL", "No companion can be recalled, or no safe ground is available.");
    }

    private static CommandResult dismissRole(final ServerPlayer player, final CompanionRole role) {
        final CompanionLifecycleService.LifecycleResult result = CompanionLifecycleService.dismiss(player, role);
        return CommandResult.success(result.code(), role.personalName() + " is temporarily outside the active team.");
    }

    private static CommandResult setCombat(final ServerPlayer player, final boolean enabled) {
        if (enabled && SafeModeService.enabled(player)) {
            return CommandResult.failure("SAFE_MODE_COMBAT_DISABLED", "Clear Safe Mode before enabling automatic combat.");
        }
        int count = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final Optional<CompanionEntity> companion = CompanionLifecycleService.findForOwner(player, role);
            if (companion.isPresent()) {
                companion.get().setCombatEnabled(enabled);
                count++;
            }
        }
        return count > 0 ? CommandResult.success(enabled ? "COMBAT_ON" : "COMBAT_OFF", enabled ? "Defensive combat is enabled." : "Automatic combat is disabled; safe rescue remains available.")
                : CommandResult.failure("NO_LOADED_COMPANIONS", "No companion is loaded.");
    }

    private static CommandResult safeMode(final ServerPlayer player) {
        final SafeModeService.SafeModeResult result = SafeModeService.enable(player, SafeModeReason.MANUAL,
                "Player enabled Safe Mode from the Team Journal.");
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult clearSafeMode(final ServerPlayer player) {
        final SafeModeService.SafeModeResult result = SafeModeService.clear(player);
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult setHomeAnchor(final ServerPlayer player) {
        final BaseAnchorService.AnchorResult result = BaseAnchorService.setAnchor(player, com.riftcompanions.world.BaseAnchorType.HOME);
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult returnHome(final ServerPlayer player) {
        final BaseAnchorService.AnchorResult result = BaseAnchorService.returnHome(player);
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult reviveNearest(final ServerPlayer player) {
        final RescueService.RescueResult result = RescueService.beginNearestPlayerRescue(player);
        return result.successful() ? CommandResult.success(result.code(), result.detail())
                : CommandResult.failure(result.code(), result.detail());
    }

    private static CommandResult useAbility(final ServerPlayer player, final CompanionAbility ability) {
        if (!CompanionConfig.POWERS_ENABLED.get()) {
            return CommandResult.failure("POWERS_DISABLED", "Powers are disabled in the world settings.");
        }
        final AbilityService.AbilityResult result = AbilityService.cast(player, ability);
        return result.successful() ? CommandResult.success(result.code(), "Executed " + ability.id() + ".")
                : CommandResult.failure(result.code(), "Could not execute " + ability.id() + " under the current safety rules.");
    }

    private static Optional<CompanionRole> roleById(final int id) {
        final CompanionRole[] values = CompanionRole.values();
        return id >= 0 && id < values.length ? Optional.of(values[id]) : Optional.empty();
    }

    private static Optional<CompanionAbility> abilityById(final int id) {
        final CompanionAbility[] values = CompanionAbility.values();
        return id >= 0 && id < values.length ? Optional.of(values[id]) : Optional.empty();
    }

    private static CommandResult missing(final String code) {
        return CommandResult.failure(code, "This command needs a valid companion or ability selection.");
    }

    public record CommandResult(boolean success, String code, String detail) {
        public static CommandResult success(final String code, final String detail) {
            return new CommandResult(true, code, detail);
        }

        public static CommandResult failure(final String code, final String detail) {
            return new CommandResult(false, code, detail);
        }
    }
}
