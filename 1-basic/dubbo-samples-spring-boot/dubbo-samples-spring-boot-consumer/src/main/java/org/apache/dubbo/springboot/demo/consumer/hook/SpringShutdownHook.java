package org.apache.dubbo.springboot.demo.consumer.hook;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

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
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            LOGGER.error("waiting for shutting down is interrupted", e);
        }
        this.blockingQueue.add(new Object());
    }

    @PostConstruct
    public void registerShutdownHook() {
        LOGGER.info("[SpringShutdownHook] Register ShutdownHook....");
        Thread shutdownHook = new Thread() {
            public void run() {
                try {
                    int timeOut = 30000;
                    LOGGER.info("[SpringShutdownHook] Application need sleep {} seconds to wait Dubbo shutdown", (double)timeOut / 1000.0D);
                    blockingQueue.take();
                    configurableApplicationContext.close();
                    LOGGER.info("[SpringShutdownHook] ApplicationContext closed, Application shutdown");
                } catch (InterruptedException e) {
	                LOGGER.error("interrupted when waiting for shuting down spring.", e);
                } finally {

                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
}
