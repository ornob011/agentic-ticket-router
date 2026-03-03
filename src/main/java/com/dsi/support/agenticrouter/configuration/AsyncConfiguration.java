package com.dsi.support.agenticrouter.configuration;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    @Bean(name = "ticketRoutingExecutor")
    public Executor ticketRoutingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ticket-routing-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setTaskDecorator(new MdcContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return ticketRoutingExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (
            Throwable throwable,
            Method method,
            Object... params
        ) -> log.error(
            "AsyncExecutionFail(method:{},paramCount:{})",
            method.getName(),
            params.length,
            throwable
        );
    }

    private static class MdcContextTaskDecorator implements TaskDecorator {

        @NonNull
        @Override
        public Runnable decorate(
            @NonNull Runnable runnable
        ) {
            Map<String, String> callerContext = MDC.getCopyOfContextMap();

            return () -> {
                Map<String, String> previousContext = MDC.getCopyOfContextMap();

                try {
                    if (Objects.isNull(callerContext)) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(callerContext);
                    }

                    runnable.run();
                } finally {
                    if (Objects.isNull(previousContext)) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previousContext);
                    }
                }
            };
        }
    }
}
