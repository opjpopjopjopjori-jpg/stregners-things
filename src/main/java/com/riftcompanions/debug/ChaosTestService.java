package com.riftcompanions.debug;

import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Local-only scenario briefing. It intentionally does not spoof a passing test:
 * a Forge runtime operator still performs the setup and observes the result.
 */
public final class ChaosTestService {
    private ChaosTestService() {}

    public static ChaosBriefing briefing(final ServerPlayer player, final ChaosScenario scenario) {
        if (player == null || scenario == null) return new ChaosBriefing("INVALID", "No scenario is selected.", "No action taken.");
        final String expected = switch (scenario) {
            case LOGOUT_DURING_ACTIVE_PLAN -> "Reload cancels or safely resumes only a valid plan; no duplicate plan action id.";
            case PAUSE_DURING_COMBAT -> "A single-player pause creates no extra decision, damage, item, or path action; state remains coherent when ticks resume.";
            case EXIT_DURING_SAFE_RECALL -> "An incomplete recall rolls back or resolves once after reload; no duplicate companion or unsafe destination is created.";
            case SAFE_RECALL_DESTINATION_CONTENTION -> "A same-tick Recall All or spawn/recall burst assigns each companion a distinct loaded safe destination with living-entity clearance; a tight-space failure holds/rejects safely rather than stacking bodies.";
            case EXIT_DURING_DIMENSION_BOUNDARY -> "No companion crosses a portal automatically; reload keeps the companion in a safe existing state without a dimension clone.";
            case DIMENSION_CHANGE_DURING_SCOUT -> "Scout task cancels to FOLLOW or RESTING; no portal use and no forced chunk load.";
            case PLAYER_DEATH_DURING_RESCUE -> "Rescue reservation releases; companions hold or return through the existing safe fallback.";
            case COMPANION_STUCK_IN_LEAVES_WATER_OR_DOOR -> "Navigation retries are bounded, then Safe Recall or HOLD is offered without block breaking.";
            case VISIBLE_HOSTILE_RESPONSE_CONTENTION -> "One to five visible zombies target the player or active companion. Combat-capable companions clear formation slots, acquire bounded local targets, and fight/retreat without a frozen Follow path; dialogue is bounded and Defend remains a player-approved proposal.";
            case INVENTORY_FULL_DURING_TRANSFER -> "The transfer rolls back or leaves the source unchanged; no copied item exists.";
            case TARGET_DIES_DURING_SEER_DISRUPT -> "The server rejects the missing target cleanly and no extra effect/loot is produced.";
            case POWERS_DISABLED_DURING_GIFTED_FOCUS -> "Existing focus resolves only through its safety path; future casts are rejected.";
            case SAFE_WAYPOINT_DESTROYED -> "The invalid target is cancelled; navigation never assumes a replacement block exists.";
            case STRUCTURE_CHUNK_UNLOAD_DURING_MEMORY_CALLBACK -> "The memory callback records no unseen target and returns to a safe state.";
            case CHAT_FLOOD_THEN_ALERT -> "P0 alert remains readable while low-priority lines are bounded or delayed.";
            case DOCTRINE_CHANGE_DURING_ACTIVE_PLAN -> "The active plan keeps its captured safety state; future proposals use the new doctrine.";
            case DATAPACK_RELOAD_PROTECTS_WILL_TARGET -> "A release revalidates hostile eligibility and protected tags, then cancels before full cost if the selected target is now protected.";
            case RESTING_SNAPSHOT_SAFE_BASE_BOUNDARY -> "A RESTING snapshot restores only once at the original-dimension safe base and never acts as remote storage.";
            case REPEATED_COMPANION_FAULT -> "Repeated controlled companion faults trigger a readable circuit breaker and Safe Mode rather than a crash loop.";
            case CLOSED_DOOR_NAVIGATION -> "Closed doors are rejected from companion traversal; route around, hold, or Safe Recovery without door use.";
            case CLIFF_DROP_REJECTION -> "A path node above the configured safe drop is rejected before a companion deliberately descends the cliff.";
            case FORMATION_SLOT_CONTENTION -> "Two companions keep distinct expiring formation reservations instead of piling onto one candidate square.";
            case RESET_TASK_DURING_NAVIGATION -> "Reset Task cancels the local path, clears focus, and reaches FOLLOW, Safe Recall, or safe HOLD without a phantom success.";
            case GIFTED_PUSH_PROTECTED_BYSTANDER -> "Push rejects near a protected villager, animal, pet, player, or companion boundary and spends no power cost.";
            case PERFORMANCE_TIER_DEGRADATION -> "LIGHT reduces optional work first while Follow, Safe Recall, rescue safety, and readable alerts remain available.";
            case VANILLA_RULE_POLICY_TRANSITION -> "Peaceful, Creative, and Spectator pause combat initiative without granting block edits or disabling Follow, Recall, Journal, or safe controls.";
            case TUTORIAL_SAVE_LOAD -> "Optional guidance progress saves once, does not flood on reload, and dismiss/resume never blocks gameplay.";
            case CRITICAL_ALERT_PROFILE -> "CRITICAL_ONLY suppresses non-critical dialogue while P0 alerts, HUD, Journal, and command feedback remain readable.";
            case ANIMATION_MARKER_CANCEL -> "A target or policy failure before a planned visual marker cancels the success clip and produces no success VFX or sound.";
            case ANIMATION_SAFE_LOAD_FALLBACK -> "Reload during focus, strike, or recovery derives a safe current visual state from server AI state without applying a delayed effect.";
            case ANIMATION_LOW_EFFECTS_CAP -> "Low Effects reduces optional particle counts while preserving a readable icon, HUD, chat, or pose fallback.";
            case SOCIAL_REPLY_INTERRUPTED -> "A hostile, Safe Mode, active plan, low health, or pair-distance break cancels a queued social reply with no late dialogue or gameplay action.";
            case SOCIAL_LOGOUT_PENDING_REPLY -> "Logout clears pending social lead/reply state. Login never fabricates the missing reply or resumes a stale social animation.";
        };
        final String state = "Current plan=" + TeamSavedData.get(player.server).blackboard(player.getUUID()).plan().type()
                + "/" + TeamSavedData.get(player.server).blackboard(player.getUUID()).plan().status();
        return new ChaosBriefing(scenario.name(), expected, state);
    }

    public record ChaosBriefing(String id, String expected, String state) {}
}
