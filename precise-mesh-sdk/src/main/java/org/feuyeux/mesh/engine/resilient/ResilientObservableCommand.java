package org.feuyeux.mesh.engine.resilient;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.schedulers.Schedulers;

/**
 * @author 六翁 lu.hl@alibaba-inc.com
 * @date 2018/10/15
 */
public class ResilientObservableCommand extends HystrixObservableCommand<String> {

    public ResilientObservableCommand(String key) {
        super(HystrixCommandGroupKey.Factory.asKey(key));
    }

    @Override
    protected Observable<String> construct() {
        return Observable.create((OnSubscribe<String>)observer -> {
            try {
                if (!observer.isUnsubscribed()) {
                    // a real example would do work like a network call here
                    observer.onNext("Hello");
                    observer.onNext("!");
                    observer.onCompleted();
                }
            } catch (Exception e) {
                observer.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }
}
