package com.study.springcloud.service;

import com.study.springcloud.entities.CommonResult;
import com.study.springcloud.entities.Paymet;
import feign.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/16 14:34
 * @description
 */
@Component
@FeignClient(value = "CLOUD-PAYMENT-SERVICE")
public interface PaymentFeignService {
    @GetMapping(value = "/payment/get/{id}")
    public CommonResult<Paymet> getPaymentById(@PathVariable("id") Long id);
}
