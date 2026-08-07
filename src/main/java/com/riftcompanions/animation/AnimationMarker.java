package com.riftcompanions.animation;

/**
 * Allowed semantic timing markers. They describe a visual sync point only;
 * they cannot mutate inventory, blocks, plans, memory, or AI state.
 */
public enum AnimationMarker {
    APPLY_POWER_EFFECT,
    APPLY_MELEE_DAMAGE,
    APPLY_PROJECTILE,
    APPLY_RESCUE,
    APPLY_ANCHOR_EFFECT,
    PLAY_SOUND,
    SPAWN_PARTICLE_BURST,
    SET_TARGET_VISUAL_STATE,
    BEGIN_RECOVERY,
    SPAWN_MARKER
}
