package com.timthu.easymeds.domain;

import java.util.List;

public record GraphContext(
        boolean graphAvailable,
        List<CandidateDisease> candidateDiseases,
        List<GraphSymptomHint> unconfirmedSymptoms
) {

    public static GraphContext empty(boolean graphAvailable) {
        return new GraphContext(graphAvailable, List.of(), List.of());
    }
}
