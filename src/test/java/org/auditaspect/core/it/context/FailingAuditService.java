package org.auditaspect.core.it.context;

import org.aspectj.lang.JoinPoint;
import org.auditaspect.core.BaseAuditService;
import org.springframework.stereotype.Component;

@Component("failingAudit")
public class FailingAuditService implements BaseAuditService {
    @Override public void before(JoinPoint jp)         { throw new IllegalStateException("before boom"); }
    @Override public void afterReturning(JoinPoint jp, Object ret) { throw new IllegalStateException("afterReturning boom"); }
    @Override public void afterThrowing(JoinPoint jp, Throwable ex){ throw new IllegalStateException("afterThrowing boom"); }
}