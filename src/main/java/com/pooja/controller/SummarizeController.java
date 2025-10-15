package com.pooja.controller;

import com.pooja.service.SummarizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/summarize")
public class SummarizeController {

    private final SummarizationService summarizationService;

    public SummarizeController(SummarizationService summarizationService) {
        this.summarizationService = summarizationService;
    }

    @PostMapping
    public Map<String, String> summarize(@RequestBody Map<String, String> request) {
        String input = request.get("text");
        String summary = summarizationService.summarizeText(input);
        String sentiment = summarizationService.analyzeSentiment(input);

        Map<String, String> response = new HashMap<>();
        response.put("summary", summary);
        response.put("sentiment", sentiment);
        return response;
    }

    @PostMapping("/search")

    public List<String> searchSimilarByEmotion(@RequestBody Map<String, String> request) {
        String query = request.get("text");
        String contextTag = request.getOrDefault("contextTag", "general");
        // Basic validation: reject generic or irrelevant queries
        if (contextTag.equalsIgnoreCase("general") || query == null || query.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or too generic query. Please provide a more specific context.");
        }
        return summarizationService.searchSimilarTicketsByContextAndSentiment(query, contextTag);
    }
}
