package com.riftcompanions.compat;

/**
 * AI Mod Integration Instructions — when integrating with any new mod,
 * the AI MUST generate the following for every new entity/biome/item:
 */
public final class AIModIntegrationInstructions {
    private AIModIntegrationInstructions() {}

    public static final String MANDATORY_FEATURES = """
        REQUIRED FOR EVERY NEW MOD INTEGRATION:
        1. NEW DIALOGUE: Character must speak specific facts about new biomes.
        2. NEW DIALOGUE: Character must observe new creature behavior (e.g., breathes fire, moves fast, hostile at night).
        3. NEW INTERACTION: Role-specific response to new creature (Guardian defends, Seer observes, Gifted protects, Scout signals).
        4. NEW STORY ELEMENT: Include new creature/biome/item in at least one of the 5 chapters (CH01-CH05).
        5. NEW OBSERVATION: Character explains exact reason for retreat/defense based on new entity behavior (e.g., "It breathes fire — arrows and concealment required").
        6. NEW BIOME FACT: Character must mention the biome by name with accurate description.
        7. NO GENERIC WARNINGS. Every line references specific discovered feature from the namespace.
        """;

    public static String buildInstructionsForNamespace(final String namespace) {
        return "Namespace: " + namespace + "\n" + MANDATORY_FEATURES +
               "Generate complete story with these elements included professionally.";
    }
}
