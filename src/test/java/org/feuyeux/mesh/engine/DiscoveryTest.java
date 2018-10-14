package org.feuyeux.mesh.engine;

import java.net.SocketException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.feuyeux.mesh.config.EtcdProperties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(value = "classpath:application-test.yml")
public class DiscoveryTest {
    public static final int TIMEOUT = 1;
    public static final int LOOP = 1;
    public static final int TTL = 6;
    @Autowired
    private DiscoveryEngine discoveryEngine;
    @Autowired
    private EtcdProperties etcdProperties;

    @Before
    public void before() {
        discoveryEngine.refresh(etcdProperties);
    }

    //@Test
    public void test() throws InterruptedException {
        testRegister();
        for (int i = 0; i < LOOP; i++) {
            testDiscovery();
            TimeUnit.SECONDS.sleep(TIMEOUT);
        }
        testUnRegister();
    }

    public void testRegister() {
        log.info("\n====testRegister====");
        String s = discoveryEngine.register("A", "B", 9000);
        Assert.assertNotNull(s);
        log.info("Register Result={}", s);
        testDiscovery();
    }

    private void testDiscovery() {
        log.info("\n====testDiscovery====");
        List<String> l = discoveryEngine.discovery("A", "B");
        l.stream().forEach(n -> log.info("Discovery:{}", n));
    }

    public void testUnRegister() throws InterruptedException {
        log.info("\n====testUnRegister====");
        discoveryEngine.unRegister("A", "B", 9000);
        TimeUnit.SECONDS.sleep(TTL);
        testDiscovery();
    }

    @Test
    public void testLocalIp() {
        try {
            String localIp = DiscoveryEngine.getLocalIp();
            log.info("localIp ={}", localIp);
        } catch (SocketException e) {
            log.error("", e);
        }
    }

    @After
    public void after() {
        discoveryEngine.close();
    }
}
