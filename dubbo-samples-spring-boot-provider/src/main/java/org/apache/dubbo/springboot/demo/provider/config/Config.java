package org.apache.dubbo.springboot.demo.provider.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@RefreshScope
public class Config {

	@Value("${testConfig:false}")
	private String testConfig;
}
