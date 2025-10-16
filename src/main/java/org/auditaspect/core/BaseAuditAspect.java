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

@Aspect
@Slf4j
@Component
public class BaseAuditAspect {

    private final Map<String, ? extends BaseAuditService> serviceMap;

    @Autowired
    public BaseAuditAspect(Map<String, ? extends BaseAuditService> serviceMap) {
        this.serviceMap = serviceMap;
    }

    @Pointcut("@annotation(org.auditaspect.core.Auditable) || @within(org.auditaspect.core.Auditable)")
    void auditablePointcut() {}

    @Before("auditablePointcut()")
    void onBefore(JoinPoint jp) {
        String key = methodKey(jp);
        List<String> handlers = resolveHandlerNames(jp);

        if (isEmptyHandlers(handlers, key)) return;

        startLogOnDebug(Phase.BEFORE, key, handlers);

        long t0 = System.nanoTime();
        handlers.forEach(name -> safeInvoke(name, () -> serviceMap.get(name).before(), Phase.BEFORE, key));
        doneLogOnDebug(Phase.BEFORE, key, t0);
    }
    
    @AfterReturning(pointcut = "auditablePointcut()", returning = "ret")
    void onAfterReturning(JoinPoint jp, Object ret) {
        String key = methodKey(jp);
        List<String> handlers = resolveHandlerNames(jp);

        if (isEmptyHandlers(handlers, key)) return;

        startLogOnDebug(Phase.AFTER_RETURNING, key, handlers);

        long t0 = System.nanoTime();
        handlers.forEach(name -> safeInvoke(name, () -> serviceMap.get(name).afterReturning(), Phase.AFTER_RETURNING, key));
        doneLogOnDebug(Phase.AFTER_RETURNING, key, t0);
    }

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
        handlers.forEach(name -> safeInvoke(name, () -> serviceMap.get(name).afterThrowing(), Phase.AFTER_THROWING, key));
        if (log.isDebugEnabled()) {
            log.debug("audit phase={} done target={} totalDurationNs={} errClass={}",
                    Phase.AFTER_THROWING, key, System.nanoTime() - t0, ex.getClass().getSimpleName());
        }
        }

    //Сначала выполняется аннотации над методом, потом над классом
    private List<String> resolveHandlerNames(JoinPoint jp) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        Method method = sig.getMethod();
        Class<?> targetClass = jp.getTarget() != null ? jp.getTarget().getClass() : sig.getDeclaringType();
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

    private String methodKey(JoinPoint jp) {
        var sig = (MethodSignature) jp.getSignature();
        var method = sig.getMethod();
        var target = jp.getTarget() != null ? jp.getTarget().getClass() : sig.getDeclaringType();
        return target.getSimpleName() + "#" + method.getName();
    }


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
            // не эскалируем: аудит не должен ломать бизнес-логику
            log.warn("audit phase={} failed service={} target={} durationNs={} errClass={} errMsg={}",
                    phase, serviceName, methodKey, dur, e.getClass().getSimpleName(), e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("audit exception stack phase={} service={} target={}", phase, serviceName, methodKey, e);
            }
        }
    }

    private boolean isEmptyHandlers(List<String> handlers, String key) {
        if (handlers.isEmpty()) {
            log.debug("audit phase={} skipped: no handlers target={}", Phase.BEFORE, key);
            return true;
        }
        return false;
    }

    private void startLogOnDebug(Phase afterReturning, String key, List<String> handlers) {
        if (log.isDebugEnabled()) {
            log.debug("audit phase={} start target={} handlersCount={} handlers={}",
                    afterReturning, key, handlers.size(), handlers);
        }
    }

    private void doneLogOnDebug(Phase afterReturning, String key, long t0) {
        if (log.isDebugEnabled()) {
            log.debug("audit phase={} done target={} totalDurationNs={}",
                    afterReturning, key, System.nanoTime() - t0);
        }
    }
}
