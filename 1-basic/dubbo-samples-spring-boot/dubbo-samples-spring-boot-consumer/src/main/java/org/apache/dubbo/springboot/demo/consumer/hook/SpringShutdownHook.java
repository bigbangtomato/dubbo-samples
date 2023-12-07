package org.apache.dubbo.springboot.demo.consumer.hook;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.apache.dubbo.springboot.demo.consumer.aop.GracefulShuttingDown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringShutdownHook {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringShutdownHook.class);

    @Autowired
    private ConfigurableApplicationContext configurableApplicationContext;

    private final BlockingQueue<Object> blockingQueue = new LinkedBlockingQueue<>();

    public void shuttingDown() {
        this.blockingQueue.add(new Object());
    }

    @PostConstruct
    public void registerShutdownHook() {
        LOGGER.info("[SpringShutdownHook] Register ShutdownHook....");
        Thread shutdownHook = new Thread() {
            public void run() {
//                shutdownDirectly();
                shutdownWaiting();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void shutdownDirectly() {
        configurableApplicationContext.close();
    }

    private void shutdownWaiting() {
        try {
            int timeWait = 10000;
            LOGGER.info("[SpringShutdownHook] Application need sleep {} seconds to wait Dubbo shutdown", (double)timeWait / 1000.0D);
//            blockingQueue.take();
            while (GracefulShuttingDown.countRpcCalling() >0) {
                Thread.sleep(timeWait);
            }
            Thread.sleep(timeWait);
            configurableApplicationContext.close();
            LOGGER.info("[SpringShutdownHook] ApplicationContext closed, Application shutdown");
        } catch (InterruptedException e) {
            LOGGER.error("interrupted when waiting for shuting down spring.", e);
        } finally {

        }
    }
}
