package com.jobEmailAgent.runner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.jobEmailAgent.gmail.GmailReaderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JobEmailAgentRunner implements CommandLineRunner {

	private final GmailReaderService gmailReaderService;
	
    public JobEmailAgentRunner(GmailReaderService gmailReaderService) {
        this.gmailReaderService = gmailReaderService;
    }
	
	@Override
	public void run(String... args) throws Exception {
		log.info("Job Email Agent started.");
		gmailReaderService.readLatestEmails();
		
		
	}

}
