package com.timthu.easymeds.service;

import com.timthu.easymeds.domain.GraphContext;

import java.util.Set;

public interface KnowledgeGraphGateway {

    GraphContext searchContext(Set<String> confirmedSymptoms, Set<String> deniedSymptoms);
}
