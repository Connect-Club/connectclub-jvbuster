package com.connectclub.jvbuster.utils;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
@Order(value = 0)
public class Aspects {

    private final RedissonClient redissonClient;

    public Aspects(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around(value = "@annotation(methodArgumentsLogger)", argNames = "pjp, methodArgumentsLogger")
    public Object methodArgumentsLogger(ProceedingJoinPoint pjp, MethodArgumentsLogger methodArgumentsLogger) throws Throwable {
        List<String> mdcKeys = new ArrayList<>();
        String methodName = "<not-set>";
        try {
            Object[] args = pjp.getArgs();
            String[] argNames = methodArgumentsLogger.argNames();
            List<String> mdcExcludeArgs = Arrays.asList(methodArgumentsLogger.mdcExcludeArgs());
            methodName = pjp.getSignature().getDeclaringType().getTypeName() + "." + pjp.getSignature().getName();
            StringBuilder stringBuilder = new StringBuilder("Calling method ")
                    .append(methodName)
                    .append("(");
            int argsLength = Math.min(args.length, argNames.length);
            for (int i = 0; i < argsLength; i++) {
                String argName = argNames[i];
                String argValue = String.valueOf(args[i]);
                stringBuilder.append(argName).append("=");
                if (argValue.contains(System.lineSeparator())) {
                    stringBuilder.append(System.lineSeparator())
                            .append(argValue);
                    if(!argValue.endsWith(System.lineSeparator())) {
                        stringBuilder.append(System.lineSeparator());
                    }
                } else {
                    stringBuilder.append(argValue);
                }
                if (i + 1 < argsLength) stringBuilder.append(", ");
                if (methodArgumentsLogger.putArgsToMDC() && !mdcExcludeArgs.contains(argName)) {
                    String mdcKey = "arg_" + argName;
                    MDC.put(mdcKey, argValue);
                    mdcKeys.add(mdcKey);
                }
            }
            stringBuilder.append(")");
            log.trace(stringBuilder.toString());
        } catch (Exception e) {
            log.error("MethodArgumentsLogger aspect error", e);
        }
        try {
            Object result = pjp.proceed();
            log.info("Calling method {} complete", methodName);
            return result;
        } catch(Exception e) {
            log.info("Calling method {} failed", methodName, e);
            throw e;
        } finally {
            mdcKeys.forEach(MDC::remove);
        }
    }

    @Around(value = "@annotation(methodSync)", argNames = "pjp, methodSync")
    public Object methodSync(ProceedingJoinPoint pjp, MethodSync methodSync) throws Throwable {
        StringBuilder lockNameBuilder = new StringBuilder(methodSync.lockName());
        Annotation[][] parameterAnnotations = ((MethodSignature) pjp.getSignature()).getMethod().getParameterAnnotations();
        for(int i=0;i<parameterAnnotations.length;i++) {
            for(Annotation annotation : parameterAnnotations[i]) {
                if(annotation instanceof MethodSyncArg) {
                    lockNameBuilder.append('-').append(pjp.getArgs()[i]);
                    break;
                }
            }
        }
        String lockName = lockNameBuilder.toString();
        RLock lock;
        switch (methodSync.mode()) {
            case REGULAR:
                lock = redissonClient.getLock(lockName);
                break;
            case FAIR:
                lock = redissonClient.getFairLock(lockName);
                break;
            case READ:
                lock = redissonClient.getReadWriteLock(lockName).readLock();
                break;
            case WRITE:
                lock = redissonClient.getReadWriteLock(lockName).writeLock();
                break;
            default:
                throw new RuntimeException("not implemented");
        }
        lock.lock();
        try {
            return pjp.proceed();
        } finally {
            lock.unlock();
        }
    }

}
