package org.auditaspect.core.it;

import jakarta.annotation.Resource;
import org.auditaspect.core.it.context.AuditEventRepository;
import org.auditaspect.core.it.context.DemoService;
import org.auditaspect.core.it.context.InMemoryAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuditIntegrationTest {

    @Resource
    DemoService demo;
    @Resource
    AuditEventRepository repo;
    @Resource
    InMemoryAuditService inMemory;

    @Test
    void ok_flow_writes_before_and_afterReturning_to_db_and_memory() {
        String res = demo.ok("abc");

        assertThat(res).isEqualTo("ABC");

        assertThat(repo.countByPhaseAndTarget("BEFORE", "DemoService#ok")).isEqualTo(1);
        assertThat(repo.countByPhaseAndTarget("AFTER_RETURNING", "DemoService#ok")).isEqualTo(1);

        assertThat(inMemory.getEvents())
                .anyMatch(e -> e.startsWith("BEFORE:"))
                .anyMatch(e -> e.startsWith("AFTER_RETURNING:"));
    }

    @Test
    void method_level_annotation_overrides_class_level_handlers() {
        demo.onlyMemorySink();

        assertThat(repo.countByPhaseAndTarget("BEFORE", "DemoService#onlyMemorySink")).isZero();
        assertThat(repo.countByPhaseAndTarget("AFTER_RETURNING", "DemoService#onlyMemorySink")).isZero();

        assertThat(inMemory.getEvents())
                .anyMatch(s -> s.contains("onlyMemorySink") && s.startsWith("BEFORE:"))
                .anyMatch(s -> s.contains("onlyMemorySink") && s.startsWith("AFTER_RETURNING:"));
    }

    @Test
    void exception_flow_writes_afterThrowing_and_is_fail_safe_with_failing_handler() {
        assertThatThrownBy(() -> demo.boom())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expected");

        assertThat(repo.countByPhaseAndTarget("BEFORE", "DemoService#boom")).isEqualTo(1);
        assertThat(repo.countByPhaseAndTarget("AFTER_THROWING", "DemoService#boom")).isEqualTo(1);

        assertThat(inMemory.getEvents())
                .anyMatch(s -> s.startsWith("AFTER_THROWING:") && s.contains("boom"));
    }

    @SpringBootApplication
    @ComponentScan(basePackages = {
            "org.auditaspect.core",      // aspect, annotation, interface
            "org.auditaspect.core.it"    // demo service and testing audits services
    })
    static class TestApp {

    }
}