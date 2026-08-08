package com.riftcompanions.transaction;

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;
import java.util.UUID;

/** A time-bounded exclusive claim; expiry is always safe to release. */
public record Reservation(ReservationType type, String subjectKey, UUID actionId, long claimedAt, long expiresAt) {
    public boolean activeAt(final long now) { return now < expiresAt; }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putString("Subject", subjectKey);
        tag.putUUID("ActionId", actionId);
        tag.putLong("ClaimedAt", claimedAt);
        tag.putLong("ExpiresAt", expiresAt);
        return tag;
    }

    public static Optional<Reservation> load(final CompoundTag tag) {
        if (tag == null || !tag.hasUUID("ActionId")) return Optional.empty();
        try {
            final ReservationType type = ReservationType.valueOf(tag.getString("Type"));
            final String subject = tag.getString("Subject");
            if (subject.isBlank()) return Optional.empty();
            return Optional.of(new Reservation(type, subject, tag.getUUID("ActionId"), Math.max(0L, tag.getLong("ClaimedAt")),
                    Math.max(0L, tag.getLong("ExpiresAt"))));
        } catch (final IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
