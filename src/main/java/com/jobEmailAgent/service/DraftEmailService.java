package com.jobEmailAgent.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Generates short, personalized email drafts (applying to / responding to a job)
 * using a local Llama model served by Ollama (http://localhost:11434). The
 * candidate always reviews and sends manually - this service only produces
 * draft text.
 *
 * Requires Ollama to be installed and running locally, with the configured
 * model pulled (e.g. "ollama pull llama3.2:3b"). See https://ollama.com/download
 */
@Service
@Slf4j
public class DraftEmailService {

	@Value("${ollama.base-url:http://localhost:11434}")
	private String baseUrl;

	@Value("${ollama.model:llama3.2:3b}")
	private String model;

	@Value("${ollama.max-tokens:600}")
	private int maxTokens;

	/**
	 * Generous timeout: local CPU inference on a laptop can take well over a
	 * minute for a single response, especially on the first call after the
	 * model is loaded into memory.
	 */
	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private final ObjectMapper objectMapper = new ObjectMapper();

	public record EmailDraft(String subject, String body) {
	}

	/**
	 * Generates a draft email tailored to the given job, using the candidate's resume
	 * for relevant skills/experience. Returns null if Ollama isn't reachable or the
	 * request fails (callers should treat this as "skip, try again later").
	 */
	public EmailDraft generateDraft(String resumeText, String jobTitle, String company, String jobContext) {

		String prompt = buildPrompt(resumeText, jobTitle, company, jobContext);

		try {
			ObjectNode requestBody = objectMapper.createObjectNode();
			requestBody.put("model", model);
			requestBody.put("prompt", prompt);
			requestBody.put("stream", false);

			ObjectNode options = objectMapper.createObjectNode();
			options.put("num_predict", maxTokens);
			requestBody.set("options", options);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/api/generate"))
					.header("Content-Type", "application/json")
					.timeout(Duration.ofMinutes(5))
					.POST(BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				log.error("Ollama API error: status={}, body={}", response.statusCode(), response.body());
				return null;
			}

			JsonNode root = objectMapper.readTree(response.body());
			String text = root.path("response").asText("");

			if (text.isBlank()) {
				log.warn("Ollama returned an empty response for job '{}'", jobTitle);
				return null;
			}

			return parseDraft(text, jobTitle, company);

		} catch (java.net.ConnectException ce) {
			log.error("Could not connect to Ollama at {} - is it running? (ollama serve)", baseUrl);
			return null;
		} catch (Exception e) {
			log.error("Failed to generate email draft via Ollama for job '{}'", jobTitle, e);
			return null;
		}
	}

	private String buildPrompt(String resumeText, String jobTitle, String company, String jobContext) {
		return """
				You are helping a Java/Backend engineer write a short, professional outreach email \
				related to a job opportunity. Use only information present in the resume below - \
				do not invent skills, employers, or experience.

				=== CANDIDATE RESUME ===
				%s

				=== JOB OPPORTUNITY ===
				Title: %s
				Company: %s
				Details:
				%s

				Write a concise (under 180 words) professional email expressing interest in this \
				opportunity, highlighting 2-3 relevant skills or experiences from the resume that \
				best match this role. End with the candidate's name as shown on the resume.

				Respond ONLY in this exact format, with nothing else before or after:
				SUBJECT: <subject line>
				BODY:
				<email body>
				"""
				.formatted(
						resumeText,
						jobTitle == null ? "N/A" : jobTitle,
						company == null || company.isBlank() ? "N/A" : company,
						jobContext == null ? "" : jobContext);
	}

	private EmailDraft parseDraft(String text, String jobTitle, String company) {

		String defaultSubject = "Regarding: " + (jobTitle == null ? "Job Opportunity" : jobTitle)
				+ (company != null && !company.isBlank() ? " at " + company : "");

		String subject = defaultSubject;
		String body = text.trim();

		int subjectIdx = text.indexOf("SUBJECT:");
		int bodyIdx = text.indexOf("BODY:");

		if (subjectIdx >= 0 && bodyIdx > subjectIdx) {
			subject = text.substring(subjectIdx + "SUBJECT:".length(), bodyIdx).trim();
			body = text.substring(bodyIdx + "BODY:".length()).trim();
		} else if (bodyIdx >= 0) {
			body = text.substring(bodyIdx + "BODY:".length()).trim();
		}
		

		if (subject.isBlank()) {
		    subject = defaultSubject;
		}

		// Safety net: some models repeat the "SUBJECT: ..." line again at the start
		// of the body content. Strip a leading SUBJECT: line if present.
		body = body.replaceFirst("(?i)^\\s*SUBJECT:.*?(\\r?\\n|$)", "").trim();

		return new EmailDraft(subject, body);
	}
}