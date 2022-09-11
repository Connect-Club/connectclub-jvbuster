package com.connectclub.jvbuster;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import okhttp3.OkHttpClient;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30s")
@EnableAspectJAutoProxy
@EnableWebSecurity
public class JvbusterApplication {

    public static void main(String[] args) {
        SpringApplication.run(JvbusterApplication.class, args);
    }

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockProvider(redisConnectionFactory);
    }

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter() {
            private final ThreadLocal<Instant> endRequestThreshold = new ThreadLocal<>();

            @Override
            protected void beforeRequest(HttpServletRequest request, String message) {
                super.beforeRequest(request, message);
                endRequestThreshold.set(Instant.now().plusSeconds(5));
            }

            @Override
            protected void afterRequest(HttpServletRequest request, String message) {
                if(Instant.now().isAfter(endRequestThreshold.get())) {
                    logger.warn(message + " (It took too long!)");
                } else {
                    logger.debug(message);
                }
            }
        };
        filter.setIncludeQueryString(true);
        filter.setIncludeHeaders(true);
        filter.setHeaderPredicate(Predicate.not("authorization"::equals));
        return filter;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager, new TransactionDefinition() {
            @Override
            public int getPropagationBehavior() {
                return TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            }
        });
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return (tomcat) -> tomcat.addConnectorCustomizers((connector) -> {
            if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol) {
                AbstractHttp11Protocol<?> protocolHandler = (AbstractHttp11Protocol<?>) connector
                        .getProtocolHandler();
                protocolHandler.setKeepAliveTimeout(60000);
                protocolHandler.setMaxKeepAliveRequests(500);
                protocolHandler.setUseKeepAliveResponseHeader(true);
            }
        });
    }
}
