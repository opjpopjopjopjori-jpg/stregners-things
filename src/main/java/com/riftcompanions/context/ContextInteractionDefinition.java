package com.riftcompanions.context;

import com.riftcompanions.entity.CompanionAction;

/**
 * Immutable authored context scene definition. It contains no coordinate,
 * entity UUID, inventory, loot grant, forced quest, or plan authority.
 */
public record ContextInteractionDefinition(
        String id,
        ContextInteractionKind kind,
        String match,
        String group,
        ContextInteractionStage stage,
        String leadTrigger,
        String replyTrigger,
        CompanionAction action,
        CompanionAction replyAction,
        int priority,
        int cooldownTicks,
        boolean requiresFocusedMonster
) {
    public ContextInteractionDefinition {
        id = id == null ? "" : id;
        kind = kind == null ? ContextInteractionKind.ENTITY : kind;
        match = match == null ? "" : match;
        group = group == null || group.isBlank() ? id : group;
        stage = stage == null ? ContextInteractionStage.DISCOVERY : stage;
        leadTrigger = leadTrigger == null ? "" : leadTrigger;
        replyTrigger = replyTrigger == null ? "" : replyTrigger;
        action = action == null ? CompanionAction.CONTEXT_FIELD_NOTE : action;
        replyAction = replyAction == null ? CompanionAction.CONTEXT_ANIMAL_OBSERVE : replyAction;
        priority = Math.max(0, Math.min(4, priority));
        cooldownTicks = Math.max(100, Math.min(24000, cooldownTicks));
    }
}
