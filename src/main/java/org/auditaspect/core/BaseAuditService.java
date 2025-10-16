package org.auditaspect.core;

import org.aspectj.lang.JoinPoint;

public interface BaseAuditService {

    void  before(JoinPoint jp);

    void  afterReturning(JoinPoint jp, Object ret);

    void  afterThrowing(JoinPoint jp, Throwable ex);
}
