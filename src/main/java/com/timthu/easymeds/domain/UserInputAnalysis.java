package com.timthu.easymeds.domain;

import java.util.List;

public record UserInputAnalysis(
        UserIntent intent,
        List<String> presentSymptoms,
        List<String> absentSymptoms,
        List<String> unknownSymptoms,
        boolean shouldFinish
) {
}
