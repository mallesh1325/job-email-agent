package com.jobEmailAgent.service.source;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobEmailAgent.model.JobPosting;

import lombok.extern.slf4j.Slf4j;

/**
 * Pulls job postings from the Adzuna job search API (US index).
 * Docs: https://developer.adzuna.com/ - requires a free app_id/app_key.
 *
 * If adzuna.app-id / adzuna.app-key are not configured, this source is
 * skipped (returns an empty list) so the rest of the pipeline keeps working.
 */
@Component
@Slf4j
public class AdzunaJobSource implements JobSource {

    private static final String API_URL = "https://api.adzuna.com/v1/api/jobs/us/search/1";

    @Value("${adzuna.app-id:}")
    private String appId;

    @Value("${adzuna.app-key:}")
    private String appKey;

    @Value("${adzuna.results-per-page:20}")
    private int resultsPerPage;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getSourceName() {
        return "Adzuna";
    }

    @Override
    public List<JobPosting> fetchJobs(String keyword) {

        List<JobPosting> jobs = new ArrayList<>();

        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank()) {
            log.debug("Adzuna app-id/app-key not configured - skipping Adzuna source");
            return jobs;
        }

        try {
            String what = URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8);

            String url = API_URL
                    + "?app_id=" + appId
                    + "&app_key=" + appKey
                    + "&results_per_page=" + resultsPerPage
                    + "&what=" + what
                    + "&content-type=application/json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Adzuna API returned status {}", response.statusCode());
                return jobs;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");

            if (!results.isArray()) {
                return jobs;
            }

            for (JsonNode node : results) {

                jobs.add(JobPosting.builder()
                        .source(getSourceName())
                        .externalId(node.path("id").asText())
                        .title(node.path("title").asText(""))
                        .company(node.path("company").path("display_name").asText(""))
                        .location(node.path("location").path("display_name").asText(""))
                        .description(node.path("description").asText(""))
                        .url(node.path("redirect_url").asText(""))
                        .tags("")
                        .build());
            }

            log.info("Adzuna: fetched {} jobs for keyword '{}'", jobs.size(), keyword);

        } catch (Exception e) {
            log.error("Failed to fetch jobs from Adzuna", e);
        }

        return jobs;
    }
}
