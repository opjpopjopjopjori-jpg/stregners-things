package com.riftcompanions.story;

/**
 * Stranger Things Inspired Narrative Layer — adds mystery, group dynamics,
 * progressive revelation, emotional bonds, tension-building, and supernatural
 * elements to the 5-chapter framework.
 */
public final class StrangerThingsInspiredNarrator {
    private StrangerThingsInspiredNarrator() {}

    public static final String MYSTERY_OPENING = "Something is wrong. Not dangerous yet — just wrong. The air feels heavier. The companions feel it before anyone speaks.";
    public static final String GROUP_DYNAMIC = "Every character brings something different: Seer's observation, Guardian's defense, Gifted's protection, Scout's guidance. Together they are stronger than any single ability.";
    public static final String PROGRESSIVE_REVELATION = "Each chapter reveals something new — not everything at once. The truth unfolds slowly. The danger grows with understanding.";
    public static final String EMOTIONAL_STAKE = "If one companion falls, the team loses not just ability — they lose part of themselves. The bond is the real strength.";
    public static final String SUPERNAURAL_ELEMENT = "The world is not just blocks and biomes. Something older moves beneath — in caves, in storms, in the silence between observations.";

    public static String buildStrangerThingsStory(final java.util.List<String> chapters) {
        StringBuilder story = new StringBuilder();
        story.append(MYSTERY_OPENING).append("\n");
        story.append(GROUP_DYNAMIC).append("\n");
        story.append("\n=== CHAPTER SEQUENCE ===\n");
        for (String ch : chapters) {
            story.append(ch).append(": Mystery deepens. Something new is revealed. The group must trust each other more.\n");
        }
        story.append("\n").append(PROGRESSIVE_REVELATION).append("\n");
        story.append(EMOTIONAL_STAKE).append("\n");
        story.append(SUPERNAURAL_ELEMENT).append("\n");
        story.append("=== FINAL REVELATION === The truth was never about survival. It was about discovering what the team is capable of together.\n");
        return story.toString();
    }

    public static String mysteryDialogueHook(final String chapter, final String observation) {
        return "[MYSTERY] " + chapter + ": \"" + observation + "\" — this changes everything. We must understand before we act.";
    }
}
