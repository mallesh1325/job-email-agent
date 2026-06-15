package com.jobEmailAgent.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates Gmail drafts so the candidate can review and hit "Send" themselves.
 * This service never sends email directly.
 *
 * Requires the Gmail OAuth token to include the gmail.compose scope - see
 * {@code GmailAuthService}.
 */
@Service
@Slf4j
public class GmailDraftService {

	/**
	 * Creates a draft email in the authenticated user's Gmail account.
	 *
	 * @param gmail   authenticated Gmail service
	 * @param to      recipient email address, or null/blank to leave the "To" field empty
	 *                (e.g. when applying via a job board link rather than a direct contact)
	 * @param subject email subject
	 * @param body    plain-text email body
	 */
	public void createDraft(Gmail gmail, String to, String subject, String body) {
		try {
			String rawMessage = buildRawMessage(to, subject, body);

			String encoded = Base64.getUrlEncoder().encodeToString(rawMessage.getBytes(StandardCharsets.UTF_8));

			Message message = new Message();
			message.setRaw(encoded);

			Draft draft = new Draft();
			draft.setMessage(message);

			gmail.users().drafts().create("me", draft).execute();

			log.info("Created Gmail draft: to={}, subject={}", to == null ? "(none)" : to, subject);

		} catch (Exception e) {
			log.error("Failed to create Gmail draft for subject '{}'", subject, e);
		}
	}

	/** Builds a minimal RFC 2822 message. Subject/body are expected to be plain ASCII/UTF-8 text. */
	private String buildRawMessage(String to, String subject, String body) {
		StringBuilder sb = new StringBuilder();

		if (to != null && !to.isBlank()) {
			sb.append("To: ").append(to).append("\r\n");
		}

		sb.append("Subject: ").append(subject == null ? "" : subject).append("\r\n");
		sb.append("Content-Type: text/plain; charset=\"UTF-8\"\r\n");
		sb.append("Content-Transfer-Encoding: 8bit\r\n");
		sb.append("\r\n");
		sb.append(body == null ? "" : body);

		return sb.toString();
	}
}
