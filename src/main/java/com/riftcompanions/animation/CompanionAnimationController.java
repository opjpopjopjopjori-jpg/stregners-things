package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionLookIntent;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.entity.CompanionVisualState;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * GeckoLib playback adapter with independent body, face, and secondary-motion
 * controllers. These controllers consume already-synchronized entity facts;
 * they never decide damage, targets, navigation, inventory, powers, or AI.
 */
public final class CompanionAnimationController {
    private CompanionAnimationController() {}

    /** Primary body controller: locomotion, combat, interaction, recovery, and power poses. */
    public static PlayState apply(final CompanionEntity companion, final AnimationState<CompanionEntity> state) {
        final AnimationClip clip = AnimationStateMapper.select(companion, state.isMoving());
        final RawAnimation animation = clip.playback() == AnimationPlaybackMode.LOOP
                ? RawAnimation.begin().thenLoop(clip.key())
                : RawAnimation.begin().thenPlay(clip.key());
        return state.setAndContinue(animation);
    }

    /** Face-only controller: blink, expression, and finite natural look gestures. */
    public static PlayState applyFace(final CompanionEntity companion, final AnimationState<CompanionEntity> state) {
        if (companion == null) return PlayState.STOP;
        final String suffix = faceSuffix(companion);
        final String key = "animation." + companion.getRole().id() + "." + suffix;
        final boolean finiteGesture = suffix.startsWith("face_glance_") || suffix.startsWith("face_check_back_");
        final RawAnimation animation = finiteGesture ? RawAnimation.begin().thenPlay(key) : RawAnimation.begin().thenLoop(key);
        return state.setAndContinue(animation);
    }

    /** Hair-only controller: gentle secondary motion stays active underneath body poses. */
    public static PlayState applySecondary(final CompanionEntity companion, final AnimationState<CompanionEntity> state) {
        if (companion == null) return PlayState.STOP;
        final String key = "animation." + companion.getRole().id() + "." + secondarySuffix(companion, state.isMoving());
        return state.setAndContinue(RawAnimation.begin().thenLoop(key));
    }

    private static String faceSuffix(final CompanionEntity companion) {
        final CompanionAction action = companion.getVisualAction();
        if (action == CompanionAction.TALK) return "face_talk";
        if (isPowerAction(action)) return "face_power";
        if (isContextAction(action)) return "face_context";
        if (companion.getCompanionState() == CompanionState.RECOVERING
                || companion.getCompanionState() == CompanionState.EXHAUSTED
                || action == CompanionAction.GIFTED_EXHAUSTED) {
            return "face_recovery";
        }
        final CompanionLookIntent look = companion.getLookIntent();
        if (look == CompanionLookIntent.GLANCE_LEFT) return "face_glance_left";
        if (look == CompanionLookIntent.GLANCE_RIGHT) return "face_glance_right";
        if (look == CompanionLookIntent.CHECK_BACK_LEFT) return "face_check_back_left";
        if (look == CompanionLookIntent.CHECK_BACK_RIGHT) return "face_check_back_right";
        final CompanionVisualState visual = companion.getVisualState();
        if (visual == CompanionVisualState.COMBAT_MELEE || visual == CompanionVisualState.COMBAT_RANGED
                || visual == CompanionVisualState.GUARD || visual == CompanionVisualState.ALERT) {
            return "face_combat";
        }
        if (visual == CompanionVisualState.OBSERVE || visual == CompanionVisualState.POINT_ROUTE) return "face_alert";
        return "face_idle";
    }

    private static String secondarySuffix(final CompanionEntity companion, final boolean moving) {
        final CompanionAction action = companion.getVisualAction();
        if (isPowerAction(action)) return "secondary_power";
        if (isContextAction(action)) return "secondary_context";
        if (companion.getCompanionState() == CompanionState.RECOVERING || companion.getCompanionState() == CompanionState.EXHAUSTED) {
            return "secondary_recovery";
        }
        final CompanionVisualState visual = companion.getVisualState();
        if (visual == CompanionVisualState.COMBAT_MELEE || visual == CompanionVisualState.COMBAT_RANGED
                || visual == CompanionVisualState.GUARD || visual == CompanionVisualState.ALERT) {
            return "secondary_combat";
        }
        if (!moving) return "secondary_idle";
        final double speedSquared = companion.getDeltaMovement().horizontalDistanceSqr();
        return speedSquared >= 0.012D || companion.getCompanionState() == CompanionState.RETREATING
                ? "secondary_run" : "secondary_walk";
    }

    private static boolean isContextAction(final CompanionAction action) {
        return action != null && action.name().startsWith("CONTEXT_");
    }

    private static boolean isPowerAction(final CompanionAction action) {
        return switch (action) {
            case SEER_NOTICE, SEER_FOCUS, SEER_RELEASE, SEER_RELEASE_REDIRECT, SEER_RELEASE_SHATTER,
                    GIFTED_NOTICE, GIFTED_FOCUS, GIFTED_PUSH, GIFTED_SHIELD, GIFTED_RESCUE,
                    GUARDIAN_BRACE, SCOUT_ANCHOR, SCOUT_SIGNAL -> true;
            default -> false;
        };
    }
}
