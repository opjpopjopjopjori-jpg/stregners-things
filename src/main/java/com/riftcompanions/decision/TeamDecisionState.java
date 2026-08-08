package com.riftcompanions.decision;

import com.riftcompanions.entity.CompanionRole;
import net.minecraft.nbt.CompoundTag;

/** One bounded current decision directive; never replaces player agency. */
public final class TeamDecisionState {
    private DecisionDomain domain = DecisionDomain.CALM_OR_BASE;
    private CompanionRole informationLead = CompanionRole.GUARDIAN;
    private CompanionRole safetyLead = CompanionRole.GUARDIAN;
    private String evidenceKey = "";
    private long updatedAt;
    private long lastDiscussionAt;

    public DecisionDomain domain() { return domain; }
    public CompanionRole informationLead() { return informationLead; }
    public CompanionRole safetyLead() { return safetyLead; }
    public String evidenceKey() { return evidenceKey; }

    public boolean update(DecisionDomain newDomain, CompanionRole newInformationLead, CompanionRole newSafetyLead, String newEvidence, long now) {
        boolean changed = newDomain != domain || !String.valueOf(newEvidence).equals(evidenceKey);
        domain = newDomain == null ? DecisionDomain.CALM_OR_BASE : newDomain;
        informationLead = newInformationLead == null ? CompanionRole.GUARDIAN : newInformationLead;
        safetyLead = newSafetyLead == null ? CompanionRole.GUARDIAN : newSafetyLead;
        evidenceKey = newEvidence == null ? "" : newEvidence.length() <= 96 ? newEvidence : newEvidence.substring(0,96);
        updatedAt = now;
        return changed;
    }

    public boolean canDiscuss(long now) { return now - lastDiscussionAt >= 600L; }
    public void markDiscussion(long now) { lastDiscussionAt = now; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Domain", domain.name());
        tag.putString("InformationLead", informationLead.name());
        tag.putString("SafetyLead", safetyLead.name());
        tag.putString("Evidence", evidenceKey);
        tag.putLong("UpdatedAt", updatedAt);
        tag.putLong("LastDiscussionAt", lastDiscussionAt);
        return tag;
    }

    public static TeamDecisionState load(CompoundTag tag) {
        TeamDecisionState state = new TeamDecisionState();
        if (tag == null) return state;
        try { state.domain = DecisionDomain.valueOf(tag.getString("Domain")); } catch (IllegalArgumentException ignored) { }
        try { state.informationLead = CompanionRole.valueOf(tag.getString("InformationLead")); } catch (IllegalArgumentException ignored) { }
        try { state.safetyLead = CompanionRole.valueOf(tag.getString("SafetyLead")); } catch (IllegalArgumentException ignored) { }
        state.evidenceKey = tag.getString("Evidence");
        state.updatedAt = Math.max(0L, tag.getLong("UpdatedAt"));
        state.lastDiscussionAt = Math.max(0L, tag.getLong("LastDiscussionAt"));
        return state;
    }
}
