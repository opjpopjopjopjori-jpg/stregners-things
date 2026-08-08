package com.riftcompanions.memory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** Compact learned facts for one EntityType identifier. */
public record ThreatKnowledgeRecord(
        String entityTypeId,
        ThreatKnowledgeStage stage,
        int sightings,
        int directDamageEvents,
        long lastObservedDay,
        List<String> facts
) {
    private static final int FACT_LIMIT = 4;

    public ThreatKnowledgeRecord {
        entityTypeId = entityTypeId == null ? "unknown" : entityTypeId.substring(0, Math.min(120, entityTypeId.length()));
        sightings = Math.max(0, Math.min(9999, sightings));
        directDamageEvents = Math.max(0, Math.min(9999, directDamageEvents));
        facts = List.copyOf(facts == null ? List.of() : facts.stream().limit(FACT_LIMIT)
                .map(fact -> fact == null ? "" : fact.substring(0, Math.min(96, fact.length()))).toList());
    }

    public static ThreatKnowledgeRecord first(final String typeId, final long day) {
        return new ThreatKnowledgeRecord(typeId, ThreatKnowledgeStage.UNKNOWN, 0, 0, day, List.of());
    }

    public ThreatKnowledgeRecord observe(final ThreatObservationKind kind, final long day) {
        int nextSightings = sightings;
        int nextDamage = directDamageEvents;
        ThreatKnowledgeStage nextStage = stage;
        final List<String> nextFacts = new ArrayList<>(facts);
        switch (kind) {
            case VISUAL_CONTACT -> {
                nextSightings++;
                if (nextSightings >= 2 && nextStage == ThreatKnowledgeStage.UNKNOWN) {
                    nextStage = ThreatKnowledgeStage.OBSERVED;
                }
            }
            case DIRECT_DAMAGE -> {
                nextSightings++;
                nextDamage++;
                if (nextStage == ThreatKnowledgeStage.UNKNOWN) {
                    nextStage = ThreatKnowledgeStage.OBSERVED;
                }
                addFact(nextFacts, "Observed direct damage");
            }
            case HIVE_TAG_DETECTED -> {
                nextSightings++;
                if (nextStage == ThreatKnowledgeStage.UNKNOWN) {
                    nextStage = ThreatKnowledgeStage.OBSERVED;
                }
                addFact(nextFacts, "Has Hive-linked compatibility tag");
            }
        }
        return new ThreatKnowledgeRecord(entityTypeId, nextStage, nextSightings, nextDamage, day, nextFacts);
    }

    private static void addFact(final List<String> facts, final String fact) {
        if (!facts.contains(fact) && facts.size() < FACT_LIMIT) {
            facts.add(fact);
        }
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("EntityType", entityTypeId);
        tag.putString("Stage", stage.name());
        tag.putInt("Sightings", sightings);
        tag.putInt("DirectDamage", directDamageEvents);
        tag.putLong("LastDay", lastObservedDay);
        final ListTag factTags = new ListTag();
        facts.forEach(fact -> factTags.add(StringTag.valueOf(fact)));
        tag.put("Facts", factTags);
        return tag;
    }

    public static ThreatKnowledgeRecord load(final CompoundTag tag) {
        try {
            final List<String> facts = new ArrayList<>();
            final ListTag factTags = tag.getList("Facts", Tag.TAG_STRING);
            for (int i = 0; i < factTags.size() && i < FACT_LIMIT; i++) {
                facts.add(factTags.getString(i));
            }
            return new ThreatKnowledgeRecord(tag.getString("EntityType"), ThreatKnowledgeStage.valueOf(tag.getString("Stage")),
                    tag.getInt("Sightings"), tag.getInt("DirectDamage"), tag.getLong("LastDay"), facts);
        } catch (final IllegalArgumentException exception) {
            return first("invalid", 0L);
        }
    }
}
