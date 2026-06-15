package com.jobEmailAgent.runner;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jobEmailAgent.service.GmailReaderService;
import com.jobEmailAgent.service.JobAggregatorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs the full job-hunt pipeline on a fixed schedule:
 *  1. Check Gmail for new job alerts / recruiter outreach
 *  2. Search configured job board APIs for new postings
 * Both steps score matches against the resume and create Gmail drafts for
 * strong matches, which the user reviews and sends manually.
 *
 * Spring's @Scheduled with fixedRate (and no initialDelay) runs once
 * immediately on startup, then every `fixedRate` milliseconds after that.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailPollingScheduler {

    private static final long ONE_HOUR_MS = 60L * 60L * 1000L;

    private final GmailReaderService gmailReaderService;
    private final JobAggregatorService jobAggregatorService;

    @Scheduled(fixedRate = ONE_HOUR_MS)
    public void runPipeline() {

        log.info("=== Job Email Agent: starting run ===");

        try {
            log.info("Checking Gmail for job-related emails...");
            gmailReaderService.readLatestEmails();
        } catch (Exception e) {
            log.error("Email check failed", e);
        }

        try {
            log.info("Searching job boards for new postings...");
            jobAggregatorService.runWebSearch();
        } catch (Exception e) {
            log.error("Job board search failed", e);
        }

        log.info("=== Job Email Agent: run complete ===");
    }
}
