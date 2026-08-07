package com.riftcompanions.transaction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;
import java.util.UUID;

/**
 * A compact durable journal entry. It records intent before side effects and
 * reaches COMMITTED only after the owning service has checked its postcondition.
 * Unknown/corrupt entries are ignored rather than allowed to crash a save load.
 */
public final class ActionTransaction {
    public static final int DATA_VERSION = 1;

    private final UUID actionId;
    private final ActionType type;
    private final String subjectKey;
    private final String fingerprint;
    private final long createdAt;
    private final long expiresAt;
    private ActionPhase phase;
    private long finishedAt;
    private String reasonCode;

    private ActionTransaction(final UUID actionId, final ActionType type, final String subjectKey,
                              final String fingerprint, final long createdAt, final long expiresAt,
                              final ActionPhase phase, final long finishedAt, final String reasonCode) {
        this.actionId = actionId;
        this.type = type;
        this.subjectKey = clipped(subjectKey, 96);
        this.fingerprint = clipped(fingerprint, 96);
        this.createdAt = Math.max(0L, createdAt);
        this.expiresAt = Math.max(this.createdAt + 1L, expiresAt);
        this.phase = phase;
        this.finishedAt = Math.max(0L, finishedAt);
        this.reasonCode = clipped(reasonCode, 96);
    }

    public static ActionTransaction begin(final ActionType type, final String subjectKey,
                                          final String fingerprint, final long now, final long ttlTicks) {
        return new ActionTransaction(UUID.randomUUID(), type, subjectKey, fingerprint, now,
                now + Math.max(20L, ttlTicks), ActionPhase.PREPARED, 0L, "PREPARED");
    }

    public UUID actionId() { return actionId; }
    public ActionType type() { return type; }
    public String subjectKey() { return subjectKey; }
    public String fingerprint() { return fingerprint; }
    public long createdAt() { return createdAt; }
    public long expiresAt() { return expiresAt; }
    public ActionPhase phase() { return phase; }
    public long finishedAt() { return finishedAt; }
    public String reasonCode() { return reasonCode; }

    public boolean isOpen() { return !phase.terminal(); }
    public boolean isExpired(final long now) { return isOpen() && now >= expiresAt; }

    public boolean reserve(final String reason) {
        if (phase != ActionPhase.PREPARED) return false;
        phase = ActionPhase.RESERVED;
        reasonCode = clipped(reason, 96);
        return true;
    }

    public boolean applied(final String reason) {
        if (phase != ActionPhase.RESERVED) return false;
        phase = ActionPhase.APPLIED;
        reasonCode = clipped(reason, 96);
        return true;
    }

    public boolean commit(final long now, final String reason) {
        if (phase != ActionPhase.APPLIED) return false;
        phase = ActionPhase.COMMITTED;
        finishedAt = Math.max(createdAt, now);
        reasonCode = clipped(reason, 96);
        return true;
    }

    public boolean rollback(final long now, final String reason) {
        if (!isOpen()) return false;
        phase = ActionPhase.ROLLED_BACK;
        finishedAt = Math.max(createdAt, now);
        reasonCode = clipped(reason, 96);
        return true;
    }

    public boolean expire(final long now) {
        if (!isExpired(now)) return false;
        phase = ActionPhase.EXPIRED;
        finishedAt = Math.max(createdAt, now);
        reasonCode = "EXPIRED_WITHOUT_COMMIT";
        return true;
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", DATA_VERSION);
        tag.putUUID("ActionId", actionId);
        tag.putString("Type", type.name());
        tag.putString("Subject", subjectKey);
        tag.putString("Fingerprint", fingerprint);
        tag.putLong("CreatedAt", createdAt);
        tag.putLong("ExpiresAt", expiresAt);
        tag.putString("Phase", phase.name());
        tag.putLong("FinishedAt", finishedAt);
        tag.putString("Reason", reasonCode);
        return tag;
    }

    public static Optional<ActionTransaction> load(final CompoundTag tag) {
        if (tag == null || !tag.hasUUID("ActionId")) return Optional.empty();
        try {
            final UUID id = tag.getUUID("ActionId");
            final ActionType type = ActionType.valueOf(tag.getString("Type"));
            final ActionPhase phase = ActionPhase.valueOf(tag.getString("Phase"));
            final long created = Math.max(0L, tag.getLong("CreatedAt"));
            final long expires = tag.contains("ExpiresAt", Tag.TAG_LONG) ? tag.getLong("ExpiresAt") : created + 200L;
            return Optional.of(new ActionTransaction(id, type, tag.getString("Subject"), tag.getString("Fingerprint"),
                    created, expires, phase, tag.getLong("FinishedAt"), tag.getString("Reason")));
        } catch (final IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static String clipped(final String value, final int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
