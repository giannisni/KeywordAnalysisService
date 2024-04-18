package com.newswatch.KeywordAnalysisService.controller;

import com.newswatch.KeywordAnalysisService.service.KeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class KeywordController {

    private KeywordService keywordService; // Inject the service



    @Autowired
    public KeywordController(KeywordService keywordAnalysisService) {

        this.keywordService = keywordAnalysisService;

    }

    @GetMapping("/highlights")
    public ResponseEntity<Map<String, List<String>>> getHighlights(
            @RequestParam String term1,
            @RequestParam String term2,
            @RequestParam String indexName,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate startLocalDate = LocalDate.parse(startDate);
            LocalDate endLocalDate = LocalDate.parse(endDate);
            Map<String, List<String>> highlights = keywordService.fetchHighlights(term1, term2, indexName, startLocalDate, endLocalDate);
            return ResponseEntity.ok(highlights);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/term-percentages")
    public ResponseEntity<Map<String, Float>> getTermPercentages(
            @RequestParam String term1,
            @RequestParam String term2,
            @RequestParam String term3,
            @RequestParam String term4,
            @RequestParam String indexName,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            Map<String, Float> percentages = keywordService.fetchTermPercentages(
                    term1, term2, term3, term4, indexName, start, end);
            return ResponseEntity.ok(percentages);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    //
    @GetMapping("/keyword-percentages")
    public Map<String, Object> getKeywordPercentages(
            @RequestParam String keyword1,
            @RequestParam String keyword2,
            @RequestParam String indexName) {

        try {
            return keywordService.fetchKeywordPercentages(keyword1, keyword2, indexName)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Object) entry.getValue()));
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "An error occurred while fetching keyword percentages");
        }
    }
}
