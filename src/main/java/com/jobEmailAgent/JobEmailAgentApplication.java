package com.jobEmailAgent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobEmailAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobEmailAgentApplication.class, args);
	}

}
