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
package org.apache.dubbo.springboot.demo.consumer;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.springboot.demo.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.zookeeper.serviceregistry.ZookeeperServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class Task implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);

    @DubboReference(version = "1.0.0")
    private DemoService demoService;

    @DubboReference(version = "2.0.0")
    private DemoService demoServiceV2;


    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private ZookeeperServiceRegistry serviceRegistry;

//    @Autowired
//    private ZookeeperServiceDiscovery zookeeperServiceDiscovery;

//    @Autowired
//    private ClientCnxn clientCnxn;

    @Override
    public void run(String... args) throws Exception {
        String result = demoService.sayHello("world");
        LOGGER.info("Receive result ======> " + result);

        String resultV2 = demoServiceV2.sayHello("hell");
        LOGGER.info("Receive result ======> " + resultV2);

        ApplicationContext context = SpringContextUtil.instance().getContext();
        new Thread(()-> {
            int i = 0;
//            while (true)
            {
                i++;
                try {
                    LOGGER.info("sleeping");
                    Thread.sleep(1000);
                    String res1 = demoService.sayHello("world");
                    LOGGER.info("going whatever");
                    LOGGER.info(" Receive result ======> " + res1);

                    String resInt = demoService.sayHello(i);
                    LOGGER.info(" Receive result ======> " + resInt);

//                    SpringContextUtil.instance().closeZookeeper();
                } catch (InterruptedException e) {
                    LOGGER.error("", e);
                    Thread.currentThread().interrupt();
                } catch (RpcException e) {
                    LOGGER.error("error code is {}.", e.getCode(), e);
//                    break;
                } catch (Exception e) {
                    LOGGER.error("failed to destroy zookeeperServiceDiscovery.", e);
                }

                // how to close curator gracefully?
            }
        }).start();
    }
}
