package org.feuyeux.mesh.engine.resilient;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;

/**
 * @author 六翁 lu.hl@alibaba-inc.com
 * @date 2018/10/15
 */
public class ResilientCommand extends HystrixCommand<String> {
    public ResilientCommand(HystrixCommand.Setter config) {
        super(config);
    }

    @Override
    protected String run() {
        return "Hello";
    }

    @Override
    protected String getFallback() {
        return "Hello Failure ";
    }

    /**
     * Request Cache
     *
     * @return
     */
    @Override
    protected String getCacheKey() {
        return String.valueOf("");
    }
}

