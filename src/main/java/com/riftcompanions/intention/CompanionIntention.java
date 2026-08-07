package com.riftcompanions.intention;

import com.riftcompanions.entity.CompanionRole;
import net.minecraft.nbt.CompoundTag;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/** Persisted personal request with no forced timer or material reward. */
public final class CompanionIntention {
    private final UUID actionId;
    private final String definitionId;
    private final CompanionRole role;
    private final long createdDay;
    private IntentionStatus status;
    private long updatedDay;

    private CompanionIntention(final UUID actionId, final String definitionId, final CompanionRole role,
                               final long createdDay, final IntentionStatus status, final long updatedDay) {
        this.actionId = actionId;
        this.definitionId = clip(definitionId, 64);
        this.role = role;
        this.createdDay = Math.max(0L, createdDay);
        this.status = status;
        this.updatedDay = Math.max(this.createdDay, updatedDay);
    }

    public static CompanionIntention create(final IntentionDefinition definition, final long day, final UUID owner) {
        final String seed = owner + "|" + definition.id() + "|" + day;
        return new CompanionIntention(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)), definition.id(), definition.role(), day,
                IntentionStatus.AVAILABLE, day);
    }

    public UUID actionId() { return actionId; }
    public String definitionId() { return definitionId; }
    public CompanionRole role() { return role; }
    public long createdDay() { return createdDay; }
    public IntentionStatus status() { return status; }
    public long updatedDay() { return updatedDay; }

    public boolean accept(final long day) {
        if (status != IntentionStatus.AVAILABLE && status != IntentionStatus.DEFERRED) return false;
        status = IntentionStatus.ACTIVE;
        updatedDay = Math.max(updatedDay, day);
        return true;
    }

    public boolean defer(final long day) {
        if (status != IntentionStatus.AVAILABLE && status != IntentionStatus.ACTIVE) return false;
        status = IntentionStatus.DEFERRED;
        updatedDay = Math.max(updatedDay, day);
        return true;
    }

    public boolean complete(final long day) {
        if (status != IntentionStatus.ACTIVE) return false;
        status = IntentionStatus.COMPLETED;
        updatedDay = Math.max(updatedDay, day);
        return true;
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putUUID("ActionId", actionId);
        tag.putString("Definition", definitionId);
        tag.putString("Role", role.name());
        tag.putLong("CreatedDay", createdDay);
        tag.putString("Status", status.name());
        tag.putLong("UpdatedDay", updatedDay);
        return tag;
    }

    public static Optional<CompanionIntention> load(final CompoundTag tag) {
        if (tag == null || !tag.hasUUID("ActionId")) return Optional.empty();
        try {
            final String definition = tag.getString("Definition");
            if (definition.isBlank()) return Optional.empty();
            return Optional.of(new CompanionIntention(tag.getUUID("ActionId"), definition,
                    CompanionRole.valueOf(tag.getString("Role")), tag.getLong("CreatedDay"),
                    IntentionStatus.valueOf(tag.getString("Status")), tag.getLong("UpdatedDay")));
        } catch (final IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static String clip(final String value, final int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
