package com.riftcompanions.config;

import com.riftcompanions.content.ContentMode;
import com.riftcompanions.dialogue.ChatProfile;
import com.riftcompanions.policy.AutonomyProfile;
import com.riftcompanions.policy.CombatProfile;
import com.riftcompanions.policy.RecallPolicy;
import com.riftcompanions.power.PowerPolicy;
import com.riftcompanions.resource.AutoPickupPolicy;
import com.riftcompanions.mode.PerformanceTier;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Policies change presentation and player-approved boundaries, never bypass the
 * safety gate. Common values remain server-authoritative; client values only
 * affect local HUD presentation.
 */
public final class CompanionConfig {
    /** Bump only with a documented config migration/default path. */
    public static final int CONFIG_VERSION = com.riftcompanions.persistence.SaveVersions.CONFIG_VERSION;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.BooleanValue POWERS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue AUTO_CRITICAL_RETREAT;
    public static final ForgeConfigSpec.BooleanValue STORY_DOWNED;
    public static final ForgeConfigSpec.BooleanValue SAFE_RECALL_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE_COMPANIONS;
    public static final ForgeConfigSpec.IntValue STATUS_SYNC_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue DOWNED_TIMEOUT_TICKS;
    public static final ForgeConfigSpec.BooleanValue SESSION_RECAP_ENABLED;
    public static final ForgeConfigSpec.BooleanValue TUTORIAL_ENABLED;
    public static final ForgeConfigSpec.BooleanValue STORY_UNLOCKS_ENABLED;
    public static final ForgeConfigSpec.EnumValue<ContentMode> CONTENT_MODE;
    public static final ForgeConfigSpec.EnumValue<ChatProfile> CHAT_PROFILE;
    public static final ForgeConfigSpec.BooleanValue LOW_EFFECTS;
    public static final ForgeConfigSpec.EnumValue<PerformanceTier> PERFORMANCE_TIER;
    public static final ForgeConfigSpec.EnumValue<AutonomyProfile> AUTONOMY_PROFILE;
    public static final ForgeConfigSpec.EnumValue<CombatProfile> COMBAT_PROFILE;
    public static final ForgeConfigSpec.EnumValue<RecallPolicy> RECALL_POLICY;
    public static final ForgeConfigSpec.BooleanValue MUTE_GUARDIAN;
    public static final ForgeConfigSpec.BooleanValue MUTE_SEER;
    public static final ForgeConfigSpec.BooleanValue MUTE_GIFTED;
    public static final ForgeConfigSpec.BooleanValue MUTE_SCOUT;
    public static final ForgeConfigSpec.IntValue HIVE_CHANNEL_TICKS;
    /** Legacy configuration key retained as a compatibility ceiling for Swarm Freeze. */
    public static final ForgeConfigSpec.IntValue HIVE_SWARM_TARGET_CAP;
    public static final ForgeConfigSpec.IntValue WILL_CONTROL_HEART_BUDGET;
    public static final ForgeConfigSpec.IntValue WILL_CONTROL_TARGET_CAP;
    public static final ForgeConfigSpec.IntValue WILL_CONTROL_SURGE_HEART_BONUS_PERCENT;
    public static final ForgeConfigSpec.IntValue WILL_CONTROL_OVERLOAD_DURATION_PERCENT;
    public static final ForgeConfigSpec.BooleanValue COMPANION_AUDIO_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HIVE_AUDIO_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HIVE_SURGE_ENABLED;
    public static final ForgeConfigSpec.IntValue HIVE_SURGE_CHANNEL_TICKS;
    public static final ForgeConfigSpec.IntValue HIVE_SURGE_COOLDOWN_TICKS;
    /** Legacy configuration key retained so pre-control-heart profiles keep their crowd threshold. */
    public static final ForgeConfigSpec.IntValue HIVE_SURGE_MIN_LINKED_TARGETS;
    public static final ForgeConfigSpec.IntValue WILL_SURGE_MIN_HOSTILE_TARGETS;
    public static final ForgeConfigSpec.DoubleValue HIVE_SURGE_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue HIVE_SURGE_STRAIN_COST;
    public static final ForgeConfigSpec.IntValue HIVE_SURGE_RECOVERY_TICKS;
    public static final ForgeConfigSpec.EnumValue<PowerPolicy> WILL_POWER_POLICY;
    public static final ForgeConfigSpec.EnumValue<PowerPolicy> ELEVEN_POWER_POLICY;
    public static final ForgeConfigSpec.EnumValue<PowerPolicy> MAX_POWER_POLICY;
    public static final ForgeConfigSpec.EnumValue<AutoPickupPolicy> INVENTORY_PICKUP_POLICY;
    public static final ForgeConfigSpec.DoubleValue RESOURCE_PICKUP_RADIUS;
    public static final ForgeConfigSpec.IntValue RESOURCE_PICKUP_SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue RESOURCE_PICKUP_MAX_PER_SCAN;
    public static final ForgeConfigSpec.IntValue RESOURCE_PICKUP_MIN_UTILITY;
    public static final ForgeConfigSpec.BooleanValue TEAM_SUPPLY_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MIND_ANCHOR_ENABLED;
    public static final ForgeConfigSpec.IntValue MIND_ANCHOR_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue MIND_ANCHOR_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MIND_ANCHOR_GROUNDING_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MIND_ANCHOR_BREAK_FREE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MIND_ANCHOR_ESCAPE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MIND_ANCHOR_DURATION_REDUCTION_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue MIND_ANCHOR_RADIUS;
    public static final ForgeConfigSpec.IntValue ENCOUNTER_CONTEXT_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue ENCOUNTER_DIALOGUE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue ENCOUNTER_MEMORY_RISK_THRESHOLD;
    public static final ForgeConfigSpec.IntValue WILL_SIGNAL_DIALOGUE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_SCOUT_DEFAULT_RADIUS;
    public static final ForgeConfigSpec.IntValue MAX_SCOUT_HARD_LIMIT;

    // Bounded server navigation tuning. These values never permit block edits,
    // door use, chunk loading, unsafe drops, or cross-dimension movement.
    public static final ForgeConfigSpec.BooleanValue CUSTOM_NAVIGATION_ENABLED;
    public static final ForgeConfigSpec.IntValue NAVIGATION_REPATH_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue NAVIGATION_STUCK_TIMEOUT_TICKS;
    public static final ForgeConfigSpec.IntValue NAVIGATION_MAX_RECOVERY_ATTEMPTS;
    public static final ForgeConfigSpec.IntValue NAVIGATION_MAX_SAFE_DROP;
    public static final ForgeConfigSpec.IntValue NAVIGATION_MAX_CANDIDATES;
    public static final ForgeConfigSpec.IntValue NAVIGATION_MAX_PATH_NODES;
    public static final ForgeConfigSpec.IntValue NAVIGATION_MAX_PATH_ATTEMPTS;
    public static final ForgeConfigSpec.DoubleValue NAVIGATION_TARGET_SHIFT_REPATH_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue NAVIGATION_SLOT_RESERVATION_RADIUS;
    public static final ForgeConfigSpec.IntValue PERCEPTION_INTERVAL_TICKS;
    public static final ForgeConfigSpec.BooleanValue DEVELOPER_DIAGNOSTICS_ENABLED;

    // Development isolation flags. They are not permission bypasses and never
    // disable basic Follow, Safe Recall, or Team Journal access.
    public static final ForgeConfigSpec.BooleanValue ENABLE_HOPPER_CORE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MEMORY_LITE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WILL_HIVE_LINK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WILL_HIVE_SURGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MAX_SCOUT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MAX_MIND_ANCHOR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ELEVEN_POWERS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TEAM_PLANS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_INVENTORY_ASSIST;
    public static final ForgeConfigSpec.BooleanValue ENABLE_STORY_GRAPH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DUO_DYNAMICS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COMPANION_INTENTIONS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DAILY_AMBIENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COMPANION_SOCIAL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CONTEXTUAL_INTERACTIONS;

    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue COMPACT_HUD;
    public static final ForgeConfigSpec.IntValue HUD_X_OFFSET;
    public static final ForgeConfigSpec.IntValue HUD_Y_OFFSET;
    public static final ForgeConfigSpec.BooleanValue HIGH_CONTRAST_MARKERS;
    public static final ForgeConfigSpec.BooleanValue DEVELOPER_OVERLAY_DEFAULT;

    static {
        final ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.push("gameplay_policy");
        POWERS_ENABLED = common.comment("Allows explicitly requested Seer/Gifted/Scout abilities. Does not affect follow/recall.")
                .define("powers_enabled", true);
        AUTO_CRITICAL_RETREAT = common.comment("Allows one bounded Guardian-led retreat when player health is critical and hostiles are close.")
                .define("automatic_critical_retreat", true);
        STORY_DOWNED = common.comment("Turns companion death into the tested DOWNED recovery state rather than immediate removal.")
                .define("story_downed", true);
        SAFE_RECALL_ENABLED = common.comment("Allows recall only after SafeTeleport validates ground, collision, hazards and loaded chunk.")
                .define("safe_recall_enabled", true);
        MAX_ACTIVE_COMPANIONS = common.comment("Maximum active roles at once. The supported tactical squad is one or two companions; existing companions are never deleted when this value is reduced.")
                .defineInRange("max_active_companions", 2, 1, 2);
        STATUS_SYNC_INTERVAL_TICKS = common.comment("Owner-only HUD/status packet interval. Lower is smoother; higher costs less network traffic.")
                .defineInRange("status_sync_interval_ticks", 20, 10, 200);
        DOWNED_TIMEOUT_TICKS = common.comment("Story Downed fallback after this many ticks; still requires a safe recall destination and returns weak/recovering.")
                .defineInRange("downed_timeout_ticks", 2400, 200, 72000);
        SESSION_RECAP_ENABLED = common.comment("Show one short local recap on player login when a plan or memory is meaningful.")
                .define("session_recap_enabled", true);
        TUTORIAL_ENABLED = common.comment("Show optional one-time English guidance for the first companion, plan, stuck recovery, Safe Mode, and Vanilla rule policy. It never blocks gameplay.")
                .define("tutorial_enabled", true);
        STORY_UNLOCKS_ENABLED = common.comment("Optional story mode: gate non-Guardian companions behind non-forced story milestones.")
                .define("story_unlocks_enabled", false);
        CONTENT_MODE = common.comment("PERSONAL uses the private fan-inspired presentation. PUBLIC uses original generic names and textures.")
                .defineEnum("content_mode", ContentMode.PERSONAL);
        CHAT_PROFILE = common.comment("CRITICAL_ONLY shows P0 alerts; MINIMAL shows urgent lines; STANDARD is normal; CINEMATIC allows rare ambient lines.")
                .defineEnum("chat_profile", ChatProfile.STANDARD);
        LOW_EFFECTS = common.comment("Reduce non-critical particle counts while keeping HUD/state information intact.")
                .define("low_effects", false);
        PERFORMANCE_TIER = common.comment("LIGHT reduces optional scans/scenes; STANDARD is default; CINEMATIC changes presentation cadence only and does not raise active squad limit.")
                .defineEnum("performance_tier", PerformanceTier.STANDARD);
        AUTONOMY_PROFILE = common.comment("Careful reduces initiative; Cinematic Autonomy allows more optional observations without unsafe actions.")
                .defineEnum("autonomy_profile", AutonomyProfile.STANDARD);
        COMBAT_PROFILE = common.comment("Defensive reduces pursuit; Tactical uses clearer formations without granting magical intelligence.")
                .defineEnum("combat_profile", CombatProfile.BALANCED);
        RECALL_POLICY = common.comment("Safe Recall permits validated fallback teleport; Strict Navigation stops instead of teleporting.")
                .defineEnum("recall_policy", RecallPolicy.SAFE_RECALL);
        MUTE_GUARDIAN = common.comment("Mute non-critical Guardian dialogue. Critical safety HUD/state remains active.").define("mute_guardian", false);
        MUTE_SEER = common.comment("Mute non-critical Seer dialogue. Critical safety HUD/state remains active.").define("mute_seer", false);
        MUTE_GIFTED = common.comment("Mute non-critical Gifted dialogue. Critical safety HUD/state remains active.").define("mute_gifted", false);
        MUTE_SCOUT = common.comment("Mute non-critical Scout dialogue. Critical safety HUD/state remains active.").define("mute_scout", false);
        HIVE_CHANNEL_TICKS = common.comment("Will focus duration before a validated control release. Target loss before release costs no full power.")
                .defineInRange("hive_channel_ticks", 8, 4, 40);
        HIVE_SWARM_TARGET_CAP = common.comment("Legacy compatibility ceiling for Swarm Freeze. The effective maximum also obeys will_control_target_cap and the heart budget.")
                .defineInRange("hive_swarm_target_cap", 8, 1, 16);
        WILL_CONTROL_HEART_BUDGET = common.comment("Will's maximum temporary control-heart budget. Target maximum health is the primary load; stronger and boss targets use more of it.")
                .defineInRange("will_control_heart_budget", 32, 8, 120);
        WILL_CONTROL_TARGET_CAP = common.comment("Absolute maximum targets for one Swarm Freeze release. The heart budget can reduce this number further.")
                .defineInRange("will_control_target_cap", 6, 1, 16);
        WILL_CONTROL_SURGE_HEART_BONUS_PERCENT = common.comment("Extra temporary control-heart capacity during an emergency Will Surge. This does not bypass boss resistance.")
                .defineInRange("will_control_surge_heart_bonus_percent", 25, 0, 100);
        WILL_CONTROL_OVERLOAD_DURATION_PERCENT = common.comment("Maximum duration percentage for a boss or target too strong for the remaining control-heart budget. Such targets only receive short Stagger control.")
                .defineInRange("will_control_overload_duration_percent", 25, 10, 50);
        COMPANION_AUDIO_ENABLED = common.comment("Enable original companion action and UI cues. No actor voice, show music, or copied audio is included.")
                .define("companion_audio_enabled", true);
        HIVE_AUDIO_ENABLED = common.comment("Enable original Hive cue hooks when sound assets are present. No show audio is included.")
                .define("hive_audio_enabled", true);
        HIVE_SURGE_ENABLED = common.comment("Allow rare explicit Will Surge modes against visible eligible hostile threats. Boss resistance and all Safety Gates still apply.")
                .define("hive_surge_enabled", true);
        HIVE_SURGE_CHANNEL_TICKS = common.comment("Focus time before a Hive Surge release. Target loss cancels before full cost.")
                .defineInRange("hive_surge_channel_ticks", 14, 6, 60);
        HIVE_SURGE_COOLDOWN_TICKS = common.comment("Long cooldown for all Hive Surge modes.")
                .defineInRange("hive_surge_cooldown_ticks", 1200, 400, 7200);
        HIVE_SURGE_MIN_LINKED_TARGETS = common.comment("Legacy compatibility lower bound for the Will Surge crowd threshold. Leave at the default unless preserving an older profile.")
                .defineInRange("hive_surge_min_linked_targets", 2, 2, 8);
        WILL_SURGE_MIN_HOSTILE_TARGETS = common.comment("Visible eligible hostile target count that can qualify as a Will Surge emergency without critical player health. The effective threshold also preserves a higher legacy value.")
                .defineInRange("will_surge_min_hostile_targets", 2, 2, 8);
        HIVE_SURGE_ENERGY_COST = common.comment("Energy cost applied only when a valid Surge release resolves.")
                .defineInRange("hive_surge_energy_cost", 42.0D, 20.0D, 80.0D);
        HIVE_SURGE_STRAIN_COST = common.comment("Hive strain cost applied only when a valid Surge release resolves.")
                .defineInRange("hive_surge_strain_cost", 52.0D, 20.0D, 90.0D);
        HIVE_SURGE_RECOVERY_TICKS = common.comment("Will recovery duration after a successful Surge release.")
                .defineInRange("hive_surge_recovery_ticks", 260, 60, 1200);
        WILL_POWER_POLICY = common.comment("Will policy: OFF, SENSE_ONLY, ASK_FIRST, EMERGENCY_ONLY, ALLOWED.")
                .defineEnum("will_power_policy", PowerPolicy.SENSE_ONLY);
        ELEVEN_POWER_POLICY = common.comment("Eleven policy: OFF, RESCUE_ONLY, ASK_FIRST, EMERGENCY_ONLY, ALLOWED.")
                .defineEnum("eleven_power_policy", PowerPolicy.ASK_FIRST);
        MAX_POWER_POLICY = common.comment("Max grounding policy: OFF, ASK_FIRST, EMERGENCY_ONLY, ALLOWED.")
                .defineEnum("max_power_policy", PowerPolicy.ASK_FIRST);
        INVENTORY_PICKUP_POLICY = common.comment("Default OFF. Automatic pickup is not enabled until its safety/risk tests are complete.")
                .defineEnum("inventory_pickup_policy", AutoPickupPolicy.OFF);
        RESOURCE_PICKUP_RADIUS = common.comment("Maximum physical ground-item radius around a companion; companions never navigate toward distant loot.")
                .defineInRange("resource_pickup_radius", 4.0D, 1.0D, 6.0D);
        RESOURCE_PICKUP_SCAN_INTERVAL_TICKS = common.comment("Interval for optional local ground-item assistance scans.")
                .defineInRange("resource_pickup_scan_interval_ticks", 20, 10, 200);
        RESOURCE_PICKUP_MAX_PER_SCAN = common.comment("Maximum nearby world-free item entities considered per companion scan.")
                .defineInRange("resource_pickup_max_per_scan", 3, 1, 8);
        RESOURCE_PICKUP_MIN_UTILITY = common.comment("Minimum transparent utility score required before optional world-free pickup.")
                .defineInRange("resource_pickup_min_utility", 30, 1, 100);
        TEAM_SUPPLY_ENABLED = common.comment("Enable explicit player-bound Team Supply container withdrawals. Disabled by default until runtime tests pass.")
                .define("team_supply_enabled", false);
        MIND_ANCHOR_ENABLED = common.comment("Enable advanced Max Mind Anchor actions against explicit supported mental effects. Disabled by default until runtime tests pass.")
                .define("mind_anchor_enabled", false);
        MIND_ANCHOR_DURATION_TICKS = common.comment("Temporary Mind Anchor duration in ticks.")
                .defineInRange("mind_anchor_duration_ticks", 180, 40, 1200);
        MIND_ANCHOR_COOLDOWN_TICKS = common.comment("Shared Mind Anchor Point cooldown in ticks.")
                .defineInRange("mind_anchor_cooldown_ticks", 360, 80, 2400);
        MIND_ANCHOR_GROUNDING_COOLDOWN_TICKS = common.comment("Grounding Call cooldown in ticks.")
                .defineInRange("mind_anchor_grounding_cooldown_ticks", 180, 40, 1200);
        MIND_ANCHOR_BREAK_FREE_COOLDOWN_TICKS = common.comment("Break Free cooldown in ticks.")
                .defineInRange("mind_anchor_break_free_cooldown_ticks", 260, 60, 1800);
        MIND_ANCHOR_ESCAPE_COOLDOWN_TICKS = common.comment("Escape Window cooldown in ticks.")
                .defineInRange("mind_anchor_escape_cooldown_ticks", 520, 120, 3600);
        MIND_ANCHOR_DURATION_REDUCTION_PER_SECOND = common.comment("Supported mental-effect ticks removed once per second while the player remains in an active anchor radius.")
                .defineInRange("mind_anchor_duration_reduction_per_second", 10, 1, 60);
        MIND_ANCHOR_RADIUS = common.comment("Local Mind Anchor radius; it never teleports or grants invulnerability.")
                .defineInRange("mind_anchor_radius", 8.0D, 2.0D, 16.0D);
        ENCOUNTER_CONTEXT_INTERVAL_TICKS = common.comment("Interval for bounded encounter context reassessment from visible biome/threat/plan facts.")
                .defineInRange("encounter_context_interval_ticks", 40, 20, 400);
        ENCOUNTER_DIALOGUE_COOLDOWN_TICKS = common.comment("Minimum time between encounter context dialogue announcements.")
                .defineInRange("encounter_dialogue_cooldown_ticks", 1200, 200, 7200);
        ENCOUNTER_MEMORY_RISK_THRESHOLD = common.comment("Risk threshold before an encounter transition writes a bounded memory hook.")
                .defineInRange("encounter_memory_risk_threshold", 55, 20, 100);
        WILL_SIGNAL_DIALOGUE_COOLDOWN_TICKS = common.comment("Minimum time between Will confidence signal dialogue lines.")
                .defineInRange("will_signal_dialogue_cooldown_ticks", 1200, 200, 7200);
        MAX_SCOUT_DEFAULT_RADIUS = common.comment("Preferred short Scout route radius in already loaded terrain.")
                .defineInRange("max_scout_default_radius", 24, 8, 32);
        MAX_SCOUT_HARD_LIMIT = common.comment("Absolute Scout distance limit from player for the initial implementation.")
                .defineInRange("max_scout_hard_limit", 32, 16, 40);
        common.pop();
        common.push("navigation");
        CUSTOM_NAVIGATION_ENABLED = common.comment("Use the bounded companion navigation safety layer over vanilla PathNavigation. Disabling it keeps legacy navigation only for isolation testing.")
                .define("custom_navigation_enabled", true);
        NAVIGATION_REPATH_INTERVAL_TICKS = common.comment("Minimum server ticks between normal companion path rebuilds for the same goal.")
                .defineInRange("repath_interval_ticks", 12, 4, 80);
        NAVIGATION_STUCK_TIMEOUT_TICKS = common.comment("No-progress time before a companion tries a bounded alternate path candidate.")
                .defineInRange("stuck_timeout_ticks", 60, 20, 400);
        NAVIGATION_MAX_RECOVERY_ATTEMPTS = common.comment("Bounded alternate-path attempts before companion state escalates to Stuck Recovery.")
                .defineInRange("max_recovery_attempts", 3, 1, 8);
        NAVIGATION_MAX_SAFE_DROP = common.comment("Maximum downward path-node step accepted by companion navigation; larger cliff drops are rejected.")
                .defineInRange("max_safe_drop", 2, 1, 4);
        NAVIGATION_MAX_CANDIDATES = common.comment("Maximum local safe destination alternatives sampled during one bounded replan.")
                .defineInRange("max_candidates", 12, 4, 16);
        NAVIGATION_MAX_PATH_NODES = common.comment("Maximum vanilla path-node count accepted by the safety layer to avoid expensive long-path churn.")
                .defineInRange("max_path_nodes", 72, 16, 160);
        NAVIGATION_MAX_PATH_ATTEMPTS = common.comment("Maximum vanilla path searches attempted during one bounded companion replan.")
                .defineInRange("max_path_attempts", 4, 1, 8);
        NAVIGATION_TARGET_SHIFT_REPATH_DISTANCE = common.comment("Player/formation goal movement required before an active path is rebuilt early.")
                .defineInRange("target_shift_repath_distance", 3.0D, 1.0D, 8.0D);
        NAVIGATION_SLOT_RESERVATION_RADIUS = common.comment("Minimum separation around a claimed formation slot to reduce companion pileups.")
                .defineInRange("slot_reservation_radius", 1.25D, 0.75D, 3.0D);
        PERCEPTION_INTERVAL_TICKS = common.comment("Server interval for bounded companion local sight checks. This never scans unloaded chunks or sees through walls.")
                .defineInRange("perception_interval_ticks", 20, 10, 200);
        common.pop();
        common.push("developer_diagnostics");
        DEVELOPER_DIAGNOSTICS_ENABLED = common.comment("Collect small local timing summaries for the developer overlay and report. Disabled by default and never a replacement for a real profiler.")
                .define("enabled", false);
        common.pop();
        common.push("feature_flags");
        ENABLE_HOPPER_CORE = common.comment("Development flag for Guardian-specific optional behavior. Follow and Recall remain available.").define("enable_hopper_core", true);
        ENABLE_MEMORY_LITE = common.comment("Development flag for bounded temporal/team memory updates.").define("enable_memory_lite", true);
        ENABLE_WILL_HIVE_LINK = common.comment("Development flag for Seer Hive sensing and control only.").define("enable_will_hive_link", true);
        ENABLE_WILL_HIVE_SURGE = common.comment("Development flag for rare high-cost Seer Hive Surge modes only.").define("enable_will_hive_surge", true);
        ENABLE_MAX_SCOUT = common.comment("Development flag for Scout route and grounding features only.").define("enable_max_scout", true);
        ENABLE_MAX_MIND_ANCHOR = common.comment("Development flag for advanced Max Mind Anchor actions only.").define("enable_max_mind_anchor", true);
        ENABLE_ELEVEN_POWERS = common.comment("Development flag for Gifted power actions only.").define("enable_eleven_powers", true);
        ENABLE_TEAM_PLANS = common.comment("Development flag for optional team plans; safety alerts remain readable when disabled.").define("enable_team_plans", true);
        ENABLE_INVENTORY_ASSIST = common.comment("Development flag for future automation; manual bounded transfers stay explicit.").define("enable_inventory_assist", false);
        ENABLE_STORY_GRAPH = common.comment("Development flag for optional story and companion arc reflections.").define("enable_story_graph", true);
        ENABLE_DUO_DYNAMICS = common.comment("Development flag for two-companion pair context and synergy notes.").define("enable_duo_dynamics", true);
        ENABLE_COMPANION_INTENTIONS = common.comment("Development flag for optional, deferrable base intentions.").define("enable_companion_intentions", true);
        ENABLE_DAILY_AMBIENT = common.comment("Development flag for one bounded daily ambient opportunity.").define("enable_daily_ambient", true);
        ENABLE_COMPANION_SOCIAL = common.comment("Development flag for bounded two-companion social exchanges around player-visible local world cues. It never changes combat, plans, inventory, or player input.").define("enable_companion_social", true);
        ENABLE_CONTEXTUAL_INTERACTIONS = common.comment("Development flag for bounded authored reactions to visible local animals, focused mobs, biomes, structures, and fatigue. It never controls animals, creates loot, starts forced plans, or reads containers.").define("enable_contextual_interactions", true);
        common.pop();
        COMMON_SPEC = common.build();

        final ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("hud");
        HUD_ENABLED = client.comment("Render the compact companion HUD outside other screens.")
                .define("enabled", true);
        COMPACT_HUD = client.comment("Use a smaller one-line card per companion.")
                .define("compact", false);
        HUD_X_OFFSET = client.comment("Horizontal HUD offset from the left edge.")
                .defineInRange("x_offset", 6, 0, 5000);
        HUD_Y_OFFSET = client.comment("Vertical HUD offset from the top edge.")
                .defineInRange("y_offset", 8, 0, 5000);
        HIGH_CONTRAST_MARKERS = client.comment("Use stronger state colors and labels for accessibility.")
                .define("high_contrast_markers", false);
        DEVELOPER_OVERLAY_DEFAULT = client.comment("Show the owner-only developer overlay at startup. The overlay can also be toggled with a rebindable key.")
                .define("developer_overlay_default", false);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private CompanionConfig() {}
}
