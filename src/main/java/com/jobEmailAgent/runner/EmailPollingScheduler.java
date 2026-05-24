package com.jobEmailAgent.runner;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EmailPollingScheduler {

    @Scheduled(fixedRate = 60000)
    public void checkEmails() {

        log.info("Checking Gmail for job emails...");
    }
}