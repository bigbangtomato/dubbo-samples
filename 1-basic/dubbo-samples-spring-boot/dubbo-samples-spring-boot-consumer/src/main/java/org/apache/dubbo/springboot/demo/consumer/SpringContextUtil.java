package org.apache.dubbo.springboot.demo.consumer;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.imps.CuratorFrameworkImpl;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.details.ServiceDiscoveryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.zookeeper.serviceregistry.ZookeeperAutoServiceRegistration;
import org.springframework.cloud.zookeeper.serviceregistry.ZookeeperServiceRegistry;
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

    private ZookeeperAutoServiceRegistration zookeeperAutoServiceRegistration;
    private ZookeeperServiceRegistry zookeeperServiceRegistry = null;
    private CuratorZookeeperClient zookeeperClient = null;
    private final AtomicBoolean lockZookeeper = new AtomicBoolean(false);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
	    singleton= this;

	    configurableListableBeanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
    }

	public static SpringContextUtil instance() {
        singleton.retrieveZookeeperRegistry();
		return singleton;
	}

    private void retrieveZookeeperRegistry() {
        if (!lockZookeeper.compareAndSet(false, true)) {
            return;
        }
        for (ApplicationListener<?> listener : ((AnnotationConfigApplicationContext)context).getApplicationListeners()) {
            if (listener.getClass().getName().equals(ZookeeperAutoServiceRegistration.class.getName())) {
                this.zookeeperAutoServiceRegistration = (ZookeeperAutoServiceRegistration)listener;
                getZookeeper();
            }
        }
    }

    private void getZookeeper() {
        Class clazz = AbstractAutoServiceRegistration.class;
        try {
            Field filedServiceRegistry = clazz.getDeclaredField("serviceRegistry");
            //设置即使该属性是private，也可以进行访问(默认是false)
            filedServiceRegistry.setAccessible(true);
            this.zookeeperServiceRegistry = (ZookeeperServiceRegistry)filedServiceRegistry.get(zookeeperAutoServiceRegistration);

            Field fieldServiceDiscovery = ZookeeperServiceRegistry.class.getDeclaredField("serviceDiscovery");
            fieldServiceDiscovery.setAccessible(true);
            ServiceDiscovery serviceDiscovery = (ServiceDiscoveryImpl)fieldServiceDiscovery.get(zookeeperServiceRegistry);

            Field fieldCuratorFrame = ServiceDiscoveryImpl.class.getDeclaredField("client");
            fieldCuratorFrame.setAccessible(true);
            CuratorFrameworkImpl tmp = (CuratorFrameworkImpl)fieldCuratorFrame.get(serviceDiscovery);


            Field fieldZkClient = CuratorFrameworkImpl.class.getDeclaredField("client");
            fieldZkClient.setAccessible(true);
            zookeeperClient = (CuratorZookeeperClient)fieldZkClient.get(tmp);
        }catch (Exception e){
            LOGGER.error("failed to get zookeeper registry", e);
        }
        // ((ZookeeperServiceRegistry) zookeeperAutoServiceRegistration.getServiceRegistry()).close();
    }

    public void closeZookeeper() {
        this.zookeeperServiceRegistry.close();
        this.zookeeperClient.close();
    }

    public ApplicationContext getContext() {
        return context;
    }

    public ZookeeperAutoServiceRegistration getZookeeperAutoServiceRegistration() {
        return this.zookeeperAutoServiceRegistration;
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
