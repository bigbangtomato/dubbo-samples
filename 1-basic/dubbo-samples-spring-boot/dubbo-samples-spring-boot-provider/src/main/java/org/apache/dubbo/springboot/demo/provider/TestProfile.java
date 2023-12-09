package org.apache.dubbo.springboot.demo.provider;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile({ "!test & !dev" })
@Component
public class TestProfile {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestProfile.class);


    @PostConstruct
    private void init() {
        LOGGER.info("start xxxxxxxxxxxxxxx");
    }
}
