package com.pooja.service;

import com.pooja.entity.TokenCounter;
import com.pooja.util.TextChunker;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SummarizationService {

    private final ChatLanguageModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<String> embeddingStore;

    public SummarizationService(ChatLanguageModel chatLanguageModel,
                                EmbeddingModel embeddingModel,
                                InMemoryEmbeddingStore<String> embeddingStore) {
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * Summarizes a long support ticket by:
     * 1. Chunking it
     * 2. Summarizing each chunk
     * 3. Combining summaries into a final summary
     * 4. Generating an embedding and storing it in memory
     */
    public String summarizeText(String text) {
        List<String> chunks = TextChunker.chunkText(text);

        // Log: how many chunks and total tokens
        int chunkCount = chunks.size();
        int totalTokens = chunks.stream()
                .mapToInt(TokenCounter::countTokens)
                .sum();

        System.out.println("Chunking complete: " + chunkCount + " chunks created.");
        System.out.println("Estimated total tokens across chunks: " + totalTokens);

        // Summarize each chunk using the LLM
        List<String> chunkSummaries = chunks.stream()
                .map(chunk -> {
                    String prompt = "Summarize the following customer support ticket:\n\n" + chunk;
                    return chatLanguageModel.generate(prompt);
                })
                .collect(Collectors.toList());

        // Combine summaries and summarize again for final summary
        String combined = String.join("\n", chunkSummaries);
        String finalPrompt = "Summarize the following into a concise summary of maximum two lines:\n\n" + combined;
        String summary = chatLanguageModel.generate(finalPrompt);

        // Generate and store embedding
        Embedding embedding = embeddingModel.embed(text).content();
        String id = UUID.randomUUID().toString();
        embeddingStore.add(id, embedding, text);

        // Final message
        String infoMessage = "Summary generated from " + chunkCount + " chunks (" + totalTokens + " tokens). Embedding stored in in-memory vector store.";
        System.out.println(infoMessage);

        return summary ;
    }

    /**
     * Uses LLM to classify sentiment (positive/negative/neutral)
     */
    public String analyzeSentiment(String text) {
        try {
            String prompt = "Determine the sentiment (positive, negative, or neutral) of the following customer message:\n\n" + text;
            String response = chatLanguageModel.generate(prompt);

            if (response == null || response.trim().isEmpty()) {
                return "neutral"; // fallback
            }

            response = response.trim().toLowerCase();

            if (response.contains("positive")) return "positive";
            if (response.contains("negative")) return "negative";
            if (response.contains("neutral")) return "neutral";

            return "neutral"; // fallback if no keyword matched
        } catch (Exception e) {
            System.err.println("Error during sentiment analysis: " + e.getMessage());
            return "neutral"; // fallback on error
        }
    }

    /**
     * Finds top 3 semantically similar tickets from the in-memory vector store
     */

    public List<String> searchSimilarTicketsByContextAndSentiment(String queryText, String contextTag) {
        // Step 1: Enrich query with context
        String enrichedQuery = queryText + " related to " + contextTag;

        // Step 2: Get sentiment of enriched query
        String querySentiment = analyzeSentiment(enrichedQuery);

        // Step 3: Embed enriched query
        Embedding queryEmbedding = embeddingModel.embed(enrichedQuery).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(20) // Increase to allow more filtering
                .minScore(0.0)
                .build();
        EmbeddingSearchResult<String> result = embeddingStore.search(request);

        // Step 4: Filter by sentiment and context tag
        List<String> filtered = result.matches().stream()
                .map(match -> match.embedded())
                .filter(ticket -> analyzeSentiment(ticket).equals(querySentiment))
                .filter(ticket -> ticket.toLowerCase().contains(contextTag.toLowerCase()))
                .limit(3)
                .collect(Collectors.toList());

        // Step 5: Return informative message if no match
        if (filtered.isEmpty()) {
            return List.of("No similar tickets found with matching sentiment and context: " + querySentiment + ", " + contextTag);
        }

        return filtered;
    }
}