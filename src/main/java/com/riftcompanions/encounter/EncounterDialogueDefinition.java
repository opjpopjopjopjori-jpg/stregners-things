package com.riftcompanions.encounter;

import com.riftcompanions.entity.CompanionRole;

/** Maps an observed encounter profile and role to a dialogue trigger. */
public record EncounterDialogueDefinition(String profileId, CompanionRole role, String trigger, int minRisk) {
    public EncounterDialogueDefinition {
        if (profileId == null || profileId.isBlank()) throw new IllegalArgumentException("profileId");
        if (role == null) throw new IllegalArgumentException("role");
        if (trigger == null || trigger.isBlank()) throw new IllegalArgumentException("trigger");
        minRisk = Math.max(0, Math.min(100, minRisk));
    }
}
