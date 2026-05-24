package com.jobEmailAgent.service;

import org.springframework.stereotype.Service;

import com.jobEmailAgent.enums.EmailType;

@Service
public class EmailClassifierService {

    public EmailType classify(String from, String subject, String body) {
        
        String lowerFrom = from == null ? "" : from.toLowerCase();
        String lowerSubject = subject == null ? "" : subject.toLowerCase();
        String lowerBody = body == null ? "" : body.toLowerCase();

        boolean linkedinEmail =
                lowerFrom.contains("linkedin");

        boolean jobAlertEmail =
                lowerFrom.contains("jobalerts")
                || lowerSubject.contains("job alert")
                || lowerBody.contains("job alert")
                || lowerFrom.contains("theladders")
                || lowerFrom.contains("postjobfree")
                || lowerFrom.contains("jobalert")
                || lowerBody.contains("jobs we recommend")
                || lowerFrom.contains("ladders");


        boolean recruiterEmail =
                !linkedinEmail
                && (
                    lowerSubject.contains("java")
                    || lowerSubject.contains("developer")
                    || lowerSubject.contains("recruiter")
                    || lowerBody.contains("role")
                    || lowerBody.contains("location")
                    || lowerBody.contains("duration")
                );

        if (jobAlertEmail) {
            return EmailType.JOB_ALERT;
        }

        if (linkedinEmail) {
            return EmailType.SOCIAL;
        }

        if (recruiterEmail) {
            return EmailType.RECRUITER_OUTREACH;
        }

        return EmailType.UNKNOWN;
    }
}