package com.example.ordermgmt.config;

import com.example.ordermgmt.security.TenantContextHolder;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ordermgmt-async-");
        executor.setTaskDecorator(contextCopyingTaskDecorator());
        executor.initialize();
        return executor;
    }

    private TaskDecorator contextCopyingTaskDecorator() {
        return runnable -> {
            UUID tenantId = TenantContextHolder.getTenantId();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            return () -> {
                UUID previousTenantId = TenantContextHolder.getTenantId();
                Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();

                try {
                    if (tenantId != null) {
                        TenantContextHolder.setTenantId(tenantId);
                    } else {
                        TenantContextHolder.clear();
                    }

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);

                    runnable.run();
                } finally {
                    if (previousTenantId != null) {
                        TenantContextHolder.setTenantId(previousTenantId);
                    } else {
                        TenantContextHolder.clear();
                    }

                    SecurityContext previousContext = SecurityContextHolder.createEmptyContext();
                    previousContext.setAuthentication(previousAuthentication);
                    SecurityContextHolder.setContext(previousContext);
                }
            };
        };
    }
}
