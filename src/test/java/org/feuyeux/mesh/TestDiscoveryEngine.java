package org.feuyeux.mesh;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.feuyeux.mesh.config.EtcdProperties;
import org.feuyeux.mesh.engine.DiscoveryEngine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SdkApplication.class)
public class TestDiscoveryEngine {
    private DiscoveryEngine discoveryEngine;
    @Autowired
    private EtcdProperties etcdProperties;

    @Before
    public void before() {
        discoveryEngine = new DiscoveryEngine();
        discoveryEngine.refresh(etcdProperties);
    }

    @Test
    public void test() throws InterruptedException {
        testRegister();
        TimeUnit.SECONDS.sleep(15);
        testDiscovery();
        TimeUnit.SECONDS.sleep(15);
        testDiscovery();
        TimeUnit.SECONDS.sleep(15);
        testUnRegister();
    }

    public void testRegister() {
        log.info("====testRegister====\n");
        String s = discoveryEngine.register("A", "B", 9000);
        Assert.assertNotNull(s);
        log.info("Register Result={}", s);
        testDiscovery();
    }

    private void testDiscovery() {
        log.info("====testRegister====\n");
        List<String> l = discoveryEngine.discovery("A", "B");
        l.forEach(n -> log.info("Discovery:{}", n));
    }

    public void testUnRegister() {
        log.info("====testRegister====\n");
        discoveryEngine.unRegister("A", "B", 9000);
        testDiscovery();
    }

    @After
    public void after() {
        discoveryEngine.close();
    }
}
