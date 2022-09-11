package com.connectclub.jvbuster.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodSync {
    enum Mode {
        REGULAR, READ, WRITE, FAIR
    }

    String lockName();
    Mode mode() default Mode.REGULAR;
}
