package com.riftcompanions.onboarding;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Bounded persistent tutorial progress. A player can dismiss guidance without
 * losing gameplay access, and no hint can block Vanilla progression.
 */
public final class OnboardingState {
    private final EnumSet<OnboardingHint> delivered = EnumSet.noneOf(OnboardingHint.class);
    private boolean dismissed;
    private OnboardingHint mostRecent;

    public boolean dismissed() { return dismissed; }
    public int deliveredCount() { return delivered.size(); }
    public int totalHints() { return OnboardingHint.values().length; }
    public boolean delivered(final OnboardingHint hint) { return hint != null && delivered.contains(hint); }
    public Optional<OnboardingHint> mostRecent() { return Optional.ofNullable(mostRecent); }

    /** Returns true only the first time a non-dismissed hint is eligible. */
    public boolean markDelivered(final OnboardingHint hint) {
        if (dismissed || hint == null || delivered.contains(hint)) return false;
        delivered.add(hint);
        mostRecent = hint;
        return true;
    }

    public Optional<OnboardingHint> recommendedHint() {
        for (final OnboardingHint hint : OnboardingHint.values()) {
            if (!delivered.contains(hint)) return Optional.of(hint);
        }
        return Optional.empty();
    }

    public void dismiss() { dismissed = true; }
    public void resume() { dismissed = false; }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean("Dismissed", dismissed);
        if (mostRecent != null) tag.putString("MostRecent", mostRecent.name());
        final ListTag list = new ListTag();
        for (final OnboardingHint hint : delivered) list.add(StringTag.valueOf(hint.name()));
        tag.put("Delivered", list);
        return tag;
    }

    public static OnboardingState load(final CompoundTag tag) {
        final OnboardingState state = new OnboardingState();
        if (tag == null) return state;
        state.dismissed = tag.getBoolean("Dismissed");
        try {
            state.mostRecent = tag.contains("MostRecent") ? OnboardingHint.valueOf(tag.getString("MostRecent")) : null;
        } catch (final IllegalArgumentException ignored) {
            state.mostRecent = null;
        }
        final ListTag list = tag.getList("Delivered", Tag.TAG_STRING);
        for (int index = 0; index < list.size() && state.delivered.size() < OnboardingHint.values().length; index++) {
            try {
                state.delivered.add(OnboardingHint.valueOf(list.getString(index)));
            } catch (final IllegalArgumentException ignored) {
                // Unknown future hints are ignored rather than blocking world load.
            }
        }
        return state;
    }
}
