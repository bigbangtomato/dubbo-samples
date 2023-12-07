package org.apache.dubbo.springboot.demo.consumer.aop;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

@Activate(group = CONSUMER, order = -1)
public class MyFilter implements Filter, ScopeModelAware {
    private static final AtomicInteger count = new AtomicInteger(0);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 扩展点的具体逻辑
        try {
            count.incrementAndGet();
            return invoker.invoke(invocation);
        } finally {
            count.addAndGet(-1);
        }
    }

    public static int countRpcCalling() {
        return count.get();
    }
}
