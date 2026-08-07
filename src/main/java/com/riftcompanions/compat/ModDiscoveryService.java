package com.riftcompanions.compat;

import java.util.Set;

/** Service linking UniversalModIntegrationEngine to StoryService and AI prompts. */
public final class ModDiscoveryService {
    private ModDiscoveryService() {}

    public static Set<String> discoverForNamespace(final String namespace) {
        return UniversalModIntegrationEngine.discoverFeatures(namespace);
    }

    public static boolean isCompatible(final String namespace) {
        return UniversalModIntegrationEngine.supportsNamespace(namespace);
    }

    public static String buildAIPrompt(final String namespace) {
        return UniversalModIntegrationEngine.generateIntegrationPrompt(namespace);
    }
}
