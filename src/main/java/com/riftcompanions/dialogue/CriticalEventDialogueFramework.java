package com.riftcompanions.dialogue;

import java.util.*;

/**
 * Critical Event Dialogue Framework — maps emergency/critical moments
 * to intense dialogue patterns inspired by famous emergency phrases,
 * combined with existing sound cue triggers (.ogg assets).
 *
 * Note: No licensed voice recordings exist in this repository.
 * This framework uses original professional dialogue + existing sound cues.
 */
public final class CriticalEventDialogueFramework {
    private CriticalEventDialogueFramework() {}

    public static final Map<String, String> CRITICAL_DIALOGUE_MAP = Map.ofEntries(
        Map.entry("combat_peak", "The time has come. Every ability must be used now. The team depends on this moment."),
        Map.entry("retreat_urgent", "We must leave immediately. The danger exceeds all safe limits. No question. Only action."),
        Map.entry("shield_break", "Protection holds for now — but it will not last. The next strike must be avoided at all cost."),
        Map.entry("night_fall", "Darkness falls. The world changes. We must find shelter before the unknown takes us."),
        Map.entry("dragon_fire", "Fire surrounds everything. The dragon has shown its full power. Only concealment and distance survive."),
        Map.entry("victory_final", "The challenge is complete. The team has survived together. Nothing remains except growth."),
        Map.entry("rescue_emergency", "A companion is pinned. Every ability must be directed. The team cannot lose anyone now."),
        Map.entry("blizzard_critical", "The storm blocks all vision. No structure is visible. Only sound guides the path to safety.")
    );

    public static final Map<String, String> SOUND_CUE_MAP = Map.ofEntries(
        Map.entry("combat_peak", "hive/focus.ogg"),
        Map.entry("retreat_urgent", "guardian_retreat.ogg"),
        Map.entry("shield_break", "gifted_shield.ogg"),
        Map.entry("night_fall", "seer_signal.ogg"),
        Map.entry("dragon_fire", "hive/release.ogg"),
        Map.entry("victory_final", "ui/safe_mode.ogg"),
        Map.entry("rescue_emergency", "gifted_rescue.ogg"),
        Map.entry("blizzard_critical", "scout_lookout.ogg")
    );

    public static String getCriticalDialogue(final String eventKey) {
        return CRITICAL_DIALOGUE_MAP.getOrDefault(eventKey, "Critical event detected. Immediate action required. Team must respond with full coordination.");
    }

    public static String getSoundCue(final String eventKey) {
        return SOUND_CUE_MAP.getOrDefault(eventKey, "ambience/seer_signal.ogg");
    }

    public static List<String> getAllCriticalEvents() {
        return new ArrayList<>(CRITICAL_DIALOGUE_MAP.keySet());
    }

    public static boolean isCriticalEvent(final String eventKey) {
        return CRITICAL_DIALOGUE_MAP.containsKey(eventKey);
    }

    public static int totalCriticalEvents() {
        return CRITICAL_DIALOGUE_MAP.size();
    }
}
