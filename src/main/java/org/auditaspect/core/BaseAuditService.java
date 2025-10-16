package org.auditaspect.core;

import org.aspectj.lang.JoinPoint;

/**
 * Defines the contract for audit handler implementations used by the
 * {@code BaseAuditAspect}.
 * <p>
 * Implementations of this interface encapsulate user-defined audit logic
 * that executes before and after a method invocation, or when an exception
 * occurs. Each audit handler is represented as a Spring bean and is invoked
 * by name via the {@link org.auditaspect.core.Auditable @Auditable} annotation.
 * <p>
 * Implementations must be <b>thread-safe</b> and <b>non-blocking</b> whenever possible,
 * since they may be executed synchronously within the target method’s call path.
 * Any exceptions thrown by these methods are caught and logged by the aspect;
 * they never propagate to business logic.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @Component("loggerAudit")
 * public class LoggerAuditService implements BaseAuditService {
 *
 *     @Override
 *     public void before(JoinPoint jp) {
 *         log.info("Before: {}", jp.getSignature());
 *     }
 *
 *     @Override
 *     public void afterReturning(JoinPoint jp, Object ret) {
 *         log.info("After success: {} returned {}", jp.getSignature(), ret);
 *     }
 *
 *     @Override
 *     public void afterThrowing(JoinPoint jp, Throwable ex) {
 *         log.warn("After failure: {} threw {}", jp.getSignature(), ex.toString());
 *     }
 * }
 * }</pre>
 *
 * @author serzhe1
 * @since 1.0.0
 */
public interface BaseAuditService {

    /**
     * Called immediately <b>before</b> the annotated method execution.
     * <p>
     * Typical use cases:
     * <ul>
     *   <li>Record audit start timestamp</li>
     *   <li>Log input parameters (with masking if required)</li>
     *   <li>Send pre-event metrics</li>
     * </ul>
     *
     * @param jp the current join point providing access to
     *           the target method, its arguments, and the owning object
     */
    void before(JoinPoint jp);

    /**
     * Called immediately <b>after</b> the annotated method successfully returns.
     * <p>
     * Typical use cases:
     * <ul>
     *   <li>Persist success audit record</li>
     *   <li>Log the outcome of a method call</li>
     *   <li>Emit success metrics to external systems</li>
     * </ul>
     *
     * @param jp  the current join point providing reflective access to the target method
     * @param ret the object returned by the intercepted method, may be {@code null}
     */
    void afterReturning(JoinPoint jp, Object ret);

    /**
     * Called when the annotated method throws an exception.
     * <p>
     * Typical use cases:
     * <ul>
     *   <li>Record failure audit entry</li>
     *   <li>Log error context to ELK / Splunk / database</li>
     *   <li>Send alert or notification</li>
     * </ul>
     *
     * @param jp the current join point
     * @param ex the exception thrown by the intercepted method (never {@code null})
     */
    void afterThrowing(JoinPoint jp, Throwable ex);
}