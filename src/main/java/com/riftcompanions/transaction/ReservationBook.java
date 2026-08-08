package com.riftcompanions.transaction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded reservation owner map. No stale owner blocks a world after reload. */
public final class ReservationBook {
    private static final int ENTRY_LIMIT = 32;
    private final LinkedHashMap<String, Reservation> reservations = new LinkedHashMap<>();

    public boolean claim(final ReservationType type, final String subject, final UUID actionId, final long now, final long duration) {
        if (type == null || subject == null || subject.isBlank() || actionId == null) return false;
        final String key = key(type, subject);
        final Reservation existing = reservations.get(key);
        if (existing != null && existing.activeAt(now) && !existing.actionId().equals(actionId)) return false;
        reservations.put(key, new Reservation(type, subject, actionId, now, now + Math.max(20L, duration)));
        trim();
        return true;
    }

    public void release(final ReservationType type, final String subject) {
        reservations.remove(key(type, subject));
    }

    public void releaseByAction(final UUID actionId) {
        if (actionId != null) reservations.entrySet().removeIf(entry -> entry.getValue().actionId().equals(actionId));
    }

    public int releaseExpired(final long now) {
        final int before = reservations.size();
        reservations.entrySet().removeIf(entry -> !entry.getValue().activeAt(now));
        return before - reservations.size();
    }

    public void clear() { reservations.clear(); }

    /** Releases claims whose action journal no longer exists or did not commit. */
    public int releaseMissingOrUncommitted(final ActionLedger ledger) {
        if (ledger == null) return 0;
        final int before = reservations.size();
        reservations.entrySet().removeIf(entry -> ledger.find(entry.getValue().actionId())
                .map(action -> action.phase() != ActionPhase.COMMITTED).orElse(true));
        return before - reservations.size();
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        final ListTag entries = new ListTag();
        reservations.values().forEach(reservation -> entries.add(reservation.save()));
        tag.put("Entries", entries);
        return tag;
    }

    public static ReservationBook load(final CompoundTag tag) {
        final ReservationBook book = new ReservationBook();
        if (tag == null) return book;
        final ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        final int first = Math.max(0, entries.size() - ENTRY_LIMIT);
        for (int i = first; i < entries.size(); i++) {
            Reservation.load(entries.getCompound(i)).ifPresent(reservation ->
                    book.reservations.put(key(reservation.type(), reservation.subjectKey()), reservation));
        }
        book.trim();
        return book;
    }

    private void trim() {
        while (reservations.size() > ENTRY_LIMIT) {
            final String first = reservations.keySet().iterator().next();
            reservations.remove(first);
        }
    }

    private static String key(final ReservationType type, final String subject) {
        return (type == null ? "UNKNOWN" : type.name()) + "|" + (subject == null ? "" : subject);
    }
}
