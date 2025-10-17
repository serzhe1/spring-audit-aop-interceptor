package org.auditaspect.core;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a class or method as auditable, enabling interception by
 * {@link org.auditaspect.core.BaseAuditAspect} and delegation of audit events
 * to the specified handler beans.
 *
 * <p>This annotation allows developers to declaratively specify one or more
 * audit handler beans (by name) that should process audit events for a given
 * method or class. Each handler must implement the
 * {@link org.auditaspect.core.BaseAuditService} interface and be registered
 * as a Spring bean within the application context.</p>
 *
 * <p>Behavioral rules:</p>
 * <ul>
 *   <li>If applied at the <b>method level</b>, only that method is intercepted.</li>
 *   <li>If applied at the <b>class level</b>, all public methods of that class
 *       are intercepted unless they are individually annotated with another
 *       {@code @Auditable} (which takes precedence).</li>
 *   <li>If both class and method annotations are present and the method-level
 *       annotation defines non-empty {@code handlers}, it overrides the class-level
 *       configuration.</li>
 * </ul>
 *
 * <p>Handler names correspond to Spring bean names, allowing you to reference
 * multiple instances of the same class with different configurations:</p>
 *
 * <pre>{@code
 * @Component("loggerAudit")
 * public class LoggerAuditService implements BaseAuditService { ... }
 *
 * @Component("elkAudit")
 * public class ElkAuditService implements BaseAuditService { ... }
 *
 * @Service
 * public class UserService {
 *
 *     @Auditable(handlers = {"loggerAudit", "elkAudit"})
 *     public void createUser(UserDto dto) {
 *         ...
 *     }
 * }
 * }</pre>
 *
 * <p>When a method annotated with {@code @Auditable} is invoked, the aspect:
 * <ol>
 *   <li>Resolves the specified bean names to {@link BaseAuditService} instances;</li>
 *   <li>Invokes their {@code before(...)} methods prior to execution;</li>
 *   <li>Calls {@code afterReturning(...)} if the method completes successfully;</li>
 *   <li>Calls {@code afterThrowing(...)} if the method throws an exception.</li>
 * </ol>
 * <p>All handler invocations are fail-safe — exceptions inside handlers are
 * caught and logged but never propagated to business logic.</p>
 *
 * @see org.auditaspect.core.BaseAuditService
 * @see org.auditaspect.core.BaseAuditAspect
 *
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RUNTIME)
@Inherited
@Documented
public @interface Auditable {

    /**
     * The names of Spring beans implementing {@link BaseAuditService}
     * that should handle audit events for this method or class.
     * <p>
     * Each name must correspond to a bean available in the Spring
     * {@code ApplicationContext}. Duplicates are ignored, and the
     * handlers are executed in the declared order.
     *
     * @return an array of bean names for audit handler services
     */
    String[] handlers() default {};
}
