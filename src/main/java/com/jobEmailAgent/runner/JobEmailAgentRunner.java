package com.jobEmailAgent.runner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Logs a startup message. The actual pipeline (Gmail + job board search) is
 * triggered by {@link EmailPollingScheduler}, which runs once immediately on
 * startup and then on a fixed schedule - so it isn't duplicated here.
 */
@Component
@Slf4j
public class JobEmailAgentRunner implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		log.info("Job Email Agent started. Pipeline will run shortly (and then hourly).");
	}
}
