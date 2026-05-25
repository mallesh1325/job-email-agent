package com.jobEmailAgent.util;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SkillExtractor {

	private static final List<String> SKILLS = List.of("java", "spring boot", "kafka", "aws", "microservices", "react",
			"angular", "docker", "kubernetes");

	public List<String> extractSkills(String body) {

	    String lowerBody = body.toLowerCase();

	    return SKILLS.stream()
	            .filter(skill ->
	                    lowerBody.matches(
	                            "(?s).*\\b"
	                            + java.util.regex.Pattern.quote(skill)
	                            + "\\b.*"))
	            .toList();
	}
}