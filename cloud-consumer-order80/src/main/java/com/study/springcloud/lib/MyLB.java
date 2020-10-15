package com.study.springcloud.lib;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/15 19:13
 * @description 手写轮询算法
 */
@Component
public class MyLB implements LoadBalancer{

    private AtomicInteger atomicInteger = new AtomicInteger(0);//原子操作类

    public final int getAndIncrement(){
        int current;
        int next;
        do{
            current = this.atomicInteger.get();
            next = current >= 2147483647 ? 0 : current + 1; //判断是否达到最大
        }while (!this.atomicInteger.compareAndSet(current, next));//当目前的值和下一个值不相等，那么自询
        System.out.println("*****next:" +next);
        return next;
    }
    @Override
    public ServiceInstance instance(List<ServiceInstance> serviceInstances) {
        int index = getAndIncrement() % serviceInstances.size(); //负载均衡算法
        return serviceInstances.get(index);//返回访问的主机序号
    }
}
