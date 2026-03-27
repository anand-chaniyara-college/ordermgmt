package com.example.ordermgmt.config;

import com.example.ordermgmt.security.TenantContextHolder;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
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
        executor.setCorePoolSize(300);
        executor.setMaxPoolSize(600);
        executor.setQueueCapacity(2400);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
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
