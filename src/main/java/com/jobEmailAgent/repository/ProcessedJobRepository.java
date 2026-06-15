package com.jobEmailAgent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobEmailAgent.dao.ProcessedJobEntity;

@Repository
public interface ProcessedJobRepository extends JpaRepository<ProcessedJobEntity, Long> {

    boolean existsBySourceAndExternalId(String source, String externalId);
}
