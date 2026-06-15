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
 * Pulls remote tech job postings from the public RemoteOK API.
 * Docs: https://remoteok.com/api - free, no API key required.
 */
@Component
@Slf4j
public class RemoteOkJobSource implements JobSource {

    private static final String API_URL = "https://remoteok.com/api";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getSourceName() {
        return "RemoteOK";
    }

    @Override
    public List<JobPosting> fetchJobs(String keyword) {

        List<JobPosting> jobs = new ArrayList<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    // RemoteOK blocks requests without a browser-like User-Agent
                    .header("User-Agent", "Mozilla/5.0 (compatible; JobEmailAgent/1.0; personal use)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("RemoteOK API returned status {}", response.statusCode());
                return jobs;
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (!root.isArray()) {
                log.warn("RemoteOK API returned unexpected response shape");
                return jobs;
            }

            String lowerKeyword = keyword == null ? "" : keyword.toLowerCase().trim();

            for (JsonNode node : root) {

                // The first element of the array is a legal/meta notice without an "id" field - skip it
                if (!node.has("id") || !node.has("position")) {
                    continue;
                }

                String title = node.path("position").asText("");
                String description = node.path("description").asText("");
                String tags = node.path("tags").isArray() ? node.path("tags").toString() : "";

                String searchable = (title + " " + description + " " + tags).toLowerCase();

                if (!lowerKeyword.isBlank() && !containsAllWords(searchable, lowerKeyword)) {
                    continue;
                }

                jobs.add(JobPosting.builder()
                        .source(getSourceName())
                        .externalId(node.path("id").asText())
                        .title(title)
                        .company(node.path("company").asText(""))
                        .location(node.path("location").asText("Remote"))
                        .description(description)
                        .url(node.path("url").asText(""))
                        .tags(tags)
                        .build());
            }

            log.info("RemoteOK: fetched {} matching jobs for keyword '{}'", jobs.size(), keyword);

        } catch (Exception e) {
            log.error("Failed to fetch jobs from RemoteOK", e);
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
