package org.apache.dubbo.springboot.demo.consumer.hook;

import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.DefaultModuleDeployer;
import org.apache.dubbo.config.spring.context.DubboConfigApplicationListener;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringShutdownHook {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringShutdownHook.class);

    private final ConfigurableApplicationContext configurableApplicationContext;

    private final BlockingQueue<Object> blockingQueue = new LinkedBlockingQueue<>();

    private DubboConfigApplicationListener dubboConfigApplicationListener = null;
    private DubboShutdownHook dubboShutdownHook = null;

    private final AtomicBoolean lockZookeeper = new AtomicBoolean(false);

    public SpringShutdownHook(@Autowired ConfigurableApplicationContext configurableApplicationContext) {
        this.configurableApplicationContext = configurableApplicationContext;
    }

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
            LOGGER.info("[SpringShutdownHook] Application wait Dubbo shutdown");
            blockingQueue.take();
            configurableApplicationContext.close();
            LOGGER.info("[SpringShutdownHook] ApplicationContext closed, Application shutdown");
        } catch (InterruptedException e) {
            LOGGER.error("interrupted when waiting for shuting down spring.", e);
        } finally {

        }
    }


    // <editor-folder desc="禁用 dubbo 的 hook。">
    private void retrieveDubboListener() {
        for (ApplicationListener<?> listener : ((AnnotationConfigApplicationContext)configurableApplicationContext).getApplicationListeners()) {
            if (listener.getClass().getName().equals(DubboConfigApplicationListener.class.getName())) {
                this.dubboConfigApplicationListener = (DubboConfigApplicationListener)listener;
            }
        }
    }

    public void unregisterDubboShutdownHook() {
        if (!lockZookeeper.compareAndSet(false, true)) {
            return;
        }
        retrieveDubboListener();
        Class clazz = DubboConfigApplicationListener.class;
        try {
            Field filedModuleModel = clazz.getDeclaredField("moduleModel");
            //设置即使该属性是private，也可以进行访问(默认是false)
            filedModuleModel.setAccessible(true);
            ModuleModel moduleModel = (ModuleModel)filedModuleModel.get(dubboConfigApplicationListener);

            Field fieldModuleDeployer = ModuleModel.class.getDeclaredField("deployer");
            fieldModuleDeployer.setAccessible(true);
            ModuleDeployer serviceDiscovery = (ModuleDeployer)fieldModuleDeployer.get(moduleModel);

            Field fieldApplicationDeployer = DefaultModuleDeployer.class.getDeclaredField("applicationDeployer");
            fieldApplicationDeployer.setAccessible(true);
            ApplicationDeployer applicationDeployer = (ApplicationDeployer)fieldApplicationDeployer.get(serviceDiscovery);

            Field fieldDubboShutdownHook = DefaultApplicationDeployer.class.getDeclaredField("dubboShutdownHook");
            fieldDubboShutdownHook.setAccessible(true);
            this.dubboShutdownHook = (DubboShutdownHook)fieldDubboShutdownHook.get(applicationDeployer);
            dubboShutdownHook.unregister();
        }catch (Exception e){
            LOGGER.error("failed to get zookeeper registry", e);
        }
    }
    // </editor-folder>

}
