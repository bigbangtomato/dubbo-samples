package org.apache.dubbo.springboot.demo.consumer;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.DefaultModuleDeployer;
import org.apache.dubbo.config.spring.context.DubboConfigApplicationListener;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringContextUtil implements ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringContextUtil.class);

    private static ApplicationContext context = null;
	private static SpringContextUtil singleton;
	private static ConfigurableListableBeanFactory configurableListableBeanFactory;
    private DubboConfigApplicationListener dubboConfigApplicationListener = null;
    private DubboShutdownHook dubboShutdownHook = null;

    private final AtomicBoolean lockZookeeper = new AtomicBoolean(false);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
	    singleton= this;

	    configurableListableBeanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
    }

	public static SpringContextUtil instance() {
        singleton.retrieveDubboListener();
		return singleton;
	}

    private void retrieveDubboListener() {
        if (!lockZookeeper.compareAndSet(false, true)) {
            return;
        }
        for (ApplicationListener<?> listener : ((AnnotationConfigApplicationContext)context).getApplicationListeners()) {
            if (listener.getClass().getName().equals(DubboConfigApplicationListener.class.getName())) {
                this.dubboConfigApplicationListener = (DubboConfigApplicationListener)listener;
            }
        }
    }

    public void unregisterDubboShutdownHook() {
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

	public String substitueProperties(String str) {
		return configurableListableBeanFactory.resolveEmbeddedValue(str);
	}

    public <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }

    public Object getBean(String beanId) {
        return context.getBean(beanId);
    }
}
