package com.riftcompanions.feature;

import com.riftcompanions.config.CompanionConfig;

/** Single source of truth for optional systems. Safety checks remain outside flags. */
public final class FeatureFlags {
    private FeatureFlags() {}

    public static boolean enabled(final FeatureFlag flag) {
        if (flag == null) return false;
        return switch (flag) {
            case HOPPER_CORE -> CompanionConfig.ENABLE_HOPPER_CORE.get();
            case MEMORY_LITE -> CompanionConfig.ENABLE_MEMORY_LITE.get();
            case WILL_HIVE_LINK -> CompanionConfig.ENABLE_WILL_HIVE_LINK.get();
            case WILL_HIVE_SURGE -> CompanionConfig.ENABLE_WILL_HIVE_SURGE.get();
            case MAX_SCOUT -> CompanionConfig.ENABLE_MAX_SCOUT.get();
            case MAX_MIND_ANCHOR -> CompanionConfig.ENABLE_MAX_MIND_ANCHOR.get();
            case ELEVEN_POWERS -> CompanionConfig.ENABLE_ELEVEN_POWERS.get();
            case TEAM_PLANS -> CompanionConfig.ENABLE_TEAM_PLANS.get();
            case INVENTORY_ASSIST -> CompanionConfig.ENABLE_INVENTORY_ASSIST.get();
            case STORY_GRAPH -> CompanionConfig.ENABLE_STORY_GRAPH.get();
            case DUO_DYNAMICS -> CompanionConfig.ENABLE_DUO_DYNAMICS.get();
            case COMPANION_INTENTIONS -> CompanionConfig.ENABLE_COMPANION_INTENTIONS.get();
            case DAILY_AMBIENT -> CompanionConfig.ENABLE_DAILY_AMBIENT.get();
            case COMPANION_SOCIAL -> CompanionConfig.ENABLE_COMPANION_SOCIAL.get();
            case CONTEXTUAL_INTERACTIONS -> CompanionConfig.ENABLE_CONTEXTUAL_INTERACTIONS.get();
        };
    }
}
