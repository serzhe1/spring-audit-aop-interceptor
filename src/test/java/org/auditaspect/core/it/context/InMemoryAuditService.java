package org.auditaspect.core.it.context;

import lombok.Getter;
import org.aspectj.lang.JoinPoint;
import org.auditaspect.core.BaseAuditService;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component("inMemoryAudit")
public class InMemoryAuditService implements BaseAuditService {

    @Getter
    private final ConcurrentLinkedQueue<String> events = new ConcurrentLinkedQueue<>();

    @Override public void before(JoinPoint jp) {
        events.add("BEFORE:" + jp.getSignature().toShortString());
    }
    @Override public void afterReturning(JoinPoint jp, Object ret) {
        events.add("AFTER_RETURNING:" + jp.getSignature().toShortString());
    }
    @Override public void afterThrowing(JoinPoint jp, Throwable ex) {
        events.add("AFTER_THROWING:" + jp.getSignature().toShortString());
    }
}