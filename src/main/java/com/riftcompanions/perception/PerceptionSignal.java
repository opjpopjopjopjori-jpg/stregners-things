package com.riftcompanions.perception;

import com.riftcompanions.entity.CompanionRole;
import net.minecraft.nbt.CompoundTag;

/** Local evidence record with expiry; it never represents omniscient world knowledge. */
public record PerceptionSignal(
        String key,
        CompanionRole observer,
        PerceptionSource source,
        PerceptionConfidence confidence,
        PerceptionActionability actionability,
        long createdAt,
        long expiresAt,
        String summary
) {
    public PerceptionSignal {
        key = clip(key, 96);
        observer = observer == null ? CompanionRole.GUARDIAN : observer;
        source = source == null ? PerceptionSource.VISION : source;
        confidence = confidence == null ? PerceptionConfidence.LOW : confidence;
        actionability = actionability == null ? PerceptionActionability.OBSERVE : actionability;
        createdAt = Math.max(0L, createdAt);
        expiresAt = Math.max(createdAt + 1L, expiresAt);
        summary = clip(summary, 180);
    }

    public boolean activeAt(final long now) { return now < expiresAt; }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Key", key);
        tag.putString("Observer", observer.name());
        tag.putString("Source", source.name());
        tag.putString("Confidence", confidence.name());
        tag.putString("Actionability", actionability.name());
        tag.putLong("CreatedAt", createdAt);
        tag.putLong("ExpiresAt", expiresAt);
        tag.putString("Summary", summary);
        return tag;
    }

    public static PerceptionSignal load(final CompoundTag tag) {
        try {
            return new PerceptionSignal(tag.getString("Key"), CompanionRole.valueOf(tag.getString("Observer")),
                    PerceptionSource.valueOf(tag.getString("Source")), PerceptionConfidence.valueOf(tag.getString("Confidence")),
                    PerceptionActionability.valueOf(tag.getString("Actionability")), tag.getLong("CreatedAt"), tag.getLong("ExpiresAt"), tag.getString("Summary"));
        } catch (final IllegalArgumentException ignored) {
            return new PerceptionSignal("invalid", CompanionRole.GUARDIAN, PerceptionSource.VISION,
                    PerceptionConfidence.LOW, PerceptionActionability.OBSERVE, 0L, 1L, "Invalid perception signal ignored.");
        }
    }

    private static String clip(final String value, final int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
