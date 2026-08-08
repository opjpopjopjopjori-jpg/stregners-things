package com.riftcompanions.arc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/** Bounded arc facts, persisted independently from dialogue text. */
public final class ArcState {
    private final EnumMap<CompanionArc, EnumSet<ArcMilestone>> milestones = new EnumMap<>(CompanionArc.class);
    private CompanionArc activeHook;

    public boolean add(final CompanionArc arc, final ArcMilestone milestone) {
        if (arc == null || milestone == null) return false;
        final boolean changed = milestones.computeIfAbsent(arc, ignored -> EnumSet.noneOf(ArcMilestone.class)).add(milestone);
        if (changed && activeHook == null) activeHook = arc;
        return changed;
    }

    public int count(final CompanionArc arc) { return milestones.getOrDefault(arc, EnumSet.noneOf(ArcMilestone.class)).size(); }
    public boolean completed(final CompanionArc arc) { return count(arc) >= 4; }
    public CompanionArc activeHook() { return activeHook; }

    public void deferActiveHook() { activeHook = null; }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        if (activeHook != null) tag.putString("ActiveHook", activeHook.name());
        final ListTag arcs = new ListTag();
        for (final Map.Entry<CompanionArc, EnumSet<ArcMilestone>> entry : milestones.entrySet()) {
            final CompoundTag arc = new CompoundTag();
            arc.putString("Arc", entry.getKey().name());
            final ListTag values = new ListTag();
            entry.getValue().forEach(milestone -> values.add(StringTag.valueOf(milestone.name())));
            arc.put("Milestones", values);
            arcs.add(arc);
        }
        tag.put("Arcs", arcs);
        return tag;
    }

    public static ArcState load(final CompoundTag tag) {
        final ArcState state = new ArcState();
        if (tag == null) return state;
        try { state.activeHook = tag.contains("ActiveHook") ? CompanionArc.valueOf(tag.getString("ActiveHook")) : null; }
        catch (final IllegalArgumentException ignored) { state.activeHook = null; }
        final ListTag arcs = tag.getList("Arcs", Tag.TAG_COMPOUND);
        for (int i = 0; i < arcs.size(); i++) {
            final CompoundTag entry = arcs.getCompound(i);
            try {
                final CompanionArc arc = CompanionArc.valueOf(entry.getString("Arc"));
                final ListTag values = entry.getList("Milestones", Tag.TAG_STRING);
                for (int j = 0; j < values.size(); j++) {
                    try { state.add(arc, ArcMilestone.valueOf(values.getString(j))); }
                    catch (final IllegalArgumentException ignored) { }
                }
            } catch (final IllegalArgumentException ignored) { }
        }
        return state;
    }
}
