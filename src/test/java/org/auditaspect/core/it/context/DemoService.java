package org.auditaspect.core.it.context;

import org.auditaspect.core.Auditable;
import org.springframework.stereotype.Service;

@Auditable(handlers = {"dbAudit", "inMemoryAudit"})
@Service
public class DemoService {

    public String ok(String s) {
        return s.toUpperCase();
    }

    @Auditable(handlers = {"inMemoryAudit"})
    public void onlyMemorySink() {

    }

    @Auditable(handlers = {"dbAudit", "failingAudit", "inMemoryAudit"})
    public void boom() {
        throw new IllegalArgumentException("expected");
    }
}