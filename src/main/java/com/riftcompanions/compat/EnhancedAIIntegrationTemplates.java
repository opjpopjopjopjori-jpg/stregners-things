package com.riftcompanions.compat;

import java.util.*;

/**
 * Enhanced Professional AI Integration Templates v5.0
 * Concrete examples, structured output formats, zero ambiguity.
 */
public final class EnhancedAIIntegrationTemplates {
    private EnhancedAIIntegrationTemplates() {}

    public static final String DRAGON_EXAMPLE = """
        EXAMPLE: Dragon from Ice and Fire namespace.
        Dialogue (Seer): "The dragon breathes intense fire — I observe its wings spreading and heat rising."
        Dialogue (Guardian): "What is happening exactly?"
        Dialogue (Seer): "The reason is clear: it breathes fire. Suggested action: arrows and concealment required immediately."
        Dialogue (Gifted): "I will prepare Shield — protected-boundary checks pass."
        Dialogue (Scout): "Nearby ice tower structure offers safe shelter. Route marked."
        Story Element: CH03 Combat must include dragon defeat or retreat.
        Biome Fact: Frost desert biome — extreme cold, visible ice crystals, reduced visibility.
        """;

    public static final String BIOME_EXAMPLE = """
        EXAMPLE: Fire forest biome.
        Dialogue (Seer): "We enter the fire forest — trees glow with ember light, ground is warm ash."
        Dialogue (Scout): "Safe route through dense trees — signal placed near fire-proof stone structure."
        Dialogue (Guardian): "Hostile fire-elementals detected. Defensive formation required."
        Story Element: CH02 Discovery references finding path through fire forest.
        """;

    public static final String STRUCTURED_OUTPUT_FORMAT = """
        OUTPUT FORMAT FOR AI:
        [CHAPTER_ID] [ROLE_NAME]: [SPECIFIC_OBSERVATION] — [REASON] — [ACTION]
        Example CH03 Guardian: "Dragon detected — breathes fire — defensive arrows/concealment required."
        Every line must reference discovered namespace feature by exact name.
        No omissions. No generic text.
        """;

    public static String buildCompleteTemplate(final String namespace) {
        return "Namespace: " + namespace + "\n" +
               AIModIntegrationInstructions.MANDATORY_FEATURES + "\n" +
               "CONCRETE EXAMPLES:\n" + DRAGON_EXAMPLE + "\n" + BIOME_EXAMPLE +
               "\nOUTPUT FORMAT:\n" + STRUCTURED_OUTPUT_FORMAT;
    }
}
