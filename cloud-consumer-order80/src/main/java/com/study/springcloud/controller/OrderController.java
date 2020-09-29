package com.study.springcloud.controller;


import com.study.springcloud.entities.CommonResult;
import com.study.springcloud.entities.Paymet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/9/29 12:53
 */
@RestController
public class OrderController {
    public static final String PAYMENT_URL = "http://localhost:8001/";

    @Resource
    private RestTemplate restTemplate;

    @GetMapping("/consumer/payment/create")
    public CommonResult<Paymet> create(Paymet paymet){
        return restTemplate.postForObject(PAYMENT_URL+"/payment/create",paymet,CommonResult.class);
    }

    @GetMapping("/consumer/payment/get/{id}")
    public CommonResult<Paymet> getPayment(@PathVariable("id") Long id){
        return restTemplate.getForObject(PAYMENT_URL+"/payment/get/"+id,CommonResult.class);
    }
}
