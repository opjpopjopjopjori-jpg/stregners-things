package com.riftcompanions.relationship;

import net.minecraft.nbt.CompoundTag;

/** Small persistent relation summary; no hidden punishment or dialogue-tree exploit. */
public record CompanionRelation(int trust, long sharedMilestones) {
    public CompanionRelation {
        trust = Math.max(0, Math.min(100, trust));
        sharedMilestones = Math.max(0L, sharedMilestones);
    }

    public static CompanionRelation initial() {
        return new CompanionRelation(20, 0L);
    }

    public CompanionRelation adjust(final int delta, final boolean milestone) {
        return new CompanionRelation(Math.max(0, Math.min(100, trust + delta)), sharedMilestones + (milestone ? 1L : 0L));
    }

    public RelationshipLevel level() {
        return RelationshipLevel.fromTrust(trust);
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("Trust", trust);
        tag.putLong("Milestones", sharedMilestones);
        return tag;
    }

    public static CompanionRelation load(final CompoundTag tag) {
        return new CompanionRelation(tag.getInt("Trust"), tag.getLong("Milestones"));
    }
}
