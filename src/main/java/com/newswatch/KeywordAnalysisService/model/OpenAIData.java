package com.newswatch.KeywordAnalysisService.model;

public class OpenAIData {
    private String openAI;   // If OpenAI is always a single string
    private Integer count;

    public OpenAIData(String openAI, Integer count) {
        this.openAI = openAI;
        this.count = count;
    }

    // Getter for openAI
    public String getOpenAI() {
        return openAI;
    }

    // Getter for count
    public Integer getCount() {
        return count;
    }

    // Optionally, you can add setter methods if you need them
}