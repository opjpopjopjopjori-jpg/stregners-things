package com.riftcompanions.encounter;

import com.riftcompanions.combat.EncounterAdapterRegistry;
import com.riftcompanions.combat.EncounterIdentity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.world.assessment.StructurePerimeterAssessment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.List;

/** Vanilla baseline profiles and safe adapter-based fallbacks. */
public final class EncounterProfileRegistry {
    private EncounterProfileRegistry() {}

    public static WorldEncounterProfile biomeProfile(final ServerLevel level, final BlockPos pos) {
        ResourceLocation id = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(level.getBiome(pos).value());
        String key = id == null ? "unknown" : id.getPath();
        if (level.dimension() == net.minecraft.world.level.Level.NETHER) return nether();
        if (level.dimension() == net.minecraft.world.level.Level.END) return end();
        if (key.contains("ocean") || key.contains("beach") || key.contains("river")) return oceanCoast();
        if (key.contains("desert")) return desert();
        if (key.contains("mountain") || key.contains("peak") || key.contains("grove") || key.contains("stony")) return mountain();
        if (key.contains("forest") || key.contains("wood")) return denseForest();
        return unknownBiome(id == null ? "unknown" : id.toString());
    }

    public static WorldEncounterProfile structureProfile(final StructurePerimeterAssessment assessment) {
        String id = assessment.profileId();
        return switch (id) {
            case "village" -> village();
            case "dungeon" -> dungeon();
            case "mineshaft" -> mineshaft();
            case "ruined_portal" -> ruinedPortal();
            case "stronghold" -> stronghold();
            default -> unknownStructure();
        };
    }

    public static WorldEncounterProfile profileById(final String id) {
        if (id == null) return unknownStructure();
        return switch (id.toLowerCase(java.util.Locale.ROOT)) {
            case "village" -> village();
            case "dungeon" -> dungeon();
            case "mineshaft" -> mineshaft();
            case "ruined_portal" -> ruinedPortal();
            case "stronghold" -> stronghold();
            case "dense_forest" -> denseForest();
            case "mountain_cliff" -> mountain();
            case "desert" -> desert();
            case "ocean_coast" -> oceanCoast();
            case "nether" -> nether();
            case "end" -> end();
            case "swarm", "ambusher", "ranged_threat", "explosive_threat", "controller_mental", "heavy", "boss" -> threatProfile(ThreatArchetype.valueOf(id.toUpperCase(java.util.Locale.ROOT)));
            default -> unknownStructure();
        };
    }

    public static WorldEncounterProfile threatProfile(final ThreatArchetype archetype) {
        if (archetype == null) return profile("unknown_threat", EncounterType.THREAT, "unknown", "unknown", List.of(CompanionRole.GUARDIAN), List.of("Do not claim unobserved mechanics"), List.of("Observe", "Keep a route"), true);
        return switch (archetype) {
            case SWARM -> profile("swarm", EncounterType.THREAT, "crowding", "formation collapse", List.of(CompanionRole.GUARDIAN, CompanionRole.GIFTED, CompanionRole.SCOUT), List.of("No global freeze or farm automation"), List.of("Keep center clear", "Use a retreat route", "Use limited protection"), false);
            case AMBUSHER -> profile("ambusher", EncounterType.THREAT, "surprise contact", "blind pursuit", List.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT), List.of("Do not chase unseen target"), List.of("Regroup", "Verify route", "Use cover"), false);
            case RANGED_THREAT -> profile("ranged_threat", EncounterType.THREAT, "line of fire", "low cover", List.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT), List.of("Do not stand in one line"), List.of("Use cover", "Choose side route", "Retreat"), false);
            case EXPLOSIVE_THREAT -> profile("explosive_threat", EncounterType.THREAT, "blast risk", "protected area damage", List.of(CompanionRole.GUARDIAN, CompanionRole.GIFTED), List.of("No reckless melee near base"), List.of("Open ground", "Retreat", "Protect home"), false);
            case CONTROLLER_MENTAL -> profile("controller_mental", EncounterType.THREAT, "mental pressure", "team separation", List.of(CompanionRole.SEER, CompanionRole.SCOUT, CompanionRole.GIFTED), List.of("No unsupported mental effect claim"), List.of("Anchor", "Regroup", "Observe"), true);
            case HEAVY -> profile("heavy", EncounterType.THREAT, "high pressure", "blocked escape", List.of(CompanionRole.GUARDIAN, CompanionRole.GIFTED), List.of("No damage race assumption"), List.of("Keep exit", "Shield", "Retreat"), false);
            case BOSS -> profile("boss", EncounterType.THREAT, "phase mechanics", "arena separation", List.of(CompanionRole.GUARDIAN, CompanionRole.SEER), List.of("Adapter required for advanced behavior"), List.of("Observe", "Use survival plan", "Return later"), true);
            case UNKNOWN -> profile("unknown_threat", EncounterType.THREAT, "unknown", "unknown", List.of(CompanionRole.GUARDIAN), List.of("Do not claim mechanics without evidence"), List.of("Observe", "Keep route", "Retreat if needed"), true);
        };
    }

    public static ThreatArchetype threatArchetype(final Entity entity) {
        final ThreatProfileDefinition advisory = ThreatProfileRegistry.forEntity(entity).orElse(null);
        if (advisory != null) return advisory.archetype();
        var profile = EncounterAdapterRegistry.profileFor(entity);
        return switch (profile.identity()) {
            case CONTROLLER -> ThreatArchetype.CONTROLLER_MENTAL;
            case HEAVY -> ThreatArchetype.HEAVY;
            case HUNTER -> ThreatArchetype.AMBUSHER;
            case ANCHOR -> ThreatArchetype.BOSS;
            case SWARM -> ThreatArchetype.SWARM;
            case BOSS -> ThreatArchetype.BOSS;
            default -> vanillaArchetype(entity);
        };
    }

    private static ThreatArchetype vanillaArchetype(final Entity entity) {
        String id = String.valueOf(net.minecraft.world.entity.EntityType.getKey(entity.getType()));
        if (id.endsWith("creeper")) return ThreatArchetype.EXPLOSIVE_THREAT;
        if (id.endsWith("skeleton") || id.endsWith("stray")) return ThreatArchetype.RANGED_THREAT;
        if (id.endsWith("spider")) return ThreatArchetype.AMBUSHER;
        return ThreatArchetype.UNKNOWN;
    }

    private static WorldEncounterProfile denseForest() { return profile("dense_forest", EncounterType.BIOME, "formation loss", "surprise hostiles", List.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT, CompanionRole.SEER), List.of("Do not assume every forest contains a mystery", "Do not detect structures through chunks"), List.of("Mark a temporary route", "Move to a clearing", "Return before night"), false); }
    private static WorldEncounterProfile mountain() { return profile("mountain_cliff", EncounterType.BIOME, "fall damage", "separation", List.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT, CompanionRole.GIFTED), List.of("Do not force a compact group over a ledge"), List.of("Use a switchback", "Mark route and return", "Choose a shortcut deliberately"), false); }
    private static WorldEncounterProfile desert() { return profile("desert", EncounterType.BIOME, "night exposure", "low cover", List.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT, CompanionRole.SEER), List.of("Do not invent heat or thirst damage in vanilla"), List.of("Use visible cover", "Return before night", "Inspect visible landmark"), false); }
    private static WorldEncounterProfile oceanCoast() { return profile("ocean_coast", EncounterType.BIOME, "unsafe water follow", "separation", List.of(CompanionRole.SCOUT, CompanionRole.GUARDIAN), List.of("Do not promise autonomous boat intelligence", "Do not scout ocean chunks"), List.of("Use shore route", "Use short crossing", "Use recall policy"), false); }
    private static WorldEncounterProfile nether() { return profile("nether", EncounterType.BIOME, "lava and portal separation", "vertical terrain", List.of(CompanionRole.GUARDIAN, CompanionRole.GIFTED), List.of("No automatic portal entry", "No long lava scout"), List.of("Player enters first", "Validate landing", "Mark portal anchor"), true); }
    private static WorldEncounterProfile end() { return profile("end", EncounterType.BIOME, "void and portal separation", "boss arena", List.of(CompanionRole.GUARDIAN, CompanionRole.GIFTED), List.of("No automatic End entry", "No void recall"), List.of("Return later with adapter", "Validate landing"), true); }
    private static WorldEncounterProfile unknownBiome(String id) { return profile("unknown_biome:" + id, EncounterType.BIOME, "unknown", "unknown", List.of(CompanionRole.GUARDIAN), List.of("Do not claim unseen danger"), List.of("Observe", "Mark route", "Return if unsafe"), false); }
    private static WorldEncounterProfile village() { return profile("village", EncounterType.STRUCTURE, "civilian safety", "tight lanes", List.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT), List.of("No autonomous trade or chest access"), List.of("Player initiates interaction", "Protect villagers"), false); }
    private static WorldEncounterProfile dungeon() { return profile("dungeon", EncounterType.STRUCTURE, "spawner/trap", "limited exit", List.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT, CompanionRole.SEER), List.of("No loot rush", "No automatic block interaction"), List.of("Perimeter scan", "Mark and return", "Enter slowly"), false); }
    private static WorldEncounterProfile mineshaft() { return profile("mineshaft", EncounterType.STRUCTURE, "path complexity", "observed cave threats", List.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT), List.of("Do not assume cave spiders before evidence"), List.of("Use compact formation", "Mark exits"), false); }
    private static WorldEncounterProfile ruinedPortal() { return profile("ruined_portal", EncounterType.STRUCTURE, "landmark", "future dimension route", List.of(CompanionRole.GUARDIAN, CompanionRole.SEER), List.of("No companion activates portal"), List.of("Player chooses repair", "Mark landmark"), false); }
    private static WorldEncounterProfile stronghold() { return profile("stronghold", EncounterType.STRUCTURE, "late-game complexity", "portal risk", List.of(CompanionRole.GUARDIAN, CompanionRole.SEER), List.of("Do not claim portal knowledge before discovery"), List.of("Journal discovery", "Prepare return route"), true); }
    private static WorldEncounterProfile unknownStructure() { return profile("unknown_structure", EncounterType.STRUCTURE, "unknown", "limited exits", List.of(CompanionRole.GUARDIAN), List.of("No forced entry"), List.of("Perimeter scan", "Mark", "Return later"), false); }

    private static WorldEncounterProfile profile(String id, EncounterType type, String primary, String secondary, List<CompanionRole> roles, List<String> forbidden, List<String> choices, boolean adapter) {
        return new WorldEncounterProfile(id, type, List.of(primary, secondary), primary, secondary, roles, forbidden, choices,
                List.of("Player-visible evidence", "Safe route or player approval"), List.of("Player cancel", "Threat cleared", "Timeout"),
                List.of("memory:" + id), List.of("encounter:" + id), adapter);
    }
}
