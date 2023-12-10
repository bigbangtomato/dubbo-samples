package org.apache.dubbo.springboot.demo.provider.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PrintConfig implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrintConfig.class);

	@Autowired
	private Config config;

    @Override
    public void run(String... args) {
        new Thread(()-> {
            int i = 0;
            while (true)            {
	            try {
		            Thread.sleep(1000);
	            } catch (InterruptedException e) {
		            
	            }
	            LOGGER.info(" Receive result ======> {}.", config.getTestConfig());
            }
        }).start();
    }
}
