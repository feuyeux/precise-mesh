package org.feuyeux.mesh.engine;

import java.util.concurrent.Future;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import rx.Observable;
import rx.Observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(value = "classpath:application-test.yml")
public class ResilientTest {
    @Autowired
    private ResilientEngine resilientEngine;

  /*  @Before
    public void tearUp() {
        resilientEngine = new ResilientEngine();
    }

    @After
    public void tearDown() {
        resilientEngine.destroy();
    }*/

    @Test
    public void testSynchronous() {
        assertEquals("Hello World!", resilientEngine.execute("World"));
    }

    @Test
    public void testAsynchronous1() throws Exception {
        assertEquals("Hello World!", resilientEngine.queue("World").get());
    }

    @Test
    public void testAsynchronous2() throws Exception {
        Future<String> fWorld = resilientEngine.queue("World");
        assertEquals("Hello World!", fWorld.get());
    }

    //@Test
    public void testObservable() throws Exception {

        Observable<String> fWorld = resilientEngine.observe("World");

        // blocking
        String actual = fWorld.toBlocking().single();
        assertEquals("Hello World!", actual);

        // non-blocking
        // - this is a verbose anonymous inner-class approach and doesn't do assertions
        fWorld.subscribe(new Observer<String>() {

            @Override
            public void onCompleted() {
                // nothing needed here
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(String v) {
                System.out.println("onNext: " + v);
            }

        });
    }

    //@Test
    public void testCollapser() throws Exception {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            Future<String> f1 = resilientEngine.collapser(1).queue();
            Future<String> f2 = resilientEngine.collapser(2).queue();
            Future<String> f3 = resilientEngine.collapser(3).queue();
            Future<String> f4 = resilientEngine.collapser(4).queue();

            assertEquals("ValueForKey: 1", f1.get());
            assertEquals("ValueForKey: 2", f2.get());
            assertEquals("ValueForKey: 3", f3.get());
            assertEquals("ValueForKey: 4", f4.get());

            // assert that the batch command 'GetValueForKey' was in fact
            // executed and that it executed only once
            assertEquals(1, HystrixRequestLog.getCurrentRequest().getExecutedCommands().size());
            HystrixCommand<?> command = HystrixRequestLog.getCurrentRequest().getExecutedCommands().toArray(
                new HystrixCommand<?>[1])[0];
            // assert the command is the one we're expecting
            assertEquals("GetValueForKey", command.getCommandKey().name());
            // confirm that it was a COLLAPSED command execution
            assertTrue(command.getExecutionEvents().contains(HystrixEventType.COLLAPSED));
            // and that it was successful
            assertTrue(command.getExecutionEvents().contains(HystrixEventType.SUCCESS));
        } finally {
            context.shutdown();
        }
    }
}
