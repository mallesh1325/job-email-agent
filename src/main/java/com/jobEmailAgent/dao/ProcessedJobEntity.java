package com.jobEmailAgent.dao;

import java.time.LocalDateTime;

import com.jobEmailAgent.enums.MatchLevel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "processed_jobs")
@Data
public class ProcessedJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "RemoteOK", "Arbeitnow", "Adzuna" */
    private String source;

    /** ID of the posting within its source - used together with source for de-duplication */
    private String externalId;

    private String title;

    private String company;

    private String url;

    @Enumerated(EnumType.STRING)
    private MatchLevel matchLevel;

    private int matchingScore;

    /** Whether a Gmail draft was generated for this posting */
    private boolean draftCreated;

    private LocalDateTime processedAt;
}
