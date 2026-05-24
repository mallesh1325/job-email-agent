package com.jobEmailAgent.gmail;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.jobEmailAgent.model.JobEmail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailReaderService {

	private final GmailAuthService gmailAuthService;

	public void readLatestEmails() {
		try {
			Gmail gmail = gmailAuthService.getGmailService();

			ListMessagesResponse response = gmail.users().messages().list("me").setQ(
					"(from:linkedin OR subject:Java OR subject:Developer OR subject:Engineer OR subject:Recruiter) newer_than:7d")
					.setMaxResults(10L).execute();

			List<Message> messages = response.getMessages();

			if (messages == null || messages.isEmpty()) {
				log.info("No emails found.");
				return;
			}

			for (Message message : messages) {
				Message fullMessage = gmail.users().messages().get("me", message.getId()).setFormat("metadata")
						.execute();

				String subject = getHeader(fullMessage, "Subject");
				String from = getHeader(fullMessage, "From");
				boolean recruiterEmail = subject.toLowerCase().contains("java")
				        || subject.toLowerCase().contains("developer")
				        || subject.toLowerCase().contains("engineer")
				        || from.toLowerCase().contains("recruiter");
				
				JobEmail jobEmail = JobEmail.builder()
						.from(from)
						.subject(subject)
						.emailType("JOB")
						.recruiterEmail(recruiterEmail)
						.build();
				
				log.info("Job Email Object: {}", jobEmail);
				
				
			}

		} catch (Exception e) {
			log.error("Failed to read Gmail emails", e);
		}
	}

	private String getHeader(Message message, String headerName) {
		return message.getPayload().getHeaders().stream()
				.filter(header -> header.getName().equalsIgnoreCase(headerName)).map(header -> header.getValue())
				.findFirst().orElse("N/A");
	}
}