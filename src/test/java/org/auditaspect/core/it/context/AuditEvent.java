package org.auditaspect.core.it.context;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)  private String phase;        // BEFORE / AFTER_RETURNING / AFTER_THROWING
    @Column(nullable = false)  private String target;       // Service#method
    @Column(nullable = false)  private Instant ts;          // timestamp
    @Column                    private String errorClass;   // only for AFTER_THROWING
    @Column(length = 1024)     private String errorMessage; // only for AFTER_THROWING
}