package com.jobEmailAgent.service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.jobEmailAgent.dao.ProcessedEmailEntity;
import com.jobEmailAgent.enums.EmailType;
import com.jobEmailAgent.enums.MatchLevel;
import com.jobEmailAgent.model.JobEmail;
import com.jobEmailAgent.repository.ProcessedEmailRepository;
import com.jobEmailAgent.util.SkillExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailReaderService {

	private final GmailAuthService gmailAuthService;
	private final SkillExtractor skillExtractor;
	private final EmailClassifierService emailClassifierService;
	private final MatchScoringService matchScoringService;
	private final ProcessedEmailRepository processedEmailRepository;
	private final ResumeService resumeService;

	public void readLatestEmails() {
		try {
			Gmail gmail = gmailAuthService.getGmailService();

			ListMessagesResponse response = gmail.users().messages().list("me").setQ(
					"(from:linkedin OR subject:Java OR subject:Developer OR subject:Engineer OR subject:Recruiter) newer_than:7d")
					.setMaxResults(80L).execute();
			
			String resumeText = resumeService.loadResumeText();

			log.info("Resume loaded successfully. Length={}", resumeText.length());

			List<Message> messages = response.getMessages();
			

			if (messages == null || messages.isEmpty()) {
				log.info("No emails found.");
				return;
			}

			for (Message message : messages) {
				
				String gmailMessageId = message.getId();
				
				if(processedEmailRepository.existsByGmailMessageId(gmailMessageId)) {
					 log.info("Skipping already processed email: {}", gmailMessageId);
					 continue;
				}
				
				Message fullMessage = gmail
						.users()
						.messages()
						.get("me", message.getId())
						.setFormat("full")
						.execute();

				String subject = getHeader(fullMessage, "Subject");
				String from = getHeader(fullMessage, "From");
				String body = extractEmailBody(fullMessage);
				
				String cleanBody = body
				        .replaceAll("(?s)<script.*?</script>", " ")
				        .replaceAll("(?s)<style.*?</style>", " ")
				        .replaceAll("<[^>]+>", " ")
				        .replaceAll("&nbsp;", " ")
				        .replaceAll("\\s+", " ")
				        .trim();

				List<String> skills = skillExtractor.extractSkills(cleanBody);
				
				EmailType emailType =
				        emailClassifierService.classify(from, subject, cleanBody);
				
				int matchingScore =
				        matchScoringService.calculateScore(skills, emailType, cleanBody);

				JobEmail jobEmail = JobEmail.builder()
				        .from(from)
				        .subject(subject)
				        .emailType(emailType)
						.matchLevel(matchScoringService.getMatchLevel(matchingScore))
						.matchingScore(matchingScore)
				        .body(cleanBody)
				        .build();

				log.info("Job Email: from={}, subject={}, emailType={}, matchLevel={}, matchingScore={}",
				        jobEmail.getFrom(),
				        jobEmail.getSubject(),
				        jobEmail.getEmailType(),
				        jobEmail.getMatchLevel(),
				        jobEmail.getMatchingScore());
				
				log.info("Body Preview: {}", cleanBody.substring(0, Math.min(cleanBody.length(), 300)));
				log.info("Matching Skill {} ", skills );
				
				
				if (emailType == EmailType.RECRUITER_OUTREACH || emailType == EmailType.JOB_ALERT) {

				    ProcessedEmailEntity entity = new ProcessedEmailEntity();
				    entity.setGmailMessageId(gmailMessageId);
				    entity.setFromEmail(from);
				    entity.setSubject(subject);
				    entity.setEmailType(emailType);
				    entity.setMatchLevel(jobEmail.getMatchLevel());
				    entity.setMatchingScore(matchingScore);
				    entity.setProcessedAt(LocalDateTime.now());

				    processedEmailRepository.save(entity);

				    log.info("Saved email to DB: gmailMessageId={}, emailType={}, matchLevel={}, score={}",
				            gmailMessageId,
				            emailType,
				            jobEmail.getMatchLevel(),
				            matchingScore);
				} else {
				    log.info("Not saving emailType={} to DB", emailType);
				}
			}
			

		} catch (Exception e) {
			log.error("Failed to read Gmail emails", e);
		}
	}

	//Get header
	private String getHeader(Message message, String headerName) {
		return message.getPayload().getHeaders().stream()
				.filter(header -> header.getName().equalsIgnoreCase(headerName)).map(header -> header.getValue())
				.findFirst().orElse("N/A");
	}

	// ExtractBody
	private String extractEmailBody(Message message) {

	    try {
	        return extractParts(message.getPayload());

	    } catch (Exception e) {
	        log.error("Failed to extract email body", e);
	    }

	    return "No Body Found";
	}

	
	
	private String extractParts(MessagePart part) throws Exception {

	    if (part == null) {
	        return "";
	    }

	    // Plain text preferred
	    if ("text/plain".equalsIgnoreCase(part.getMimeType())
	            && part.getBody() != null
	            && part.getBody().getData() != null) {

	        byte[] decodedBytes = Base64.getUrlDecoder()
	                .decode(part.getBody().getData());

	        return new String(decodedBytes);
	    }

	    // HTML fallback
	    if ("text/html".equalsIgnoreCase(part.getMimeType())
	            && part.getBody() != null
	            && part.getBody().getData() != null) {

	        byte[] decodedBytes = Base64.getUrlDecoder()
	                .decode(part.getBody().getData());

	        return new String(decodedBytes);
	    }

	    // Recursive traversal
	    if (part.getParts() != null) {

	        for (MessagePart childPart : part.getParts()) {

	            String result = extractParts(childPart);

	            if (result != null && !result.isBlank()) {
	                return result;
	            }
	        }
	    }

	    return "";
	}
}