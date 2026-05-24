package com.jobEmailAgent.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobEmail {

    private String from;

    private String subject;

    private String emailType;

    private boolean recruiterEmail;
}