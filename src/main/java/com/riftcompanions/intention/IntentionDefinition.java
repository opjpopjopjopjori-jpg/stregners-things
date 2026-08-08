package com.riftcompanions.intention;

import com.riftcompanions.entity.CompanionRole;

/** Data-pack content only; Java still owns validation and completion safety. */
public record IntentionDefinition(
        String id,
        CompanionRole role,
        String context,
        String optionalAction,
        String completionKey,
        String rewardSummary,
        String dialogueTrigger
) {}
