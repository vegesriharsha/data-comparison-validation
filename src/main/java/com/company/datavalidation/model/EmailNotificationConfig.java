package com.company.datavalidation.model;

import jakarta.persistence.*;

import javax.print.attribute.standard.Severity;

@Entity
@Table(name = "email_notification_config")
public class EmailNotificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Column(name = "severity_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severityLevel;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Severity getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(Severity severityLevel) {
        this.severityLevel = severityLevel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
