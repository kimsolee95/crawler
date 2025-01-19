package com.crawler.metadata.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolConfig {
	
    @Bean
    public ThreadPoolTaskExecutor crawlerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();    
        // CPU 코어 수를 기반으로 한 스레드 풀 설정
        int cores = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(cores * 2);  // I/O 작업이 많으므로 코어 수의 2배
        executor.setMaxPoolSize(cores * 4);   // 최대 코어 수의 4배
        // 작업 큐 크기 설정
        executor.setQueueCapacity(500);
        // 스레드 이름 설정
        executor.setThreadNamePrefix("crawler-");
        // 큐가 가득 찼을 때의 정책 설정
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
    
//    @Bean(name = "crawlerExecutor")
//    public ThreadPoolTaskExecutor crawlerExecutor() {
//        return createExecutor(10, 20, 500, "crawler-thread-");
//    }

//    @Bean(name = "metadataExecutor")
//    public ThreadPoolTaskExecutor metadataExecutor() {
//        return createExecutor(5, 10, 200, "metadata-thread-");
//    }

    private ThreadPoolTaskExecutor createExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
