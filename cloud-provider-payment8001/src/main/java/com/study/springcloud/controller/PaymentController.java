package com.study.springcloud.controller;

import com.study.springcloud.entities.CommonResult;
import com.study.springcloud.entities.Paymet;
import com.study.springcloud.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/9/28 20:25
 * @RequestBody：主要用来接收前端传递给后端的json字符串中的数据的
 */
@Controller
@Slf4j
public class PaymentController {
    @Resource
    private PaymentService paymentService;

    @Resource
    private DiscoveryClient discoveryClient;

    @Value("${server.port}")
    private String serverPort;

    @PostMapping(value = "/payment/create")
    @ResponseBody
    public CommonResult create(@RequestBody Paymet payment){
        int i = paymentService.create(payment);
        log.info("插入结果："+i);
        if (i>0){
            return new CommonResult(200,"插入数据库成功,serverPort:"+ serverPort, i);
        }else{
            return new CommonResult(444,"插入数据库失败",null);
        }
    }

    @GetMapping(value = "/payment/get/{id}")
    @ResponseBody
    public CommonResult getPaymentById(@PathVariable("id") Long id){
        Paymet paymet = paymentService.getPaymentById(id);
        log.info("查询结果："+paymet);
        if (paymet !=null){
            return new CommonResult(200,"查询成功,serverPort:"+ serverPort,paymet);
        }else{
            return new CommonResult(444,"查询失败",null);
        }
    }

    /**
     * 查询本微服务的相关信息
     * @return
     */
    @GetMapping(value = "/payment/discovery")
    @ResponseBody
    public Object discovery(){
        //发现服务 cloud-payment-service cloud-oreder-service
        List<String> services = discoveryClient.getServices();
        for(String element: services){
            log.info("*****element:"+element);
        }

        //获取一个Instances下相关的信息
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
        for (ServiceInstance instance : instances){
            log.info(instance.getServiceId()+"\t"+ instance.getHost()+ "\t" + instance.getPort() + "\t"+instance.getUri());
        }

        return this.discoveryClient;
    }

    @GetMapping(value = "/payment/lb")
    @ResponseBody
    public String getPaymentLB(){
        return serverPort;
    }

}
