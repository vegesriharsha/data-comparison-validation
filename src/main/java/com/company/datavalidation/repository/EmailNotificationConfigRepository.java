package com.company.datavalidation.repository;

import com.company.datavalidation.model.EmailNotificationConfig;
import com.company.datavalidation.model.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailNotificationConfigRepository extends JpaRepository<EmailNotificationConfig, Long> {

    List<EmailNotificationConfig> findBySeverityLevelAndEnabled(Severity severityLevel, boolean enabled);

    List<EmailNotificationConfig> findByEnabled(boolean enabled);
}
