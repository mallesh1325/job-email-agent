package com.jobEmailAgent.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jobEmailAgent.enums.EmailType;
import com.jobEmailAgent.enums.MatchLevel;

@Service
public class MatchScoringService {

	public int calculateScore(List<String> skills, EmailType emailType, String body) {

		int score = 0;
		String lowerBody = body == null ? "" : body.toLowerCase();

		if (emailType == EmailType.SOCIAL || emailType == EmailType.UNKNOWN) {
			return 0;
		}

		if (emailType == EmailType.JOB_ALERT) {
			score += 10;
			score += Math.min(skills.size() * 5, 20);
			return Math.min(score, 40);
		}

		score += skills.size() * 10;

		if (emailType == EmailType.RECRUITER_OUTREACH) {
			score += 30;
		}

		if (lowerBody.contains("remote")) {
			score += 10;
		}

		if (lowerBody.contains("h1b") || lowerBody.contains("visa")) {
			score += 10;
		}

		return Math.min(score, 100);
	}

	public MatchLevel getMatchLevel(int score) {

		if (score >= 70) {
			return MatchLevel.HIGH;
		}

		if (score >= 40) {
			return MatchLevel.MEDIUM;
		}

		if (score >= 10) {
			return MatchLevel.LOW;
		}

		return MatchLevel.NONE;
	}
}