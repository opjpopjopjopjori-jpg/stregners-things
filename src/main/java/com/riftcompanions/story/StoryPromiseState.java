package com.riftcompanions.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** One active optional promise at a time, with a small history budget. */
public final class StoryPromiseState {
    private static final int LIMIT = 8;
    private final List<StoryPromise> promises = new ArrayList<>();

    public List<StoryPromise> promises() { return List.copyOf(promises); }
    public Optional<StoryPromise> active() { return promises.stream().filter(StoryPromise::open).findFirst(); }
    public boolean hasId(String id) { return promises.stream().anyMatch(promise -> promise.id().equals(id)); }

    public boolean offer(StoryPromise promise) {
        if (promise == null || active().isPresent() || promises.stream().anyMatch(existing -> existing.id().equals(promise.id()))) return false;
        if (promises.size() >= LIMIT) promises.remove(0);
        promises.add(promise);
        return true;
    }

    public boolean completeByKey(String key) {
        return active().filter(promise -> promise.completionKey().equals(key)).map(StoryPromise::complete).orElse(false);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        promises.forEach(promise -> list.add(promise.save()));
        tag.put("Promises", list);
        return tag;
    }

    public static StoryPromiseState load(CompoundTag tag) {
        StoryPromiseState state = new StoryPromiseState();
        if (tag == null) return state;
        ListTag list = tag.getList("Promises", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - LIMIT); i < list.size(); i++) {
            StoryPromise promise = StoryPromise.load(list.getCompound(i));
            if (!"invalid".equals(promise.id())) state.promises.add(promise);
        }
        return state;
    }
}
