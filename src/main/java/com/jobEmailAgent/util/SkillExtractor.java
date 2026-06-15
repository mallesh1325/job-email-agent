package com.jobEmailAgent.util;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SkillExtractor {

	// Broad skill/keyword list aligned with common Java/Backend job postings.
	// Multi-word entries are matched as exact phrases.
	private static final List<String> SKILLS = List.of(
			// Core language & frameworks
			"java", "spring boot", "spring", "spring webflux", "spring security",
			"spring data jpa", "spring batch", "spring mvc", "hibernate", "jpa",

			// Messaging & integration
			"kafka", "apache camel", "ibm mq", "rabbitmq", "jms",

			// Cloud & devops
			"aws", "ecs", "fargate", "lambda", "ec2", "s3", "rds", "dynamodb",
			"cloudwatch", "sns", "sqs", "api gateway", "azure", "azure devops",
			"gcp", "docker", "kubernetes", "terraform", "jenkins", "gitlab ci/cd",
			"ci/cd", "gradle", "maven",

			// Databases
			"postgresql", "mysql", "oracle", "mongodb", "cassandra", "cosmos db",
			"redis", "sql",

			// Architecture & APIs
			"microservices", "rest", "restful", "graphql", "grpc", "reactive",
			"event-driven", "oauth2", "jwt",

			// Testing & tooling
			"junit", "mockito", "sonarqube", "postman", "swagger", "git", "jira",

			// Frontend (occasionally relevant for full-stack roles)
			"react", "angular", "typescript", "javascript"
	);

	/**
	 * Returns the subset of {@link #SKILLS} found in the given text (case-insensitive,
	 * whole-word/phrase matches only).
	 */
	public List<String> extractSkills(String text) {

		String lowerText = text == null ? "" : text.toLowerCase();

		return SKILLS.stream()
				.filter(skill -> lowerText.matches(
						"(?s).*\\b" + java.util.regex.Pattern.quote(skill) + "\\b.*"))
				.toList();
	}
}
