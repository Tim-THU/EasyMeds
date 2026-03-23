package com.timthu.easymeds.domain;

import java.util.List;

public record GraphSymptomHint(String name, int supportCount, List<String> relatedDiseases) {
}
