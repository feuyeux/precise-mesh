package org.feuyeux.mesh.engine;

import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.feuyeux.mesh.engine.resilient.ResilientCommand;
import org.feuyeux.mesh.engine.resilient.ResilientCommandCollapser;
import org.feuyeux.mesh.engine.resilient.ResilientObservableCommand;
import org.springframework.stereotype.Service;
import rx.Observable;

@Service
public class ResilientEngine {
    private HystrixRequestContext context;
    private HystrixCommand.Setter config;

    private String key = "TODO";

    @PostConstruct
    public void init() {
        context = HystrixRequestContext.initializeContext();

         /*HystrixCommandGroupKey group;
    HystrixCommandKey key;
    HystrixThreadPoolKey threadPoolKey;
    HystrixCircuitBreaker circuitBreaker;
    HystrixThreadPool threadPool;
    HystrixCommandProperties.Setter commandPropertiesDefaults;
    HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults;
    HystrixCommandMetrics metrics;
    TryableSemaphore fallbackSemaphore;
    TryableSemaphore executionSemaphore;
    HystrixPropertiesStrategy propertiesStrategy;
    HystrixCommandExecutionHook executionHook;*/

        HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory.asKey(key);
        config = HystrixCommand
            .Setter
            .withGroupKey(groupKey);

        HystrixCommandProperties.Setter properties = HystrixCommandProperties.Setter();
        properties.withExecutionTimeoutInMilliseconds(1000);
        properties.withCircuitBreakerSleepWindowInMilliseconds(4000);
        properties.withExecutionIsolationStrategy
            (HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
        properties.withCircuitBreakerEnabled(true);
        properties.withCircuitBreakerRequestVolumeThreshold(1);

        config.andCommandPropertiesDefaults(properties);
        config.andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
            .withMaxQueueSize(1)
            .withCoreSize(1)
            .withQueueSizeRejectionThreshold(1));
    }

    @PreDestroy
    public void destroy() {
        context.shutdown();
    }

    public HystrixCollapser<List<String>, String, Integer> collapser(int i) {
        return new ResilientCommandCollapser(i);
    }

    public Observable<String> observe() {
        return new ResilientObservableCommand(key).observe();
    }

    public Future<String> queue() {
        return new ResilientCommand(config).queue();
    }

    public String execute() {
        return new ResilientCommand(config).execute();
    }
}