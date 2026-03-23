package com.timthu.easymeds.domain;

import java.util.List;

public record CandidateDisease(String name, int matchedCount, List<String> matchedSymptoms) {
}
