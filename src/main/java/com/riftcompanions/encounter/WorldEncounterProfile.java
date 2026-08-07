package com.riftcompanions.encounter;

import com.riftcompanions.entity.CompanionRole;

import java.util.List;

/** Data-shaped encounter profile. It describes visible context; it never creates hidden world knowledge. */
public record WorldEncounterProfile(
        String id,
        EncounterType type,
        List<String> observableSignals,
        String primaryRisk,
        String secondaryRisk,
        List<CompanionRole> suitableRoles,
        List<String> forbiddenAssumptions,
        List<String> suggestedPlayerChoices,
        List<String> entryConditions,
        List<String> exitConditions,
        List<String> memoryHooks,
        List<String> dialogueCategories,
        boolean adapterRequired
) {
    public WorldEncounterProfile {
        observableSignals = List.copyOf(observableSignals);
        suitableRoles = List.copyOf(suitableRoles);
        forbiddenAssumptions = List.copyOf(forbiddenAssumptions);
        suggestedPlayerChoices = List.copyOf(suggestedPlayerChoices);
        entryConditions = List.copyOf(entryConditions);
        exitConditions = List.copyOf(exitConditions);
        memoryHooks = List.copyOf(memoryHooks);
        dialogueCategories = List.copyOf(dialogueCategories);
    }
}
