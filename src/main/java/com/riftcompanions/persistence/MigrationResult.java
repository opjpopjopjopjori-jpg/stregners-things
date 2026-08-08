package com.riftcompanions.persistence;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

/** Result object keeps a migration failure visible and recoverable instead of silent. */
public record MigrationResult(boolean successful, CompoundTag tag, int sourceVersion, int targetVersion, List<String> warnings) {
    public String summary() {
        return warnings.isEmpty() ? "No migration warnings." : String.join("; ", warnings);
    }
}
