package com.riftcompanions.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;

/** Small persistent optional-story state. Sandbox survival stays playable regardless of its status. */
public final class StoryState {
    private final EnumMap<StoryChapter, StoryNodeStatus> nodes = new EnumMap<>(StoryChapter.class);
    private MysteryBoardState mysteryBoard = new MysteryBoardState();
    private StoryPromiseState promises = new StoryPromiseState();

    public StoryState() {
        for (StoryChapter chapter : StoryChapter.values()) nodes.put(chapter, StoryNodeStatus.LOCKED);
        nodes.put(StoryChapter.ARRIVAL, StoryNodeStatus.AVAILABLE);
    }

    public StoryNodeStatus status(final StoryChapter chapter) { return nodes.getOrDefault(chapter, StoryNodeStatus.LOCKED); }
    public void set(final StoryChapter chapter, final StoryNodeStatus status) { nodes.put(chapter, status); }
    public boolean isAtLeastAvailable(final StoryChapter chapter) { return status(chapter) != StoryNodeStatus.LOCKED; }
    public MysteryBoardState mysteryBoard() { return mysteryBoard; }
    public StoryPromiseState promises() { return promises; }

    public boolean complete(final StoryChapter chapter) {
        if (status(chapter) == StoryNodeStatus.COMPLETED) return false;
        nodes.put(chapter, StoryNodeStatus.COMPLETED);
        return true;
    }

    public Map<StoryChapter, StoryNodeStatus> snapshot() { return Map.copyOf(nodes); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        nodes.forEach((chapter,status) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Chapter", chapter.name());
            entry.putString("Status", status.name());
            list.add(entry);
        });
        tag.put("Nodes", list);
        tag.put("MysteryBoard", mysteryBoard.save());
        tag.put("Promises", promises.save());
        return tag;
    }

    public static StoryState load(final CompoundTag tag) {
        StoryState state = new StoryState();
        ListTag list = tag.getList("Nodes", Tag.TAG_COMPOUND);
        for (int i=0;i<list.size();i++) {
            CompoundTag entry=list.getCompound(i);
            try { state.set(StoryChapter.valueOf(entry.getString("Chapter")), StoryNodeStatus.valueOf(entry.getString("Status"))); }
            catch (IllegalArgumentException ignored) { }
        }
        if (tag.contains("MysteryBoard", Tag.TAG_COMPOUND)) state.mysteryBoard = MysteryBoardState.load(tag.getCompound("MysteryBoard"));
        if (tag.contains("Promises", Tag.TAG_COMPOUND)) state.promises = StoryPromiseState.load(tag.getCompound("Promises"));
        return state;
    }
}
