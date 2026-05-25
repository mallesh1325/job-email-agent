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
        boolean noReplyEmail =
                lowerFrom.contains("noreply")
                || lowerFrom.contains("no-reply");

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
                && !noReplyEmail
                && (
                    lowerSubject.contains("java")
                    || lowerSubject.contains("developer")
                    || lowerSubject.contains("recruiter")
                    || lowerSubject.contains("engineer")
                    || lowerSubject.contains("fulltime")
                    || lowerSubject.contains("permanent")
                    || lowerSubject.contains("contract")
                    || lowerSubject.contains("hybrid")
                    || lowerSubject.contains("onsite")
                    || lowerBody.contains("role")
                    || lowerBody.contains("location")
                    || lowerBody.contains("duration")
                    || lowerBody.contains("job opportunity")
                    || lowerBody.contains("staffing")
                    || lowerBody.contains("client")
                    || lowerBody.contains("experience")
                );

        if (jobAlertEmail) {
            return EmailType.JOB_ALERT;
        }
        
        boolean linkedInRecruiterMessage =
                lowerFrom.contains("inmail")
                && (
                    lowerBody.contains("contract opportunity")
                    || lowerBody.contains("remote contract")
                    || lowerBody.contains("senior java developer role")
                    || lowerBody.contains("wanted to reach out")
                    || lowerBody.contains("came across your profile")
                );

        if (linkedInRecruiterMessage) {
            return EmailType.RECRUITER_OUTREACH;
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