package org.feuyeux.mesh.engine.resilient;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

/**
 * @author 六翁 lu.hl@alibaba-inc.com
 * @date 2018/10/15
 */
public class ResilientCommand extends HystrixCommand<String> {
    private final String value;
    private final boolean throwException;

    public ResilientCommand(String value) {
        this(value, false);
    }

    public ResilientCommand(String value, boolean throwException) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.value = value;
        this.throwException = throwException;
    }

    @Override
    protected String run() {
        /**
         * Fail Fast
         */
        if (throwException) {
            throw new RuntimeException("failure from CommandThatFailsFast");
        } else {
            return "Hello " + value + "!";
        }
    }

    @Override
    protected String getFallback() {
        return "Hello Failure " + value + "!";

        /**
         * Fail Silent
         */
        //return null;
    }

    /**
     * Request Cache
     *
     * @return
     */
    @Override
    protected String getCacheKey() {
        return String.valueOf(value);
    }
}

