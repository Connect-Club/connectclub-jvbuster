package com.connectclub.jvbuster.utils;

import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Predicate;

public class MDCCopyHelper {
    private final Map<String, String> contextMap = MDC.getCopyOfContextMap();
    private final Thread contextThread = Thread.currentThread();

    public void set(Object... any) {
        if(Thread.currentThread() != contextThread && contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }

    public void clear(Object... any) {
        if(Thread.currentThread() != contextThread) {
            MDC.clear();
        }
    }

    public <T> Predicate<T> clearIfFalse(Predicate<T> predicate) {
        return predicate.or(x -> {
            clear();
            return false;
        });
    }
}
