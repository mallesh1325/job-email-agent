package com.jobEmailAgent.service.source;

import java.util.List;

import com.jobEmailAgent.model.JobPosting;

/**
 * A source of job postings (a job board API). Implementations should only use
 * publicly documented, scraping-friendly APIs - never scrape sites whose
 * terms of service prohibit automated access (e.g. LinkedIn, Indeed).
 */
public interface JobSource {

    /** Human-readable name of this source, used for de-duplication and logging */
    String getSourceName();

    /**
     * Fetch job postings matching the given keyword (e.g. "Java Developer").
     * Implementations should fail soft - return an empty list and log on error,
     * never throw, so one broken source doesn't stop the whole pipeline.
     */
    List<JobPosting> fetchJobs(String keyword);
}
