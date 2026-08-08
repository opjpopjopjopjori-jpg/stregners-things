package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.entity.CompanionVisualState;

/**
 * Single mapping authority from server-derived presentation facts to clip keys.
 * No clip selection changes AI state, target, damage, inventory, or plans.
 */
public final class CompanionAnimationStateMapper {
    private CompanionAnimationStateMapper() {}

    public static AnimationClip select(final CompanionEntity companion, final boolean moving) {
        if (companion == null) return AnimationClip.loop("animation.guardian.idle", AnimationPriority.AMBIENT, 0.20F);
        final CompanionRole role = companion.getRole();
        final String prefix = role.id();
        final CompanionState state = companion.getCompanionState();
        if (state == CompanionState.DOWNED) {
            return AnimationClip.loop("animation." + prefix + ".downed_hold", AnimationPriority.DOWNED, 0.10F);
        }
        if (state == CompanionState.STUCK_RECOVERY) {
            return AnimationClip.loop("animation." + prefix + ".stuck_struggle", AnimationPriority.INTERACTION, 0.15F);
        }
        final CompanionVisualState visual = companion.getVisualState();
        final CompanionAction requestedAction = companion.getVisualAction();
        final AnimationClip action = suppressForUrgency(requestedAction, visual, state) ? null : forAction(role, requestedAction);
        if (action != null) return action;
        return forVisualState(role, visual, moving);
    }

    /** Ambient interaction cues must never cover an urgent combat or retreat pose. */
    private static boolean suppressForUrgency(final CompanionAction action, final CompanionVisualState visual,
                                              final CompanionState state) {
        final boolean urgent = visual == CompanionVisualState.ALERT || visual == CompanionVisualState.COMBAT_MELEE
                || visual == CompanionVisualState.COMBAT_RANGED || visual == CompanionVisualState.RETREAT
                || state == CompanionState.RETREATING || state == CompanionState.FIGHTING;
        return urgent && (action == CompanionAction.TALK || action == CompanionAction.SCOUT_POINT
                || action == CompanionAction.SCOUT_LOOKOUT || action.name().startsWith("SOCIAL_")
                || action.name().startsWith("CONTEXT_"));
    }

    private static AnimationClip forAction(final CompanionRole role, final CompanionAction action) {
        final String prefix = role.id();
        return switch (action) {
            case GUARD -> AnimationClip.loop(role == CompanionRole.GUARDIAN
                    ? "animation.guardian.guard_stance" : "animation." + prefix + ".combat_ready", AnimationPriority.COMBAT, 0.15F);
            case GUARDIAN_BRACE -> AnimationClip.oneShot("animation.guardian.protect", AnimationPriority.POWER, 0.10F, true);
            case MELEE_ATTACK -> AnimationClip.oneShot("animation." + prefix + ".melee_attack_1", AnimationPriority.COMBAT, 0.15F, false);
            case HIT_REACT -> AnimationClip.oneShot("animation." + prefix + ".combat_hit_react", AnimationPriority.COMBAT, 0.10F, true);
            case TALK -> AnimationClip.loop("animation." + prefix + ".interact_talk", AnimationPriority.INTERACTION, 0.20F);
            case RETREAT_SIGNAL -> AnimationClip.oneShot("animation." + prefix + ".retreat_signal", AnimationPriority.RESCUE, 0.15F, true);
            case DOWNED -> AnimationClip.oneShot("animation." + prefix + ".downed", AnimationPriority.DOWNED, 0.10F, false);
            case RECOVER -> AnimationClip.oneShot("animation." + prefix + ".recover", AnimationPriority.RESCUE, 0.30F, true);
            case SEER_NOTICE -> AnimationClip.oneShot("animation.seer.anomaly_notice", AnimationPriority.POWER, 0.10F, true);
            case SEER_FOCUS -> AnimationClip.loop("animation.seer.hive_focus", AnimationPriority.POWER, 0.10F);
            case SEER_RELEASE -> AnimationClip.oneShot("animation.seer.hive_release_suspend", AnimationPriority.POWER, 0.10F, true);
            case SEER_RELEASE_REDIRECT -> AnimationClip.oneShot("animation.seer.hive_release_redirect", AnimationPriority.POWER, 0.10F, true);
            case SEER_RELEASE_SHATTER -> AnimationClip.oneShot("animation.seer.hive_release_shatter", AnimationPriority.POWER, 0.10F, true);
            case SEER_DANGER_MODE -> AnimationClip.loop("animation.seer.hive_danger_mode", AnimationPriority.POWER, 0.10F);
            case GIFTED_NOTICE -> AnimationClip.oneShot("animation.gifted.power_notice", AnimationPriority.POWER, 0.10F, true);
            case GIFTED_FOCUS -> AnimationClip.loop("animation.gifted.power_focus", AnimationPriority.POWER, 0.10F);
            case GIFTED_PUSH -> AnimationClip.oneShot("animation.gifted.push_release", AnimationPriority.POWER, 0.10F, true);
            case GIFTED_SHIELD -> AnimationClip.loop("animation.gifted.shield_hold", AnimationPriority.POWER, 0.10F);
            case GIFTED_RESCUE -> AnimationClip.oneShot("animation.gifted.rescue_pull", AnimationPriority.RESCUE, 0.10F, true);
            case GIFTED_EXHAUSTED -> AnimationClip.oneShot("animation.gifted.exhausted_recovery", AnimationPriority.INTERACTION, 0.30F, true);
            case SCOUT_POINT -> AnimationClip.oneShot("animation.scout.route_point", AnimationPriority.INTERACTION, 0.15F, true);
            case SCOUT_LOOKOUT -> AnimationClip.loop("animation.scout.lookout", AnimationPriority.INTERACTION, 0.20F);
            case SCOUT_ANCHOR -> AnimationClip.oneShot("animation.scout.anchor_call", AnimationPriority.POWER, 0.10F, true);
            case SCOUT_SIGNAL -> AnimationClip.oneShot("animation.scout.ranged_release", AnimationPriority.POWER, 0.10F, true);
            case SOCIAL_LISTEN -> AnimationClip.oneShot("animation." + prefix + ".social_listen", AnimationPriority.INTERACTION, 0.15F, true);
            case SOCIAL_POINT -> AnimationClip.oneShot("animation." + prefix + ".social_point", AnimationPriority.INTERACTION, 0.12F, true);
            case SOCIAL_REASSURE -> AnimationClip.oneShot("animation." + prefix + ".social_reassure", AnimationPriority.INTERACTION, 0.15F, true);
            case SOCIAL_GEAR_CHECK -> AnimationClip.oneShot(switch (role) {
                case GUARDIAN -> "animation.guardian.social_radio_check";
                case SCOUT -> "animation.scout.social_route_confirm";
                default -> "animation." + prefix + ".social_gear_check";
            }, AnimationPriority.INTERACTION, 0.15F, true);
            case SOCIAL_OBSERVE -> AnimationClip.oneShot(switch (role) {
                case SEER -> "animation.seer.social_trace_signal";
                case GUARDIAN -> "animation.guardian.social_perimeter_scan";
                case GIFTED -> "animation.gifted.social_grounding_pose";
                case SCOUT -> "animation.scout.social_map_read";
            }, AnimationPriority.INTERACTION, 0.16F, true);
            case SOCIAL_CAMPFIRE -> AnimationClip.oneShot("animation." + prefix + ".social_campfire", AnimationPriority.INTERACTION, 0.18F, true);
            case SOCIAL_WEATHER -> AnimationClip.oneShot("animation." + prefix + ".social_weather", AnimationPriority.INTERACTION, 0.15F, true);
            case SOCIAL_HORIZON -> AnimationClip.oneShot("animation." + prefix + ".social_horizon", AnimationPriority.INTERACTION, 0.15F, true);
            case SOCIAL_BASE -> AnimationClip.oneShot("animation." + prefix + ".social_base", AnimationPriority.INTERACTION, 0.18F, true);
            case SOCIAL_WORK -> AnimationClip.oneShot("animation." + prefix + ".social_work", AnimationPriority.INTERACTION, 0.15F, true);
            case SOCIAL_CAVE -> AnimationClip.oneShot("animation." + prefix + ".social_cave", AnimationPriority.INTERACTION, 0.16F, true);
            case SOCIAL_VILLAGE -> AnimationClip.oneShot("animation." + prefix + ".social_village", AnimationPriority.INTERACTION, 0.16F, true);
            case SOCIAL_TRAVEL -> AnimationClip.oneShot("animation." + prefix + ".social_travel", AnimationPriority.INTERACTION, 0.16F, true);
            case SOCIAL_CALM -> AnimationClip.oneShot(switch (role) {
                case SEER -> "animation.seer.social_reflect";
                case GIFTED -> "animation.gifted.social_breathe";
                case SCOUT -> "animation.scout.social_route_confirm";
                case GUARDIAN -> "animation.guardian.social_base";
            }, AnimationPriority.INTERACTION, 0.18F, true);
            case CONTEXT_ANIMAL_GREET -> AnimationClip.oneShot("animation." + prefix + ".context_animal_greet", AnimationPriority.INTERACTION, 0.16F, true);
            case CONTEXT_ANIMAL_OBSERVE -> AnimationClip.oneShot("animation." + prefix + ".context_animal_observe", AnimationPriority.INTERACTION, 0.16F, true);
            case CONTEXT_FIELD_NOTE -> AnimationClip.oneShot("animation." + prefix + ".context_field_note", AnimationPriority.INTERACTION, 0.16F, true);
            case CONTEXT_THREAT_BRIEF -> AnimationClip.oneShot("animation." + prefix + ".context_threat_brief", AnimationPriority.INTERACTION, 0.12F, true);
            case CONTEXT_LOOT_NOTE -> AnimationClip.oneShot("animation." + prefix + ".context_loot_note", AnimationPriority.INTERACTION, 0.16F, true);
            case CONTEXT_BIOME_BRIEF -> AnimationClip.oneShot("animation." + prefix + ".context_biome_brief", AnimationPriority.INTERACTION, 0.16F, true);
            case CONTEXT_STRUCTURE_BRIEF -> AnimationClip.oneShot("animation." + prefix + ".context_structure_brief", AnimationPriority.INTERACTION, 0.16F, true);
            case CONTEXT_REST_REQUEST -> AnimationClip.oneShot("animation." + prefix + ".context_rest_request", AnimationPriority.INTERACTION, 0.20F, true);
            case CONTEXT_ROUTE_NOTE -> AnimationClip.oneShot("animation." + prefix + ".context_route_note", AnimationPriority.INTERACTION, 0.16F, true);
            case NONE -> null;
        };
    }

    private static AnimationClip forVisualState(final CompanionRole role, final CompanionVisualState visual, final boolean moving) {
        final String prefix = role.id();
        return switch (visual) {
            case GUARD -> AnimationClip.loop(role == CompanionRole.GUARDIAN ? "animation.guardian.guard_stance" : "animation." + prefix + ".combat_ready", AnimationPriority.COMBAT, 0.15F);
            case OBSERVE -> AnimationClip.loop("animation." + prefix + ".idle_observe", AnimationPriority.INTERACTION, 0.20F);
            case ALERT -> AnimationClip.loop("animation." + prefix + ".idle_alert", AnimationPriority.COMBAT, 0.15F);
            case COMBAT_MELEE -> AnimationClip.loop("animation." + prefix + ".combat_ready", AnimationPriority.COMBAT, 0.15F);
            case COMBAT_RANGED -> AnimationClip.loop("animation.scout.ranged_aim", AnimationPriority.COMBAT, 0.15F);
            case RETREAT -> AnimationClip.loop("animation." + prefix + ".run", AnimationPriority.COMBAT, 0.30F);
            case RECOVERY -> AnimationClip.loop("animation." + prefix + ".emotion_exhausted", AnimationPriority.INTERACTION, 0.30F);
            case POWER_FOCUS -> AnimationClip.loop(switch (role) {
                case SEER -> "animation.seer.hive_focus";
                case GIFTED -> "animation.gifted.power_focus";
                case SCOUT -> "animation.scout.mind_anchor_hold";
                case GUARDIAN -> "animation.guardian.combat_ready";
            }, AnimationPriority.POWER, 0.10F);
            case POINT_ROUTE -> AnimationClip.loop("animation.scout.lookout", AnimationPriority.INTERACTION, 0.20F);
            case INTERACT_ANCHOR -> AnimationClip.loop("animation." + prefix + ".interact_use", AnimationPriority.INTERACTION, 0.20F);
            case FOLLOW -> moving
                    ? AnimationClip.loop("animation." + prefix + ".walk", AnimationPriority.LOCOMOTION, 0.20F)
                    : AnimationClip.loop("animation." + prefix + ".idle", AnimationPriority.AMBIENT, 0.20F);
            case IDLE_CALM -> AnimationClip.loop("animation." + prefix + ".idle", AnimationPriority.AMBIENT, 0.20F);
            case DOWNED -> AnimationClip.loop("animation." + prefix + ".downed_hold", AnimationPriority.DOWNED, 0.10F);
        };
    }
}
