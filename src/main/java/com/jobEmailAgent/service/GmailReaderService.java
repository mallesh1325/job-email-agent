package com.jobEmailAgent.service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
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

	private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");

	/**
	 * Matches generic/no-reply/mailer-platform sender addresses (e.g. "notify@oorwindigital.com",
	 * "jobs-noreply@linkedin.com"). When the "From" address matches this, we look in the email
	 * body for a more specific recruiter contact email instead.
	 */
	private static final Pattern GENERIC_SENDER_PATTERN = Pattern.compile(
			"(?i)^(notify|no-?reply|noreply|do-?not-?reply|alerts?|jobs?-?noreply|notifications?"
					+ "|invitations?|updates?-?noreply|messages?-?noreply|info|support|admin|jobseekers?)@");

	private final GmailAuthService gmailAuthService;
	private final SkillExtractor skillExtractor;
	private final EmailClassifierService emailClassifierService;
	private final MatchScoringService matchScoringService;
	private final ProcessedEmailRepository processedEmailRepository;
	private final ResumeService resumeService;
	private final DraftEmailService draftEmailService;
	private final GmailDraftService gmailDraftService;

	/** Minimum match score (0-100) required to generate a reply draft for recruiter emails */
	@Value("${email.match.draft-threshold:50}")
	private int draftThreshold;

	public void readLatestEmails() {
		try {
			Gmail gmail = gmailAuthService.getGmailService();

			// The authenticated user's own address - used to skip emails the user sent
			// themselves (e.g. "Re:" replies that show up in the same search query).
			String myEmailAddress = gmail.users().getProfile("me").execute().getEmailAddress();
			log.info("Authenticated Gmail account: {}", myEmailAddress);

			ListMessagesResponse response = gmail.users().messages().list("me").setQ(
					"(from:linkedin OR subject:Java OR subject:Developer OR subject:Engineer OR subject:Recruiter) newer_than:7d")
					.setMaxResults(80L).execute();
			
			String resumeText = resumeService.loadResumeText();
			List<String> resumeSkills = skillExtractor.extractSkills(resumeText);

			log.info("Resume loaded successfully. Length={}, skills={}", resumeText.length(), resumeSkills);

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
				
				Message fullMessage;
				try {
					fullMessage = gmail
							.users()
							.messages()
							.get("me", message.getId())
							.setFormat("full")
							.execute();
				} catch (Exception e) {
					log.warn("Skipping message {} - failed to fetch (it may have been moved/deleted): {}",
							gmailMessageId, e.getMessage());
					continue;
				}

				String subject = getHeader(fullMessage, "Subject");
				String from = getHeader(fullMessage, "From");

				// Skip emails sent by the user themselves (e.g. "Re:" replies to recruiters
				// that match the same search query and would otherwise get drafted as
				// replies-to-self).
				if (myEmailAddress != null && from != null
						&& from.toLowerCase().contains(myEmailAddress.toLowerCase())) {
					log.info("Skipping self-sent email: subject={}", subject);
					continue;
				}

				String body = extractEmailBody(fullMessage);
				
				String cleanBody = body
				        .replaceAll("(?s)<script.*?</script>", " ")
				        .replaceAll("(?s)<style.*?</style>", " ")
				        .replaceAll("<[^>]+>", " ")
				        .replaceAll("&nbsp;", " ")
				        .replaceAll("\\s+", " ")
				        .trim();

				// LinkedIn appends a footer like "This email was intended for <Name> (<your own
				// LinkedIn headline, e.g. Java Engineer @Walmart * Spring Boot * Kafka ...)" to
				// almost every email. Strip it so skill matching reflects the JOB content, not
				// the candidate's own profile headline (which would otherwise inflate every
				// email to roughly the same score).
				cleanBody = cleanBody.replaceAll("(?i)This email was intended for[^)]*\\)", " ").trim();

				List<String> skills = skillExtractor.extractSkills(cleanBody);
				
				
				EmailType emailType =
				        emailClassifierService.classify(from, subject, cleanBody);
				
				int baseScore =
				        matchScoringService.calculateScore(skills, emailType, cleanBody);

				// Bonus for overlap between skills mentioned in the email and the resume
				long overlap = skills.stream().filter(resumeSkills::contains).count();
				int matchingScore = Math.max(0, Math.min(baseScore + (int) (overlap * 5), 100));

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

				    // For recruiter outreach with a decent match, draft a reply for the user to review
				    if (emailType == EmailType.RECRUITER_OUTREACH && matchingScore >= draftThreshold) {
				        createReplyDraft(gmail, resumeText, jobEmail, cleanBody);
				    }
				} else {
				    log.info("Not saving emailType={} to DB", emailType);
				}
			}
			

		} catch (Exception e) {
			log.error("Failed to read Gmail emails", e);
		}
	}

	/** Generates and saves a reply draft for a recruiter email that's a strong resume match */
	private void createReplyDraft(Gmail gmail, String resumeText, JobEmail jobEmail, String cleanBody) {
		try {
			String senderEmail = pickReplyToEmail(jobEmail.getFrom(), cleanBody);

			String context = "This is a reply to a recruiter outreach email.\n"
					+ "From: " + jobEmail.getFrom() + "\n"
					+ "Subject: " + jobEmail.getSubject() + "\n\n"
					+ cleanBody;

			DraftEmailService.EmailDraft draft = draftEmailService.generateDraft(
					resumeText, jobEmail.getSubject(), null, context);

			if (draft == null) {
				return;
			}

			String replySubject = jobEmail.getSubject() != null && jobEmail.getSubject().toLowerCase().startsWith("re:")
					? jobEmail.getSubject()
					: "Re: " + jobEmail.getSubject();

			gmailDraftService.createDraft(gmail, senderEmail, replySubject, draft.body());

		} catch (Exception e) {
			log.error("Failed to create reply draft for email '{}'", jobEmail.getSubject(), e);
		}
	}

	/** Extracts the email address from a "From" header like "Jane Doe <jane@example.com>" */
	private String extractEmailAddress(String from) {
		if (from == null) {
			return null;
		}
		Matcher matcher = EMAIL_PATTERN.matcher(from);
		return matcher.find() ? matcher.group() : null;
	}

	/**
	 * Picks the best email address to send the reply draft to. If the "From" address is a
	 * generic notification/mailer address (e.g. "notify@oorwindigital.com"), search the email
	 * body for a more specific recruiter contact email (e.g. "please reach me at
	 * rajesh.y@softcomsystems.com") and use that instead.
	 */
	private String pickReplyToEmail(String from, String body) {
		String fromEmail = extractEmailAddress(from);

		if (fromEmail != null && !GENERIC_SENDER_PATTERN.matcher(fromEmail).find()) {
			return fromEmail;
		}

		if (body != null) {
			Matcher matcher = EMAIL_PATTERN.matcher(body);
			while (matcher.find()) {
				String candidate = matcher.group();
				if (!GENERIC_SENDER_PATTERN.matcher(candidate).find()
						&& (fromEmail == null || !candidate.equalsIgnoreCase(fromEmail))) {
					return candidate;
				}
			}
		}

		return fromEmail;
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