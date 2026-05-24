package com.jobEmailAgent.dao;

import java.time.LocalDateTime;

import com.jobEmailAgent.enums.EmailType;
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
@Table(name = "processed_emails")
@Data
public class ProcessedEmailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String gmailMessageId;

    private String fromEmail;

    private String subject;

    @Enumerated(EnumType.STRING)
    private EmailType emailType;

    @Enumerated(EnumType.STRING)
    private MatchLevel matchLevel;

    private int matchingScore;

    private LocalDateTime processedAt;
}