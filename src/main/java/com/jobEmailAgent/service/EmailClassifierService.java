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

        boolean linkedinJobNotification =
                lowerFrom.contains("jobs-noreply@linkedin.com")
                && (
                    lowerSubject.contains("apply now")
                    || lowerSubject.contains("your application was sent")
                    || lowerSubject.contains("your application was viewed")
                    || lowerSubject.contains("problem with your")
                    || lowerBody.contains("view job:")
                );

        // Automated "application received" confirmations from ATS platforms (Workday,
        // Greenhouse, Lever, iCIMS, etc.). These are auto-replies, not a recruiter asking
        // for a response, so they shouldn't generate a reply draft.
        boolean applicationConfirmationEmail =
                lowerSubject.contains("thank you for applying")
                || lowerSubject.contains("thanks for applying")
                || lowerSubject.contains("application received")
                || lowerSubject.contains("we received your application")
                || lowerSubject.contains("your application to")
                || lowerBody.contains("thank you for applying")
                || lowerBody.contains("thanks for taking the time to apply")
                || lowerBody.contains("we have received your application")
                || lowerBody.contains("reviewing your application")
                || lowerBody.contains("our recruiting team will review")
                || lowerFrom.contains("myworkday.com")
                || lowerFrom.contains("myworkdayjobs.com")
                || lowerFrom.contains("greenhouse.io")
                || lowerFrom.contains("lever.co")
                || lowerFrom.contains("icims.com")
                || lowerFrom.contains("smartrecruiters.com")
                || lowerFrom.contains("taleo.net")
                || lowerFrom.contains("successfactors")
                || lowerFrom.contains("workablemail.com")
                || lowerFrom.contains("jobvite.com")
                || lowerFrom.contains("ashbyhq.com");

        if (jobAlertEmail || linkedinJobNotification || applicationConfirmationEmail) {
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