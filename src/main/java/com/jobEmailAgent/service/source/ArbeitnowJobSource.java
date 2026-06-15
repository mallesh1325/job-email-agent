package com.jobEmailAgent.service.source;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobEmailAgent.model.JobPosting;

import lombok.extern.slf4j.Slf4j;

/**
 * Pulls job postings from the public Arbeitnow Job Board API.
 * Docs: https://www.arbeitnow.com/api/job-board-api - free, no API key required.
 */
@Component
@Slf4j
public class ArbeitnowJobSource implements JobSource {

    private static final String API_URL = "https://www.arbeitnow.com/api/job-board-api";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getSourceName() {
        return "Arbeitnow";
    }

    @Override
    public List<JobPosting> fetchJobs(String keyword) {

        List<JobPosting> jobs = new ArrayList<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("User-Agent", "Mozilla/5.0 (compatible; JobEmailAgent/1.0; personal use)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Arbeitnow API returned status {}", response.statusCode());
                return jobs;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");

            if (!data.isArray()) {
                log.warn("Arbeitnow API returned unexpected response shape");
                return jobs;
            }

            String lowerKeyword = keyword == null ? "" : keyword.toLowerCase().trim();

            for (JsonNode node : data) {

                String title = node.path("title").asText("");
                String description = node.path("description").asText("");
                JsonNode tagsNode = node.path("tags");
                String tags = tagsNode.isArray() ? tagsNode.toString() : "";

                String searchable = (title + " " + description + " " + tags).toLowerCase();

                if (!lowerKeyword.isBlank() && !containsAllWords(searchable, lowerKeyword)) {
                    continue;
                }

                boolean remote = node.path("remote").asBoolean(false);

                jobs.add(JobPosting.builder()
                        .source(getSourceName())
                        .externalId(node.path("slug").asText())
                        .title(title)
                        .company(node.path("company_name").asText(""))
                        .location(remote ? "Remote" : node.path("location").asText(""))
                        .description(description)
                        .url(node.path("url").asText(""))
                        .tags(tags)
                        .build());
            }

            log.info("Arbeitnow: fetched {} matching jobs for keyword '{}'", jobs.size(), keyword);

        } catch (Exception e) {
            log.error("Failed to fetch jobs from Arbeitnow", e);
        }

        return jobs;
    }

    /** Returns true only if every word in the keyword phrase appears somewhere in the text */
    private boolean containsAllWords(String text, String keyword) {
        for (String word : keyword.split("\\s+")) {
            if (!word.isBlank() && !text.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
