package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionAction;

import java.util.Optional;

/**
 * Marker metadata used by the visual/audio layer and QA. The logical server
 * validates and resolves gameplay before any success clip is requested; a
 * client marker never becomes an authority boundary.
 */
public record AnimationMarkerPlan(
        CompanionAction action,
        AnimationMarker marker,
        float seconds,
        boolean cancellableBeforeMarker,
        String serverAuthorityNote
) {
    public AnimationMarkerPlan {
        action = action == null ? CompanionAction.NONE : action;
        marker = marker == null ? AnimationMarker.PLAY_SOUND : marker;
        seconds = Math.max(0.0F, Math.min(4.0F, seconds));
        serverAuthorityNote = serverAuthorityNote == null ? "Presentation only." : serverAuthorityNote;
    }

    public static Optional<AnimationMarkerPlan> forAction(final CompanionAction action) {
        if (action == null) return Optional.empty();
        return switch (action) {
            case MELEE_ATTACK -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.APPLY_MELEE_DAMAGE, 0.42F, false,
                    "Server combat resolves target damage before the confirmed strike cue is broadcast."));
            case RETREAT_SIGNAL -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.PLAY_SOUND, 0.28F, true,
                    "Server plan state already owns retreat; marker may request an original local cue only."));
            case SEER_RELEASE -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.APPLY_POWER_EFFECT, 0.28F, true,
                    "AbilityService or HiveChannelManager validates the target and resolves the effect before release presentation."));
            case SEER_RELEASE_REDIRECT -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.APPLY_POWER_EFFECT, 0.30F, true,
                    "HiveChannelManager validates a visible eligible hostile target and its Will control-heart budget before redirect release presentation."));
            case SEER_RELEASE_SHATTER -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.APPLY_POWER_EFFECT, 0.34F, true,
                    "HiveChannelManager validates a hostile target, control budget, and boss resistance before non-graphic structural shatter presentation."));
            case GUARDIAN_BRACE -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.APPLY_POWER_EFFECT, 0.14F, true,
                    "GuardianBraceService validates threat, range, energy, and duration before the protect presentation."));
            case GIFTED_PUSH -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.APPLY_POWER_EFFECT, 0.12F, true,
                    "AbilityService validates and resolves Push before the release presentation."));
            case GIFTED_RESCUE -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.APPLY_RESCUE, 0.24F, true,
                    "AbilityService validates safe rescue placement before the pull presentation."));
            case SCOUT_ANCHOR -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.APPLY_ANCHOR_EFFECT, 0.32F, true,
                    "MindAnchorService validates supported effects and safe location before presentation."));
            case SCOUT_POINT -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.SPAWN_MARKER, 0.38F, true,
                    "Route observation is server-owned; marker can render a local readable route cue only."));
            case SCOUT_SIGNAL -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.SET_TARGET_VISUAL_STATE, 0.16F, true,
                    "AbilityService validates visible hostile targets before temporary signal-mark presentation."));
            case RECOVER -> Optional.of(new AnimationMarkerPlan(action, AnimationMarker.BEGIN_RECOVERY, 0.20F, false,
                    "Recovery state is already server-authoritative; marker communicates it visually."));
            default -> Optional.empty();
        };
    }
}
