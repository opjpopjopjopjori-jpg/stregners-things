package com.riftcompanions.transaction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-team bounded action journal. It is deliberately small: it protects save
 * recovery and idempotency without becoming an unbounded event log.
 */
public final class ActionLedger {
    private static final int ENTRY_LIMIT = 96;

    private final LinkedHashMap<UUID, ActionTransaction> transactions = new LinkedHashMap<>();

    public BeginResult begin(final ActionType type, final String subjectKey, final String fingerprint,
                             final long now, final long ttlTicks) {
        final Optional<ActionTransaction> open = findOpen(type, subjectKey);
        if (open.isPresent()) return BeginResult.reused(open.get());
        final ActionTransaction created = ActionTransaction.begin(type, subjectKey, fingerprint, now, ttlTicks);
        transactions.put(created.actionId(), created);
        trim();
        return BeginResult.created(created);
    }

    public Optional<ActionTransaction> find(final UUID id) {
        return Optional.ofNullable(transactions.get(id));
    }

    public Optional<ActionTransaction> findOpen(final ActionType type, final String subjectKey) {
        for (final ActionTransaction transaction : transactions.values()) {
            if (transaction.type() == type && transaction.subjectKey().equals(subjectKey) && transaction.isOpen()) {
                return Optional.of(transaction);
            }
        }
        return Optional.empty();
    }

    /** Completed records are retained long enough to make milestone writes idempotent across reload. */
    public boolean hasCommitted(final ActionType type, final String subjectKey) {
        for (final ActionTransaction transaction : transactions.values()) {
            if (transaction.type() == type && transaction.subjectKey().equals(subjectKey)
                    && transaction.phase() == ActionPhase.COMMITTED) return true;
        }
        return false;
    }

    /** Returns the number of incomplete actions made terminal during recovery. */
    public int recoverIncomplete(final long now, final String reason) {
        int recovered = 0;
        for (final ActionTransaction transaction : transactions.values()) {
            if (transaction.isOpen() && transaction.rollback(now, reason)) recovered++;
        }
        trim();
        return recovered;
    }

    public int expire(final long now) {
        int expired = 0;
        for (final ActionTransaction transaction : transactions.values()) {
            if (transaction.expire(now)) expired++;
        }
        trim();
        return expired;
    }

    public List<ActionTransaction> recent(final int limit) {
        final List<ActionTransaction> values = new ArrayList<>(transactions.values());
        final int first = Math.max(0, values.size() - Math.max(0, limit));
        return List.copyOf(values.subList(first, values.size()));
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        final ListTag entries = new ListTag();
        for (final ActionTransaction transaction : transactions.values()) entries.add(transaction.save());
        tag.put("Entries", entries);
        return tag;
    }

    public static ActionLedger load(final CompoundTag tag) {
        final ActionLedger ledger = new ActionLedger();
        if (tag == null) return ledger;
        final ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        final int first = Math.max(0, entries.size() - ENTRY_LIMIT);
        for (int i = first; i < entries.size(); i++) {
            ActionTransaction.load(entries.getCompound(i)).ifPresent(transaction -> ledger.transactions.put(transaction.actionId(), transaction));
        }
        ledger.trim();
        return ledger;
    }

    private void trim() {
        while (transactions.size() > ENTRY_LIMIT) {
            UUID removable = null;
            for (final Map.Entry<UUID, ActionTransaction> entry : transactions.entrySet()) {
                if (entry.getValue().phase().terminal()) {
                    removable = entry.getKey();
                    break;
                }
            }
            if (removable == null) break; // open work is never silently discarded
            transactions.remove(removable);
        }
    }

    public record BeginResult(ActionTransaction transaction, boolean reused) {
        private static BeginResult created(final ActionTransaction transaction) { return new BeginResult(transaction, false); }
        private static BeginResult reused(final ActionTransaction transaction) { return new BeginResult(transaction, true); }
    }
}
