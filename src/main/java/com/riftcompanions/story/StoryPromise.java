package com.riftcompanions.story;

import com.riftcompanions.entity.CompanionRole;
import net.minecraft.nbt.CompoundTag;

/** Optional companion promise/intention; no timer, penalty, loot, or progression lock. */
public final class StoryPromise {
    private final String id;
    private final CompanionRole role;
    private final String summary;
    private final String completionKey;
    private final long createdDay;
    private PromiseStatus status;

    public StoryPromise(String id, CompanionRole role, String summary, String completionKey, long createdDay, PromiseStatus status) {
        this.id = clip(id, 64);
        this.role = role == null ? CompanionRole.GUARDIAN : role;
        this.summary = clip(summary, 160);
        this.completionKey = clip(completionKey, 64);
        this.createdDay = Math.max(0L, createdDay);
        this.status = status == null ? PromiseStatus.OFFERED : status;
    }

    public String id() { return id; }
    public CompanionRole role() { return role; }
    public String summary() { return summary; }
    public String completionKey() { return completionKey; }
    public long createdDay() { return createdDay; }
    public PromiseStatus status() { return status; }
    public boolean open() { return status == PromiseStatus.OFFERED || status == PromiseStatus.ACCEPTED || status == PromiseStatus.DEFERRED; }
    public boolean accept() { if (status != PromiseStatus.OFFERED && status != PromiseStatus.DEFERRED) return false; status = PromiseStatus.ACCEPTED; return true; }
    public boolean defer() { if (status != PromiseStatus.OFFERED && status != PromiseStatus.ACCEPTED) return false; status = PromiseStatus.DEFERRED; return true; }
    public boolean complete() { if (status != PromiseStatus.ACCEPTED) return false; status = PromiseStatus.COMPLETED; return true; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Role", role.name());
        tag.putString("Summary", summary);
        tag.putString("CompletionKey", completionKey);
        tag.putLong("CreatedDay", createdDay);
        tag.putString("Status", status.name());
        return tag;
    }

    public static StoryPromise load(CompoundTag tag) {
        try { return new StoryPromise(tag.getString("Id"), CompanionRole.valueOf(tag.getString("Role")), tag.getString("Summary"), tag.getString("CompletionKey"), tag.getLong("CreatedDay"), PromiseStatus.valueOf(tag.getString("Status"))); }
        catch (IllegalArgumentException ignored) { return new StoryPromise("invalid", CompanionRole.GUARDIAN, "Invalid promise ignored.", "", 0L, PromiseStatus.DEFERRED); }
    }

    private static String clip(String value, int max) { if (value == null) return ""; return value.length() <= max ? value : value.substring(0, max); }
}
