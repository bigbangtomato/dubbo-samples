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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
	    singleton= this;

	    configurableListableBeanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
    }

	public static SpringContextUtil instance() {
		return singleton;
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
