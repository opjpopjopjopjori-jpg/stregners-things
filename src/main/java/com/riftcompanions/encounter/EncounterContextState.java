package com.riftcompanions.encounter;

import com.riftcompanions.formation.FormationType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Bounded current encounter context; it never represents unseen world data. */
public final class EncounterContextState {
    private String profileId = "unknown";
    private EncounterType type = EncounterType.JOURNEY;
    private ResourceLocation dimension;
    private BlockPos focus;
    private int riskScore;
    private long enteredAt;
    private long lastAnnouncedAt;
    private FormationType recommendedFormation = FormationType.FOLLOW;

    public String profileId() { return profileId; }
    public EncounterType type() { return type; }
    public ResourceLocation dimension() { return dimension; }
    public BlockPos focus() { return focus == null ? null : focus.immutable(); }
    public int riskScore() { return riskScore; }
    public long enteredAt() { return enteredAt; }
    public FormationType recommendedFormation() { return recommendedFormation; }

    /** Returns true when this is a profile transition or a meaningful risk escalation. */
    public boolean update(final WorldEncounterProfile profile, final ResourceLocation newDimension, final BlockPos newFocus,
                          final int newRisk, final FormationType formation, final long now) {
        final String nextId = profile == null ? "unknown" : profile.id();
        final boolean profileChanged = !nextId.equals(profileId) || dimension == null || !dimension.equals(newDimension);
        final boolean escalated = !profileChanged && newRisk >= riskScore + 20;
        if (profileChanged) enteredAt = now;
        profileId = nextId;
        type = profile == null ? EncounterType.JOURNEY : profile.type();
        dimension = newDimension;
        focus = newFocus == null ? null : newFocus.immutable();
        riskScore = Math.max(0, Math.min(100, newRisk));
        recommendedFormation = formation == null ? FormationType.FOLLOW : formation;
        return profileChanged || escalated;
    }

    public boolean canAnnounce(final long now, final long cooldown) {
        return lastAnnouncedAt == 0L || now - lastAnnouncedAt >= Math.max(40L, cooldown);
    }

    public void markAnnounced(final long now) { lastAnnouncedAt = now; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Profile", profileId);
        tag.putString("Type", type.name());
        if (dimension != null) tag.putString("Dimension", dimension.toString());
        if (focus != null) tag.putLong("Focus", focus.asLong());
        tag.putInt("Risk", riskScore);
        tag.putLong("EnteredAt", enteredAt);
        tag.putLong("LastAnnouncedAt", lastAnnouncedAt);
        tag.putString("Formation", recommendedFormation.name());
        return tag;
    }

    public static EncounterContextState load(final CompoundTag tag) {
        EncounterContextState state = new EncounterContextState();
        if (tag == null) return state;
        state.profileId = tag.getString("Profile");
        try { state.type = EncounterType.valueOf(tag.getString("Type")); } catch (IllegalArgumentException ignored) { }
        try { if (tag.contains("Dimension")) state.dimension = new ResourceLocation(tag.getString("Dimension")); } catch (RuntimeException ignored) { }
        if (tag.contains("Focus")) state.focus = BlockPos.of(tag.getLong("Focus"));
        state.riskScore = Math.max(0, Math.min(100, tag.getInt("Risk")));
        state.enteredAt = Math.max(0L, tag.getLong("EnteredAt"));
        state.lastAnnouncedAt = Math.max(0L, tag.getLong("LastAnnouncedAt"));
        try { state.recommendedFormation = FormationType.valueOf(tag.getString("Formation")); } catch (IllegalArgumentException ignored) { }
        return state;
    }
}
