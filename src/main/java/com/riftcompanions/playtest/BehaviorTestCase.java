package com.riftcompanions.playtest;

import java.util.List;

/**
 * A deterministic source-side behavior acceptance briefing. It is not an
 * automated Forge test result and never marks a scenario as passed by itself.
 */
public record BehaviorTestCase(
        String id,
        String focus,
        String setup,
        String expected,
        List<String> requiredReasonCodes,
        String failureBoundary
) {
    public BehaviorTestCase {
        id = clip(id, 64);
        focus = clip(focus, 96);
        setup = clip(setup, 280);
        expected = clip(expected, 320);
        requiredReasonCodes = List.copyOf(requiredReasonCodes == null ? List.of() : requiredReasonCodes.stream()
                .filter(value -> value != null && !value.isBlank()).map(value -> clip(value, 64)).limit(8).toList());
        failureBoundary = clip(failureBoundary, 180);
    }

    private static String clip(final String value, final int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
