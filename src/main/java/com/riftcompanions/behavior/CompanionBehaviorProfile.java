package com.riftcompanions.behavior;

import com.riftcompanions.entity.CompanionRole;

import java.util.List;

/** Separates personality presentation, tactical role, and forbidden actions from capability code. */
public record CompanionBehaviorProfile(
        CompanionRole role,
        String formationBias,
        List<String> priorityDomains,
        List<String> forbiddenActions,
        String dialogueStyle
) {
    public CompanionBehaviorProfile {
        if (role == null) throw new IllegalArgumentException("role");
        formationBias = formationBias == null ? "neutral" : formationBias;
        priorityDomains = List.copyOf(priorityDomains == null ? List.of() : priorityDomains);
        forbiddenActions = List.copyOf(forbiddenActions == null ? List.of() : forbiddenActions);
        dialogueStyle = dialogueStyle == null ? "clear" : dialogueStyle;
    }
}
