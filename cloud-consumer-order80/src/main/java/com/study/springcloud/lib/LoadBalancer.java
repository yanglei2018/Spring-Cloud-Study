package com.study.springcloud.lib;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/15 19:11
 * @description
 */
public interface LoadBalancer {
    //获取机器总数
    ServiceInstance instance(List<ServiceInstance> serviceInstances);
}
