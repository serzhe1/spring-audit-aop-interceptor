package org.auditaspect.core.it.context;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    long countByPhaseAndTarget(String phase, String target);
}