package com.zufar.icedlatte.ai.dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TextModerationResult {

    private String id;
    private String model;
    private List<Result> results;

    @Setter
    @Getter
    public static class Result {

        private boolean flagged;
        private Map<String, Boolean> categories;
        private Map<String, Double> category_scores;

    }

}

