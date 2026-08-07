package com.riftcompanions.dialogue;

import com.riftcompanions.entity.CompanionRole;

/** Parsed data-pack dialogue. Text is content, never hard-coded into AI classes. */
public record DialogueLine(
        String id,
        CompanionRole role,
        String trigger,
        int priority,
        long cooldownTicks,
        String text
) {}
