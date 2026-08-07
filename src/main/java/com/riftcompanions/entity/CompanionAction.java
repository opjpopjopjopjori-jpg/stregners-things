package com.riftcompanions.entity;

/**
 * Visual action only. Gameplay effects remain server-side in AbilityService and
 * related safety services. New values append at the end to preserve existing
 * synchronized ordinal meanings for a matching protocol version.
 */
public enum CompanionAction {
    NONE,
    GUARD,
    RETREAT_SIGNAL,
    DOWNED,
    RECOVER,
    SEER_NOTICE,
    SEER_FOCUS,
    SEER_RELEASE,
    GIFTED_NOTICE,
    GIFTED_FOCUS,
    GIFTED_PUSH,
    GIFTED_SHIELD,
    GIFTED_RESCUE,
    GIFTED_EXHAUSTED,
    SCOUT_POINT,
    SCOUT_LOOKOUT,
    SCOUT_ANCHOR,
    MELEE_ATTACK,
    HIT_REACT,
    TALK,
    SEER_RELEASE_REDIRECT,
    SEER_RELEASE_SHATTER,
    GUARDIAN_BRACE,
    SCOUT_SIGNAL,
    SOCIAL_LISTEN,
    SOCIAL_POINT,
    SOCIAL_REASSURE,
    SOCIAL_GEAR_CHECK,
    SOCIAL_OBSERVE,
    SOCIAL_CAMPFIRE,
    SOCIAL_WEATHER,
    SOCIAL_HORIZON,
    SOCIAL_BASE,
    SOCIAL_WORK,
    SOCIAL_CAVE,
    SOCIAL_VILLAGE,
    SOCIAL_TRAVEL,
    SOCIAL_CALM,
    CONTEXT_ANIMAL_GREET,
    CONTEXT_ANIMAL_OBSERVE,
    CONTEXT_FIELD_NOTE,
    CONTEXT_THREAT_BRIEF,
    CONTEXT_LOOT_NOTE,
    CONTEXT_BIOME_BRIEF,
    CONTEXT_STRUCTURE_BRIEF,
    CONTEXT_REST_REQUEST,
    CONTEXT_ROUTE_NOTE;

    public static CompanionAction byId(final int id) {
        final CompanionAction[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
