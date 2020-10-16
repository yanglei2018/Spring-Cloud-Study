package com.study.springcloud.controller;


import com.study.springcloud.entities.CommonResult;
import com.study.springcloud.entities.Paymet;
import com.study.springcloud.lib.LoadBalancer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.URI;
import java.util.List;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/9/29 12:53
 */
@RestController
public class OrderController {
    //public static final String PAYMENT_URL = "http://localhost:8001/";
    public static final String PAYMENT_URL = "http://CLOUD-PAYMENT-SERVICE"; //获取集群中名为CLOUD-PAYMENT-SERVICE的服务
    @Resource
    private RestTemplate restTemplate;

    @Resource
    private LoadBalancer loadBalancer;//引入自己的轮询规则

    @Resource
    private DiscoveryClient discoveryClient;

    @GetMapping("/consumer/payment/create")
    public CommonResult<Paymet> create(Paymet paymet){
        return restTemplate.postForObject(PAYMENT_URL+"/payment/create",paymet,CommonResult.class);
    }

    @GetMapping("/consumer/payment/get/{id}")
    public CommonResult<Paymet> getPayment(@PathVariable("id") Long id){
        return restTemplate.getForObject(PAYMENT_URL+"/payment/get/"+id,CommonResult.class);
    }

    @GetMapping("/consumer/paymentEntity/get/{id}")
    public CommonResult<Paymet> getPayment2(@PathVariable("id") Long id){
        ResponseEntity<CommonResult> entity = restTemplate.getForEntity(PAYMENT_URL + "/payment/get/" + id, CommonResult.class);
        if(entity.getStatusCode().is2xxSuccessful()){ //如果状态码是2XX，则返回响应体
            return entity.getBody();
        }else {
            return new CommonResult<>(404,"操作失败");
        }
    }

    //调用自己的轮询规则
    @GetMapping(value = "/consumer/payment/lb")
    public String getPaymentLB(){
        //获取一个Instances下相关的信息
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
        if(instances == null || instances.size() <=0){ //w无效的实例
            return null;
        }
        ServiceInstance instance = loadBalancer.instance(instances);//将instances传入到自己的规则中
        URI uri = instance.getUri();

        return restTemplate.getForObject(uri + "/payment/lb", String.class);
    }
}
