package com.jobEmailAgent.model;

import com.jobEmailAgent.enums.EmailType;
import com.jobEmailAgent.enums.MatchLevel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobEmail {

    private String from;

    private String subject;
    
    private String body;
    
    private EmailType emailType;
    
    private MatchLevel matchLevel;
    
    private int matchingScore;
    
    
}