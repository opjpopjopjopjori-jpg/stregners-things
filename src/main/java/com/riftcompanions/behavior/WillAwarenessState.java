package com.riftcompanions.behavior;

import net.minecraft.nbt.CompoundTag;

/** Small persistent Will signal state. It stores evidence summary, not hidden entity data. */
public final class WillAwarenessState {
    private String evidenceKey = "";
    private int evidenceCount;
    private WillSignalLevel level = WillSignalLevel.NONE;
    private long lastEvidenceAt;
    private long stressedUntil;
    private long lastDialogueAt;

    public WillSignalLevel level() { return level; }
    public boolean stressedAt(long now) { return now < stressedUntil; }
    public long lastDialogueAt() { return lastDialogueAt; }

    /** Returns true only when genuinely new/stronger evidence changes the confidence state. */
    public boolean observe(String key, boolean confirmedTag, long now) {
        if (key == null || key.isBlank()) return false;
        boolean same = key.equals(evidenceKey) && now - lastEvidenceAt <= 1200L;
        if (!same) {
            evidenceKey = key;
            evidenceCount = 1;
        } else {
            evidenceCount = Math.min(3, evidenceCount + 1);
        }
        lastEvidenceAt = now;
        WillSignalLevel next = confirmedTag ? WillSignalLevel.HIGH
                : evidenceCount >= 2 ? WillSignalLevel.MEDIUM : WillSignalLevel.LOW;
        boolean changed = next.ordinal() > level.ordinal() || !same;
        level = next;
        return changed;
    }

    public void markStressed(long now, long duration) {
        stressedUntil = Math.max(stressedUntil, now + Math.max(20L, duration));
    }

    public void markDialogue(long now) { lastDialogueAt = now; }

    public void clearIfExpired(long now) {
        if (level != WillSignalLevel.NONE && now - lastEvidenceAt > 2400L) {
            level = WillSignalLevel.NONE;
            evidenceCount = 0;
            evidenceKey = "";
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("EvidenceKey", evidenceKey);
        tag.putInt("EvidenceCount", evidenceCount);
        tag.putString("Level", level.name());
        tag.putLong("LastEvidenceAt", lastEvidenceAt);
        tag.putLong("StressedUntil", stressedUntil);
        tag.putLong("LastDialogueAt", lastDialogueAt);
        return tag;
    }

    public static WillAwarenessState load(CompoundTag tag) {
        WillAwarenessState state = new WillAwarenessState();
        if (tag == null) return state;
        state.evidenceKey = tag.getString("EvidenceKey");
        state.evidenceCount = Math.max(0, Math.min(3, tag.getInt("EvidenceCount")));
        try { state.level = WillSignalLevel.valueOf(tag.getString("Level")); } catch (IllegalArgumentException ignored) { }
        state.lastEvidenceAt = Math.max(0L, tag.getLong("LastEvidenceAt"));
        state.stressedUntil = Math.max(0L, tag.getLong("StressedUntil"));
        state.lastDialogueAt = Math.max(0L, tag.getLong("LastDialogueAt"));
        return state;
    }
}
