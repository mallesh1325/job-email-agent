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

		// Recruiter outreach base score
		if (emailType == EmailType.RECRUITER_OUTREACH) {
			score += 30;
		}

		// Core backend skills
		if (skills.contains("java"))
			score += 15;
		if (skills.contains("spring boot"))
			score += 15;
		if (skills.contains("kafka"))
			score += 10;
		if (skills.contains("aws"))
			score += 10;
		if (skills.contains("microservices"))
			score += 10;
		if (skills.contains("docker"))
			score += 5;
		if (skills.contains("kubernetes"))
			score += 5;

		// Work mode
		if (lowerBody.contains("remote"))
		    score += 10;

		if (lowerBody.contains("hybrid"))
		    score += 10;

		if (lowerBody.contains("onsite"))
		    score += 10;

		// Visa / work authorization
		if (lowerBody.contains("h1b") || lowerBody.contains("h-1b"))
			score += 15;
		if (lowerBody.contains("visa"))
			score += 10;
		if (lowerBody.contains("usc") || lowerBody.contains("green card only"))
			score -= 30;
		if (lowerBody.contains("no h1b") || lowerBody.contains("no visa"))
			score -= 40;

		// Employment type
		if (lowerBody.contains("no c2c")) {
			score -= 30;
		} else if (lowerBody.contains("c2c")) {
			score += 20;
		}

		if (lowerBody.contains("contract")) {
			score += 15;
		}

		if (lowerBody.contains("contract to hire") || lowerBody.contains("contract-to-hire")) {
			score += 10;
		}

		if (lowerBody.contains("w2")) {
			score += 5;
		}

		if (lowerBody.contains("full time") || lowerBody.contains("full-time")) {
			score += 5;
		}

		if (lowerBody.contains("usc only"))
			score -= 50;

		if (lowerBody.contains("gc only"))
			score -= 50;

		if (lowerBody.contains("citizen only"))
			score -= 50;

		if (lowerBody.contains("usc/gc"))
			score -= 40;

		if (lowerBody.contains("u.s. citizens or green card"))
			score -= 50;

		return Math.max(0, Math.min(score, 100));
	}

	// Matching Score
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