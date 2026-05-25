package com.jobEmailAgent.service;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class ResumeService {

    public String loadResumeText() {

        try {
            ClassPathResource resource = new ClassPathResource("resume.txt");

            return StreamUtils.copyToString(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to load resume.txt", e);
        }
    }
}