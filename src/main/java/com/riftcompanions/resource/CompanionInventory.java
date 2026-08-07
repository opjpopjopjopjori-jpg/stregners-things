package com.riftcompanions.resource;

import com.riftcompanions.persistence.SaveVersions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Six bounded personal supply slots. It is deliberately not a remote chest,
 * cannot be used by recall across dimensions, and has no automatic pickup by
 * default. A resting snapshot can preserve these slots only through the
 * server-side roster transaction; clients never receive its item data.
 */
public final class CompanionInventory {
    public static final int SLOT_COUNT = 6;
    private final List<ItemStack> slots = new ArrayList<>(SLOT_COUNT);
    private int lastLoadWarningCount;

    public CompanionInventory() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots.add(ItemStack.EMPTY);
        }
    }

    public int size() {
        return SLOT_COUNT;
    }

    public ItemStack get(final int slot) {
        return valid(slot) ? slots.get(slot).copy() : ItemStack.EMPTY;
    }

    public int occupiedSlots() {
        return (int) slots.stream().filter(stack -> !stack.isEmpty()).count();
    }

    /** Invalid saved entries are ignored, never replaced with generated items. */
    public int lastLoadWarningCount() { return lastLoadWarningCount; }

    /** Inserts one explicitly handed item. Returns true only after the source is actually reduced. */
    public boolean insertOneFrom(final ItemStack source) {
        if (source == null || source.isEmpty()) {
            return false;
        }
        // Merge first, retaining Minecraft's normal stack-size rules.
        for (int i = 0; i < SLOT_COUNT; i++) {
            final ItemStack existing = slots.get(i);
            if (ItemStack.isSameItemSameTags(existing, source) && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1);
                source.shrink(1);
                return true;
            }
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots.get(i).isEmpty()) {
                final ItemStack one = source.copy();
                one.setCount(1);
                slots.set(i, one);
                source.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Removes the entire stack from one explicit slot; caller owns the returned stack. */
    public ItemStack withdraw(final int slot) {
        if (!valid(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack result = slots.get(slot);
        slots.set(slot, ItemStack.EMPTY);
        return result;
    }

    public List<ItemStack> snapshot() {
        return slots.stream().map(ItemStack::copy).toList();
    }

    /** Empties the bounded inventory for safe dismissal; items are returned to the player, never copied. */
    public List<ItemStack> withdrawAll() {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = slots.get(i);
            if (!stack.isEmpty()) {
                result.add(stack);
                slots.set(i, ItemStack.EMPTY);
            }
        }
        return result;
    }

    public void save(final CompoundTag ownerTag) {
        ownerTag.putInt("PersonalInventoryDataVersion", SaveVersions.INVENTORY_DATA);
        final ListTag entries = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            final ItemStack stack = slots.get(i);
            if (!stack.isEmpty()) {
                final CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                entries.add(itemTag);
            }
        }
        ownerTag.put("PersonalInventory", entries);
    }

    public void load(final CompoundTag ownerTag) {
        lastLoadWarningCount = 0;
        for (int i = 0; i < SLOT_COUNT; i++) slots.set(i, ItemStack.EMPTY);
        final ListTag entries = ownerTag.getList("PersonalInventory", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            final CompoundTag itemTag = entries.getCompound(i);
            final int slot = itemTag.getByte("Slot") & 255;
            if (!valid(slot) || !slots.get(slot).isEmpty()) {
                lastLoadWarningCount++;
                continue;
            }
            final ItemStack loaded = ItemStack.of(itemTag);
            if (loaded.isEmpty() || loaded.getCount() <= 0 || loaded.getCount() > loaded.getMaxStackSize()) {
                lastLoadWarningCount++;
                continue;
            }
            slots.set(slot, loaded);
        }
    }

    /**
     * Converts a server-owned resting snapshot into concrete item stacks only
     * when the owner deliberately dismisses that companion. This is never used
     * for a client preview or a remote inventory read.
     */
    public static List<ItemStack> drainRestingSnapshot(final CompoundTag snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return List.of();
        final CompanionInventory inventory = new CompanionInventory();
        inventory.load(snapshot);
        return inventory.withdrawAll();
    }

    private static boolean valid(final int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }
}
