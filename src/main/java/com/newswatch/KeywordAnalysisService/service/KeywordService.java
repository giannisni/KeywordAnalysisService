package com.newswatch.KeywordAnalysisService.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;


import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;

import com.newswatch.KeywordAnalysisService.model.Document;
import com.newswatch.KeywordAnalysisService.model.OpenAIData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class KeywordService {


    @Autowired
    private RestTemplate restTemplate;
    private final ElasticsearchClient elasticsearchClient;
//    private final ElasticsearchUtilityService elasticsearchUtilityService;

    @Autowired
    public KeywordService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
//        this.elasticsearchUtilityService = elasticsearchUtilityService;
    }

    /**
     * Retrieves keyword counts from Elasticsearch for a given keyword and date range.
     *
//     * @param keyword    The keyword to search for.
//     * @param startDate  The start date of the range.
//     * @param endDate    The end date of the range.
     * @return A map of dates to keyword counts.
     * @throws IOException If there's an issue communicating with Elasticsearch.
     */



    public List<Map<String, Object>> getWordCloudData(String indexName) throws IOException {
        // Create search request
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q
                        .matchAll(new MatchAllQuery.Builder().build())
                )
                .size(10000) // Adjust the size as needed
        );

        // Execute the search
        SearchResponse<Object> searchResponse = elasticsearchClient.search(searchRequest, Object.class);

        // Process the search hits
        List<Map<String, Object>> wordCloudData = new ArrayList<>();
        for (Hit<Object> hit : searchResponse.hits().hits()) {
            Map<String, Object> sourceAsMap = (Map<String, Object>) hit.source();
            String keyword = (String) sourceAsMap.get("keyword");
            Double frequency = (Double) sourceAsMap.get("frequency");
            wordCloudData.add(Map.of("text", keyword, "value", frequency));
        }

        return wordCloudData;
    }


    //add javadoc for fetchOpenAIData method

    public List<OpenAIData> fetchOpenAIData(String indexName) throws IOException {
        // Define the search query
        SearchResponse<Map> searchResponse = elasticsearchClient.search(s -> s
                        .index(indexName)
                        .size(1000) // Adjust size as needed
                        .query(q -> q
                                .matchAll(m -> m)
                        ),
                Map.class
        );

        // Process the search hits
        List<OpenAIData> openAIDataList = new ArrayList<>();
        for (Hit<Map> hit : searchResponse.hits().hits()) {
            Map<String, Object> sourceAsMap = hit.source();
            Object openAIObject = sourceAsMap.get("OpenAI");

            String openAIString = "";
            if (openAIObject instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> openAIList = (List<String>) openAIObject;
                openAIString = String.join(", ", openAIList); // Concatenate list into a string
            } else if (openAIObject instanceof String) {
                openAIString = (String) openAIObject; // Use it directly if it's already a string
            }

            Integer count = (Integer) sourceAsMap.get("Count");
            openAIDataList.add(new OpenAIData(openAIString, count));
        }

        return openAIDataList;
    }

    //add javadoc of getKeywordCounts method
    /**
     * Retrieves keyword counts from Elasticsearch for a given keyword and date range.
     *
     * @param index     The name of the Elasticsearch index.
     * @param keyword   The keyword to search for.
     * @param startDate The start date of the range.
     * @param endDate   The end date of the range.
     * @return A map of dates to keyword counts.
     * @throws IOException If there's an issue communicating with Elasticsearch.
     */
    public Map<LocalDate, Integer> getKeywordCounts(String index, String keyword, LocalDate startDate, LocalDate endDate) throws IOException {
        Map<LocalDate, Integer> keywordCounts = new HashMap<>();

//        System.out.println("index : "+ index);

        // Build the request
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(index)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .term(t -> t
                                                .field("keyword.keyword")
                                                .value(keyword)
                                        )
                                )
                                .filter(f -> f
                                        .range(r -> r
                                                .field("date")
                                                .gte(JsonData.of(startDate.toString())) // Keep using JsonData.of() with LocalDate.toString()
                                                .lte(JsonData.of(endDate.toString()))
                                        )
                                )
                        )
                )
                .size(10000) // Adjust size as needed
        );

        // Execute the search
        SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

//        System.out.println("response " + response);
        // Process the results
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            LocalDate date = LocalDate.parse((String) source.get("date"));
            Integer value = (Integer) source.get("value");
            keywordCounts.put(date, value);
        }

        // Close the client

        return keywordCounts;
    }

    //add javadoc for getTopKeywordCounts method

    /**
     * Retrieves the top 3 keyword counts from a map of keyword counts.
     *
     * @param keywordCounts The map of keyword counts.
     * @return A map of the top 3 keyword counts.
     */
    public static Map<LocalDate, Integer> getTopKeywordCounts(Map<LocalDate, Integer> keywordCounts) {
        // Sort the map entries by their values (integer counts) in descending order
        // and limit the results to the top 3 entries
        Map<LocalDate, Integer> topKeywordCounts = keywordCounts.entrySet()
                .stream()
                .sorted(Map.Entry.<LocalDate, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new // Use LinkedHashMap to maintain order
                ));

        return topKeywordCounts;
    }



    /**

     * Fetches the percentage of documents containing a given pair of terms from a specified index.
     *
     * @param term1     The first term.
     * @param term2     The second term.
     * @param term3     The third term.
     * @param term4     The fourth term.
     * @param indexName The name of the Elasticsearch index.
     * @param startDate The start date of the range.
     * @param endDate   The end date of the range.
     * @return A map of term pairs to their respective percentages.
     * @throws IOException If there's an issue communicating with Elasticsearch.
     */

        public Map<String, Float> fetchTermPercentages(
                String term1, String term2, String term3, String term4,
                String indexName, LocalDate startDate, LocalDate endDate) throws IOException {

//            long count1 = elasticsearchUtilityService.executeCountQuery(term1, term2, indexName, startDate, endDate);
//            long count2 = elasticsearchUtilityService.executeCountQuery(term3, term4, indexName, startDate, endDate);

            String urlTemplate = "http://localhost:8081/api/news/count?term1={term1}&term2={term2}&indexName={indexName}&startDate={startDate}&endDate={endDate}";

            long count1 = restTemplate.getForObject(urlTemplate, Long.class, term1, term2, indexName, startDate, endDate);
            long count2 = restTemplate.getForObject(urlTemplate, Long.class, term3, term4, indexName, startDate, endDate);


            long total = count1 + count2;
            float percentage1 = total > 0 ? (float) count1 / total * 100 : 0;
            float percentage2 = total > 0 ? (float) count2 / total * 100 : 0;

            return Map.of(
                    term1 + " " + term2, percentage1,
                    term3 + " " + term4, percentage2
            );
        }

    //generate javadoc for executeCountQuery method

    /**
     * Fetches highlights for a given pair of terms from a specified index.
     *
     * @param term1     The first term.
     * @param term2     The second term.
     * @param indexName The name of the Elasticsearch index.
     * @param startDate The start date of the range.
     * @param endDate   The end date of the range.
     * @return A map of document IDs to lists of highlights.
     * @throws IOException If there's an issue communicating with Elasticsearch.
     */
    public Map<String, List<String>> fetchHighlights(String term1, String term2, String indexName, LocalDate startDate, LocalDate endDate) throws IOException {
        Map<String, List<String>> highlights = new HashMap<>();

        SearchResponse<Document> response = elasticsearchClient.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .spanNear(n -> n
                                        .clauses(List.of(
                                                SpanQuery.of(c -> c.spanTerm(st -> st.field("text").value(term1))),
                                                SpanQuery.of(c -> c.spanTerm(st -> st.field("text").value(term2)))
                                        ))
                                        .slop(50)
                                        .inOrder(false)
                                )
                        )
                        .highlight(h -> h
                                .fields("text", f -> f
                                        .preTags("<em>")
                                        .postTags("</em>")
                                )
                        )
                        .size(10)
                , Document.class);

        for (Hit<Document> hit : response.hits().hits()) {
            String documentId = hit.id();
            List<String> highlightTexts = new ArrayList<>();

            if (hit.highlight() != null && hit.highlight().containsKey("text")) {
                // Use the list of strings directly
                highlightTexts = hit.highlight().get("text");
            }

            highlights.put(documentId, highlightTexts);
        }

        return highlights;
    }


    //Adjust an Elasticsearch service method in Java to include an optional search term for the title field using the Elasticsearch Java client 8.7.0, ensuring compatibility with the method's access modifiers and correctly constructing the conditional query logic




    //add javadoc for fetchKeywordPercentages method
    /**
     * Fetches the percentage of documents containing a given pair of keywords from a specified index.
     *
     * @param keyword1   The first keyword.
     * @param keyword2   The second keyword.
     * @param indexName  The name of the Elasticsearch index.
     * @return A map of keywords to their respective percentages.
     * @throws IOException If there's an issue communicating with Elasticsearch.
     */
    public Map<String, Float> fetchKeywordPercentages(
            String keyword1, String keyword2, String indexName) throws IOException {
        try {
//            long count1 = elasticsearchUtilityService.executeCountQuery(keyword1, indexName);
//            long count2 = elasticsearchUtilityService.executeCountQuery(keyword2, indexName);

            String serviceUrl = "http://localhost:8081/api/news/count?keyword={keyword}&indexName={indexName}";

            // Fetch counts for each keyword
            Map<String, String> uriVariables1 = Map.of("keyword", keyword1, "indexName", indexName);
            long count1 = restTemplate.getForObject(serviceUrl, Long.class, uriVariables1);

            Map<String, String> uriVariables2 = Map.of("keyword", keyword2, "indexName", indexName);
            long count2 = restTemplate.getForObject(serviceUrl, Long.class, uriVariables2);

            // Calculate the total count and percentages
            long total = count1 + count2;
            float percentage1 = total > 0 ? (float) count1 / total * 100 : 0;
            float percentage2 = total > 0 ? (float) count2 / total * 100 : 0;

            // Return a map with keywords mapped to their respective percentages
            return Map.of(
                    keyword1, percentage1,
                    keyword2, percentage2
            );

        } catch (Exception e) {
            // Handle exception
            throw new RuntimeException("Error fetching keyword percentages", e);
        }
    }

    // Method to execute count query for a specific keyword





}
