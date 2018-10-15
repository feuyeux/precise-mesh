package org.feuyeux.mesh.engine;

import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.feuyeux.mesh.engine.resilient.ResilientCommand;
import org.feuyeux.mesh.engine.resilient.ResilientCommandCollapser;
import org.feuyeux.mesh.engine.resilient.ResilientObservableCommand;
import org.springframework.stereotype.Service;
import rx.Observable;

@Service
public class ResilientEngine {
    private HystrixRequestContext context;

    @PostConstruct
    public void init() {
        context = HystrixRequestContext.initializeContext();
    }

    @PreDestroy
    public void destroy() {
        context.shutdown();
    }

    public HystrixCollapser<List<String>, String, Integer> collapser(int i) {
        return new ResilientCommandCollapser(i);
    }

    public Observable<String> observe(String value) {
        return new ResilientObservableCommand(value).observe();
    }

    public Future<String> queue(String value) {
        return (new ResilientCommand(value)).queue();
    }

    public String execute(String value) {
        return new ResilientCommand(value).execute();
    }
}