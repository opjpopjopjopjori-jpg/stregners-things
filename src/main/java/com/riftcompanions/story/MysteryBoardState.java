package com.riftcompanions.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bounded optional clue board; no clue starts a forced quest or world event. */
public final class MysteryBoardState {
    private static final int LIMIT = 24;
    private final List<MysteryClue> clues = new ArrayList<>();

    public List<MysteryClue> clues() { return List.copyOf(clues); }
    public int observedOrBetterCount() { return (int) clues.stream().filter(clue -> clue.confidence().ordinal() >= MysteryClueConfidence.OBSERVED.ordinal()).count(); }

    public boolean add(MysteryClue clue) {
        if (clue == null || clue.id().isBlank() || clues.stream().anyMatch(existing -> existing.id().equals(clue.id()))) return false;
        if (clues.size() >= LIMIT) clues.remove(0);
        clues.add(clue);
        return true;
    }

    public boolean defer(String id) {
        for (int i = 0; i < clues.size(); i++) {
            if (clues.get(i).id().equals(id)) {
                clues.set(i, clues.get(i).defer());
                return true;
            }
        }
        return false;
    }

    public Optional<MysteryClue> latest() {
        return clues.isEmpty() ? Optional.empty() : Optional.of(clues.get(clues.size() - 1));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        clues.forEach(clue -> list.add(clue.save()));
        tag.put("Clues", list);
        return tag;
    }

    public static MysteryBoardState load(CompoundTag tag) {
        MysteryBoardState state = new MysteryBoardState();
        if (tag == null) return state;
        ListTag list = tag.getList("Clues", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - LIMIT); i < list.size(); i++) {
            MysteryClue clue = MysteryClue.load(list.getCompound(i));
            if (!clue.id().isBlank() && !"invalid".equals(clue.id())) state.clues.add(clue);
        }
        return state;
    }
}
