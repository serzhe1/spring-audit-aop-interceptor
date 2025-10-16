package org.auditaspect.core;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

/**
 * {@code BaseAuditAspect} is a Spring AOP aspect responsible for
 * intercepting methods and classes annotated with {@link Auditable}.
 * <p>
 * The aspect resolves the {@code handlers} declared in the {@link Auditable}
 * annotation and executes the corresponding {@link BaseAuditService} beans
 * from the Spring application context during three distinct phases:
 * <ul>
 *     <li>{@link Phase#BEFORE} – before the target method invocation</li>
 *     <li>{@link Phase#AFTER_RETURNING} – after successful completion</li>
 *     <li>{@link Phase#AFTER_THROWING} – after an exception is thrown</li>
 * </ul>
 * <p>
 * The aspect is <b>fail-safe</b>: exceptions thrown by audit handlers are caught
 * and logged but never propagated back to the business logic. This ensures
 * that auditing never breaks functional execution.
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * @Auditable(handlers = {"dbAudit", "loggerAudit"})
 * public void createUser(UserRequest request) {
 *     ...
 * }
 * }</pre>
 *
 * @author Alex
 * @since 0.1.0
 */
@Aspect
@Slf4j
@Component
public class BaseAuditAspect {

    private final Map<String, ? extends BaseAuditService> serviceMap;

    /**
     * Constructs a new {@code BaseAuditAspect}.
     *
     * @param serviceMap map of audit service beans available in the Spring context,
     *                   keyed by bean name. These beans are used to invoke
     *                   {@link BaseAuditService#before(JoinPoint)}},
     *                   {@link BaseAuditService#afterReturning(JoinPoint, Object)}, and
     *                   {@link BaseAuditService#afterReturning(JoinPoint, Object)}.
     */
    @Autowired
    public BaseAuditAspect(Map<String, ? extends BaseAuditService> serviceMap) {
        this.serviceMap = serviceMap;
    }

    /**
     * Defines a reusable pointcut that matches any method or class annotated with {@link Auditable}.
     */
    @Pointcut("@annotation(org.auditaspect.core.Auditable) || @within(org.auditaspect.core.Auditable)")
    void auditablePointcut() {}

    /**
     * Executes {@link BaseAuditService#before(JoinPoint)} on all resolved audit handlers
     * before the target method invocation.
     *
     * @param jp the current join point providing reflective access to the method and target class
     */
    @Before("auditablePointcut()")
    void onBefore(JoinPoint jp) {
        String key = methodKey(jp);
        List<String> handlers = resolveHandlerNames(jp);

        if (isEmptyHandlers(handlers, key)) return;

        startLogOnDebug(Phase.BEFORE, key, handlers);

        long t0 = System.nanoTime();
        handlers.forEach(name -> safeInvoke(name, () -> serviceMap.get(name).before(jp), Phase.BEFORE, key));
        doneLogOnDebug(Phase.BEFORE, key, t0);
    }

    /**
     * Executes {@link BaseAuditService#afterThrowing(JoinPoint, Throwable)} on all resolved audit handlers
     * after successful completion of the target method.
     *
     * @param jp  the current join point
     * @param ret the value returned by the intercepted method
     */
    @AfterReturning(pointcut = "auditablePointcut()", returning = "ret")
    void onAfterReturning(JoinPoint jp, Object ret) {
        String key = methodKey(jp);
        List<String> handlers = resolveHandlerNames(jp);

        if (isEmptyHandlers(handlers, key)) return;

        startLogOnDebug(Phase.AFTER_RETURNING, key, handlers);

        long t0 = System.nanoTime();
        handlers.forEach(name ->
                safeInvoke(name, () -> serviceMap.get(name).afterReturning(jp, ret), Phase.AFTER_RETURNING, key)
        );
        doneLogOnDebug(Phase.AFTER_RETURNING, key, t0);
    }

    /**
     * Executes {@link BaseAuditService#afterThrowing(JoinPoint, Throwable)} on all resolved audit handlers
     * when the target method throws an exception.
     *
     * @param jp the current join point
     * @param ex the exception thrown by the intercepted method
     */
    @AfterThrowing(pointcut = "auditablePointcut()", throwing = "ex")
    void onAfterThrowing(JoinPoint jp, Throwable ex) {
        String key = methodKey(jp);
        List<String> handlers = resolveHandlerNames(jp);

        if (isEmptyHandlers(handlers, key)) return;

        if (log.isDebugEnabled()) {
            log.debug("audit phase={} start target={} handlersCount={} handlers={} errClass={} errMsg={}",
                    Phase.AFTER_THROWING, key, handlers.size(), handlers, ex.getClass().getSimpleName(), ex.getMessage());
        }

        long t0 = System.nanoTime();
        handlers.forEach(name -> safeInvoke(name, () -> serviceMap.get(name).afterThrowing(jp, ex), Phase.AFTER_THROWING, key));
        if (log.isDebugEnabled()) {
            log.debug("audit phase={} done target={} totalDurationNs={} errClass={}",
                    Phase.AFTER_THROWING, key, System.nanoTime() - t0, ex.getClass().getSimpleName());
        }
    }

    /**
     * Resolves handler bean names from {@link Auditable} annotation.
     * <p>
     * Precedence rules:
     * <ul>
     *   <li>If the method is annotated with {@link Auditable} and {@code handlers} is non-empty,
     *       use those handler names.</li>
     *   <li>Otherwise, if the declaring class is annotated and its {@code handlers} is non-empty,
     *       use those.</li>
     *   <li>If neither provides any handler, returns an empty list.</li>
     * </ul>
     *
     * @param jp the current join point
     * @return a list of bean names of {@link BaseAuditService} to invoke
     */
    private List<String> resolveHandlerNames(JoinPoint jp) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        Method method = sig.getMethod();
        Class<?> targetClass = jp.getTarget() != null
                ? jp.getTarget().getClass()
                : sig.getDeclaringType();
        Method specific = AopUtils.getMostSpecificMethod(method, targetClass);

        Auditable mAnn = AnnotatedElementUtils.findMergedAnnotation(specific, Auditable.class);
        Auditable cAnn = AnnotatedElementUtils.findMergedAnnotation(targetClass, Auditable.class);

        List<String> names = new ArrayList<>();
        if (mAnn != null && mAnn.handlers().length > 0) {
            names.addAll(Arrays.asList(mAnn.handlers()));
        } else if (cAnn != null && cAnn.handlers().length > 0) {
            names.addAll(Arrays.asList(cAnn.handlers()));
        } else {
            return Collections.emptyList();
        }

        return names;
    }

    /**
     * Builds a concise textual key identifying the target method, formatted as
     * {@code ClassName#methodName}.
     *
     * @param jp the current join point
     * @return a readable identifier for the intercepted method
     */
    private String methodKey(JoinPoint jp) {
        var sig = (MethodSignature) jp.getSignature();
        var method = sig.getMethod();
        var target = jp.getTarget() != null ? jp.getTarget().getClass() : sig.getDeclaringType();
        return target.getSimpleName() + "#" + method.getName();
    }

    /**
     * Safely executes an audit handler action for the given phase, measuring execution time and logging results.
     * Any exception thrown by the handler is caught, logged, and ignored to ensure business code stability.
     *
     * @param serviceName the Spring bean name of the audit service
     * @param action      the handler method to invoke (e.g., {@code BaseAuditService::before})
     * @param phase       the current audit phase
     * @param methodKey   a readable identifier of the intercepted method
     */
    private void safeInvoke(String serviceName, Runnable action, Phase phase, String methodKey) {
        long t0 = System.nanoTime();
        try {
            action.run();
            if (log.isTraceEnabled()) {
                long dur = System.nanoTime() - t0;
                log.trace("audit phase={} ok service={} target={} durationNs={}",
                        phase, serviceName, methodKey, dur);
            }
        } catch (Exception e) {
            long dur = System.nanoTime() - t0;
            log.warn("audit phase={} failed service={} target={} durationNs={} errClass={} errMsg={}",
                    phase, serviceName, methodKey, dur,
                    e.getClass().getSimpleName(), e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("audit exception stack phase={} service={} target={}",
                        phase, serviceName, methodKey, e);
            }
        }
    }

    /**
     * Checks if the list of handler names is empty and logs a debug message if no handlers are resolved.
     *
     * @param handlers list of handler bean names
     * @param key      the intercepted method identifier
     * @return {@code true} if no handlers are defined, {@code false} otherwise
     */
    private boolean isEmptyHandlers(List<String> handlers, String key) {
        if (handlers.isEmpty()) {
            log.debug("audit phase={} skipped: no handlers target={}", Phase.BEFORE, key);
            return true;
        }
        return false;
    }

    /**
     * Logs a debug message marking the start of a specific audit phase.
     *
     * @param phase    the current audit phase
     * @param key      the intercepted method identifier
     * @param handlers list of handler bean names
     */
    private void startLogOnDebug(Phase phase, String key, List<String> handlers) {
        if (log.isDebugEnabled()) {
            log.debug("audit phase={} start target={} handlersCount={} handlers={}",
                    phase, key, handlers.size(), handlers);
        }
    }

    /**
     * Logs a debug message marking the end of a specific audit phase and the total duration of all handler calls.
     *
     * @param phase the current audit phase
     * @param key   the intercepted method identifier
     * @param t0    start time in nanoseconds
     */
    private void doneLogOnDebug(Phase phase, String key, long t0) {
        if (log.isDebugEnabled()) {
            log.debug("audit phase={} done target={} totalDurationNs={}",
                    phase, key, System.nanoTime() - t0);
        }
    }
}