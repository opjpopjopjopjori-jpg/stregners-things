package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;

import java.util.Optional;

/**
 * Presentation marker façade. It exposes marker timing to client VFX/audio and
 * QA code while intentionally refusing to apply any gameplay side effect.
 */
public final class CompanionAnimationHandler {
    private CompanionAnimationHandler() {}

    public static Optional<AnimationMarkerPlan> markerPlan(final CompanionAction action) {
        return AnimationMarkerPlan.forAction(action);
    }

    public static boolean mayRenderPresentationCue(final CompanionEntity companion) {
        return companion != null && companion.getCompanionState() != com.riftcompanions.entity.CompanionState.DOWNED;
    }

    /** A client marker can never request damage, inventory, block, plan, or memory mutation. */
    public static boolean mayApplyGameplayFromMarker() {
        return false;
    }
}
