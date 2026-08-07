package com.riftcompanions.debug;

/** Developer-readable summary; never exposed as raw stack trace to normal UI. */
public record DecisionTraceEntry(long gameTime, String category, String detail) {}
