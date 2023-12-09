package org.apache.dubbo.springboot.demo.consumer.aop;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.springboot.demo.consumer.hook.SpringShutdownHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

@Activate(group = CONSUMER, order = -1)
@Component
public class GracefulShuttingDown implements Filter, ScopeModelAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(GracefulShuttingDown.class);

    private static final AtomicInteger count = new AtomicInteger(0);
    private static final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @Autowired
    private SpringShutdownHook springShutdownHook;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 扩展点的具体逻辑
        try {
            count.incrementAndGet();
            LOGGER.info("processing {}", invocation.getMethodName());
            return invoker.invoke(invocation);
        } finally {
            count.addAndGet(-1);
        }
    }

    private static int countRpcCalling() {
        return count.get();
    }

    @PostConstruct
    private void init() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.info("GracefulShuttingDown marked.");
                shuttingDown.set(true);
                try {
                    int timeWait = 10000;
                    while (countRpcCalling() > 0) {
                        Thread.sleep(timeWait);
                    }
                    Thread.sleep(timeWait);
                } catch (InterruptedException e) {
                    LOGGER.error("interrupted when waiting for shuting down spring.", e);
                }
                springShutdownHook.shuttingDown();
            }
        });
    }

    public static boolean isShuttingDown() {
        return shuttingDown.get();
    }
}
