package com.riftcompanions.conversation;

import java.util.Optional;

/** Bounded player-led conversation topics; not open-ended text generation. */
public enum ConversationTopic {
    PLAN,
    ARE_YOU_OKAY,
    MEMORY,
    PLACE,
    ROLE_POLICY,
    ACCEPT_PLAN,
    DELAY_PLAN,
    OFFER_REST,
    WORLD,
    TEAM,
    LAST_ENCOUNTER,
    CHECK_IN;

    public static Optional<ConversationTopic> byId(final int id) {
        final ConversationTopic[] values = values();
        return id >= 0 && id < values.length ? Optional.of(values[id]) : Optional.empty();
    }
}
