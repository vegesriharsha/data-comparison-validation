package com.company.datavalidation.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "email_notification_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
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
    @Builder.Default
    private boolean enabled = true;
}
