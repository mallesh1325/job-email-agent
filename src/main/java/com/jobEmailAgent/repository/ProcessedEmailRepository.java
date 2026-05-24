package com.jobEmailAgent.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobEmailAgent.dao.ProcessedEmailEntity;

@Repository
public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmailEntity, Long> {

	boolean existsByGmailMessageId(String gmailMessageId);

	Optional<ProcessedEmailEntity> findByGmailMessageId(String gmailMessageId);
}