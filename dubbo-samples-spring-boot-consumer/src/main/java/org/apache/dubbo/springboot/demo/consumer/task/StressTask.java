/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.springboot.demo.consumer.task;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.springboot.demo.DemoService;
import org.apache.dubbo.springboot.demo.consumer.aop.GracefulShuttingDown;
import org.apache.dubbo.springboot.demo.consumer.hook.SpringShutdownHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("stress")
@Component
public class StressTask implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(StressTask.class);

    @DubboReference(version = "1.0.0")
    private DemoService demoService;

    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public void run(String... args) throws Exception {
        String result = demoService.sayHello("world");
        LOGGER.info("Receive result ======> " + result);
        for (int i=0; i<10; i++) {
            buildThread(i);
        }
    }

    private void buildThread(int num) {
        new Thread(new Runnable() {
            private int threadNum = num;

	        @Override
	        public void run() {
		        int i = 0;
		        while (!GracefulShuttingDown.isShuttingDown()) {
			        i++;
			        running.set(true);
			        try {
				        String res1 = demoService.sayHello("world");
				        LOGGER.info("thread {} Receive result ======> {}", threadNum, res1);

				        String resInt = demoService.sayHello(i);
				        LOGGER.info("thread {} Receive result ======> {}", threadNum, resInt);
			        } catch (RpcException e) {
				        LOGGER.error("error code is {}.", e.getCode(), e);
			        } catch (Exception e) {
				        LOGGER.error("failed to destroy registry at V1.", e);
			        }
		        }
	        }
        }).start();
    }

}
