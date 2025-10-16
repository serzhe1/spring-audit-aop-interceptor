package org.auditaspect.core.it.context;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.auditaspect.core.BaseAuditService;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("dbAudit")
@RequiredArgsConstructor
public class DbAuditService implements BaseAuditService {

    private final AuditEventRepository repo;

    private String target(JoinPoint jp) {
        var cls = jp.getTarget() != null ? jp.getTarget().getClass().getSimpleName()
                                         : jp.getSignature().getDeclaringType().getSimpleName();
        return cls + "#" + jp.getSignature().getName();
    }

    @Override public void before(JoinPoint jp) {
        repo.save(AuditEvent.builder()
                .phase("BEFORE").target(target(jp)).ts(Instant.now()).build());
    }

    @Override public void afterReturning(JoinPoint jp, Object ret) {
        repo.save(AuditEvent.builder()
                .phase("AFTER_RETURNING").target(target(jp)).ts(Instant.now()).build());
    }

    @Override public void afterThrowing(JoinPoint jp, Throwable ex) {
        repo.save(AuditEvent.builder()
                .phase("AFTER_THROWING").target(target(jp)).ts(Instant.now())
                .errorClass(ex.getClass().getSimpleName())
                .errorMessage(ex.getMessage())
                .build());
    }
}