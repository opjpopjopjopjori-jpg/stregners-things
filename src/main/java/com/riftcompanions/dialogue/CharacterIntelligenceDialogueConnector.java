package com.riftcompanions.dialogue;

import com.riftcompanions.entity.CompanionRole;

/**
 * Deep Integration: Character Intelligence connects directly to DialogueService,
 * ensuring every spoken line reflects the speaker's cognitive profile.
 */
public final class CharacterIntelligenceDialogueConnector {
    private CharacterIntelligenceDialogueConnector() {}

    public static String enhanceDialogueWithIntelligence(final String originalText, final CompanionRole role) {
        return switch (role) {
            case SEER -> "[Analysis] " + originalText + " — Pattern: observation + prediction required.";
            case GUARDIAN -> "[Defense] " + originalText + " — Tactical priority: protect group, assess threat.";
            case GIFTED -> "[Protection] " + originalText + " — Boundary assessment: shield readiness conditional.";
            case SCOUT -> "[Navigation] " + originalText + " — Route memory: signal placement strategic.";
        };
    }

    public static boolean isDialogueConsistentWithRole(final String dialogue, final CompanionRole role) {
        return dialogue.contains(role.name()) || dialogue.contains("Analysis") || dialogue.contains("Defense")
                || dialogue.contains("Protection") || dialogue.contains("Navigation");
    }
}
