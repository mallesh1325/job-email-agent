package com.jobEmailAgent.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.services.gmail.Gmail;
import com.jobEmailAgent.dao.ProcessedJobEntity;
import com.jobEmailAgent.enums.MatchLevel;
import com.jobEmailAgent.model.JobPosting;
import com.jobEmailAgent.repository.ProcessedJobRepository;
import com.jobEmailAgent.service.source.JobSource;
import com.jobEmailAgent.util.SkillExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates web job-board search: fetches postings from every configured
 * {@link JobSource}, de-duplicates against previously processed jobs, scores
 * each posting against the candidate's resume, and creates a Gmail draft for
 * strong matches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobAggregatorService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");

	private final List<JobSource> jobSources;
	private final ProcessedJobRepository processedJobRepository;
	private final SkillExtractor skillExtractor;
	private final MatchScoringService matchScoringService;
	private final ResumeService resumeService;
	private final DraftEmailService draftEmailService;
	private final GmailAuthService gmailAuthService;
	private final GmailDraftService gmailDraftService;

	/** Comma-separated list of search terms, e.g. "Java Developer,Backend Engineer,Java" */
	@Value("#{'${job.search.keywords:Java Developer,Backend Engineer,Java}'.split(',')}")
	private List<String> keywords;

	/** Minimum match score (0-100) required to generate a draft email */
	@Value("${job.match.draft-threshold:60}")
	private int draftThreshold;

	public void runWebSearch() {

		String resumeText = resumeService.loadResumeText();
		List<String> resumeSkills = skillExtractor.extractSkills(resumeText);

		log.info("Resume skills detected: {}", resumeSkills);

		for (JobSource source : jobSources) {
			for (String rawKeyword : keywords) {

				String keyword = rawKeyword.trim();
				if (keyword.isEmpty()) {
					continue;
				}

				List<JobPosting> jobs = source.fetchJobs(keyword);

				for (JobPosting job : jobs) {
					processJob(job, resumeText, resumeSkills);
				}
			}
		}
	}

	private void processJob(JobPosting job, String resumeText, List<String> resumeSkills) {

		if (job.getExternalId() == null || job.getExternalId().isBlank()) {
			return;
		}

		if (processedJobRepository.existsBySourceAndExternalId(job.getSource(), job.getExternalId())) {
			return;
		}

		List<String> jobSkills = skillExtractor.extractSkills(job.getTitle() + " " + job.getDescription());

		int score = matchScoringService.calculateJobMatchScore(jobSkills, resumeSkills, job.getDescription());
		MatchLevel matchLevel = matchScoringService.getMatchLevel(score);

		log.info("Job [{}] '{}' @ {} -> score={}, level={}, url={}",
				job.getSource(), job.getTitle(), job.getCompany(), score, matchLevel, job.getUrl());

		boolean draftCreated = false;

		if (score >= draftThreshold) {
			draftCreated = tryCreateDraft(resumeText, job);
		}

		ProcessedJobEntity entity = new ProcessedJobEntity();
		entity.setSource(job.getSource());
		entity.setExternalId(job.getExternalId());
		entity.setTitle(job.getTitle());
		entity.setCompany(job.getCompany());
		entity.setUrl(job.getUrl());
		entity.setMatchLevel(matchLevel);
		entity.setMatchingScore(score);
		entity.setDraftCreated(draftCreated);
		entity.setProcessedAt(LocalDateTime.now());

		processedJobRepository.save(entity);
	}

	private boolean tryCreateDraft(String resumeText, JobPosting job) {
		try {
			String jobContext = job.getDescription() + "\n\nSource: " + job.getSource()
					+ "\nLink to apply: " + job.getUrl();

			DraftEmailService.EmailDraft draft = draftEmailService.generateDraft(
					resumeText, job.getTitle(), job.getCompany(), jobContext);

			if (draft == null) {
				return false;
			}

			Gmail gmail = gmailAuthService.getGmailService();

			// Some job postings (especially smaller/staffing companies) include a direct
			// contact email in the description. If one is present, address the draft to it.
			String contactEmail = extractEmailAddress(job.getDescription());

			String body;
			if (contactEmail != null) {
				body = draft.body() + "\n\n---\nJob link: " + job.getUrl();
			} else {
				// No direct recipient for job-board postings - draft is left with empty "To"
				// for the user to fill in (or attach to their application portal submission).
				body = "NOTE: This is a draft cover letter. No direct application email was found "
						+ "for this posting - copy this text when applying via the link below.\n\n"
						+ draft.body() + "\n\n---\nJob link: " + job.getUrl();
			}

			gmailDraftService.createDraft(gmail, contactEmail, draft.subject(), body);

			return true;

		} catch (Exception e) {
			log.error("Failed to create draft for job '{}' @ {}", job.getTitle(), job.getCompany(), e);
			return false;
		}
	}

	/** Extracts the first email address found in the given text, or null if none. */
	private String extractEmailAddress(String text) {
		if (text == null) {
			return null;
		}
		Matcher matcher = EMAIL_PATTERN.matcher(text);
		return matcher.find() ? matcher.group() : null;
	}
}