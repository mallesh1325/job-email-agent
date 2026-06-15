package com.jobEmailAgent.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single job posting pulled from an external job board API.
 */
@Data
@Builder
public class JobPosting {

    /** Name of the source, e.g. "RemoteOK", "Arbeitnow" */
    private String source;

    /** Unique identifier for this posting within its source (used for de-duplication) */
    private String externalId;

    private String title;

    private String company;

    private String location;

    private String description;

    /** Link to the original posting */
    private String url;

    /** Raw tags/keywords associated with the posting, if any */
    private String tags;
}
