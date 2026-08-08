package com.riftcompanions.resource;

import com.riftcompanions.entity.CompanionRole;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Optional;

/** One explicit player-bound container anchor with per-role/day withdrawal caps. */
public final class TeamSupplyState {
    private ResourceLocation dimension;
    private BlockPos position;
    private long boundDay = -1L;
    private long usageDay = -1L;
    private final EnumMap<CompanionRole, Integer> withdrawn = new EnumMap<>(CompanionRole.class);

    public boolean isBound() { return dimension != null && position != null; }
    public Optional<ResourceLocation> dimension() { return Optional.ofNullable(dimension); }
    public Optional<BlockPos> position() { return Optional.ofNullable(position == null ? null : position.immutable()); }
    public long boundDay() { return boundDay; }

    public void bind(final ResourceLocation newDimension, final BlockPos newPosition, final long day) {
        dimension = newDimension;
        position = newPosition == null ? null : newPosition.immutable();
        boundDay = Math.max(0L, day);
        usageDay = day;
        withdrawn.clear();
    }

    public void clear() {
        dimension = null;
        position = null;
        boundDay = -1L;
        usageDay = -1L;
        withdrawn.clear();
    }

    public boolean canWithdraw(final CompanionRole role, final ItemCategory category, final long day) {
        resetDay(day);
        return role != null && CompanionResourceProfile.allowedAutoCategories(role).contains(category)
                && !CompanionResourceProfile.neverAutoTake(category)
                && withdrawn.getOrDefault(role, 0) < CompanionResourceProfile.dailyTeamSupplyCap(role);
    }

    public void recordWithdraw(final CompanionRole role, final long day) {
        resetDay(day);
        withdrawn.put(role, withdrawn.getOrDefault(role, 0) + 1);
    }

    public int withdrawnToday(final CompanionRole role, final long day) {
        resetDay(day);
        return withdrawn.getOrDefault(role, 0);
    }

    /** Read-only UI/packet view that never mutates day state during a status sync. */
    public int withdrawnTodayReadOnly(final CompanionRole role, final long day) {
        return usageDay == day ? withdrawn.getOrDefault(role, 0) : 0;
    }

    private void resetDay(final long day) {
        if (usageDay != day) {
            usageDay = day;
            withdrawn.clear();
        }
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        if (dimension != null) tag.putString("Dimension", dimension.toString());
        if (position != null) tag.putLong("Position", position.asLong());
        tag.putLong("BoundDay", boundDay);
        tag.putLong("UsageDay", usageDay);
        final ListTag list = new ListTag();
        withdrawn.forEach((role, count) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putString("Role", role.name());
            entry.putInt("Count", count);
            list.add(entry);
        });
        tag.put("Withdrawn", list);
        return tag;
    }

    public static TeamSupplyState load(final CompoundTag tag) {
        final TeamSupplyState state = new TeamSupplyState();
        if (tag == null) return state;
        try {
            if (tag.contains("Dimension") && tag.contains("Position")) {
                state.dimension = new ResourceLocation(tag.getString("Dimension"));
                state.position = BlockPos.of(tag.getLong("Position"));
            }
        } catch (final RuntimeException ignored) { state.clear(); }
        state.boundDay = tag.getLong("BoundDay");
        state.usageDay = tag.getLong("UsageDay");
        final ListTag list = tag.getList("Withdrawn", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            try { state.withdrawn.put(CompanionRole.valueOf(entry.getString("Role")), Math.max(0, entry.getInt("Count"))); }
            catch (final IllegalArgumentException ignored) { }
        }
        return state;
    }
}
