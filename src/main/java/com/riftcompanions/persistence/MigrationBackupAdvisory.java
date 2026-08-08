package com.riftcompanions.persistence;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes a small local advisory, never an automatic copy of a live world. A
 * full world backup remains the player's deliberate filesystem operation.
 */
public final class MigrationBackupAdvisory {
    private MigrationBackupAdvisory() {}

    public static String write(final ServerPlayer player, final String detail) {
        if (player == null || player.server == null) return "Migration backup advisory could not be written.";
        try {
            final Path directory = player.server.getWorldPath(LevelResource.ROOT).resolve("riftcompanions-reports");
            Files.createDirectories(directory);
            final Path file = directory.resolve("migration-backup-required.txt");
            final String body = "Rift Companions migration advisory\n\n"
                    + "A save migration requires review before optional systems are resumed.\n"
                    + "Create a manual copy of the complete world folder before clearing Safe Mode.\n"
                    + "Reason: " + (detail == null ? "Unknown migration issue." : detail) + "\n";
            Files.writeString(file, body, StandardCharsets.UTF_8);
            return "A local migration backup advisory was written to " + file.getFileName() + ".";
        } catch (final IOException exception) {
            return "Migration backup advisory could not be written: " + exception.getClass().getSimpleName() + ".";
        }
    }
}
