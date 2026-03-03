package com.dsi.support.agenticrouter.dto;

import java.util.List;

public record PatternHint(
    String category,
    String successfulAction,
    double successRate,
    int sampleCount,
    List<String> keywords
) {}
