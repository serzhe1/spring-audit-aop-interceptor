package org.auditaspect.core;

public interface BaseAuditService {

    void  before();

    void  afterReturning();

    void  afterThrowing();
}
