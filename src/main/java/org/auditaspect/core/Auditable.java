package org.auditaspect.core;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RUNTIME)
@Inherited
@Documented
public @interface Auditable {
    String[] handlers() default {};
}
