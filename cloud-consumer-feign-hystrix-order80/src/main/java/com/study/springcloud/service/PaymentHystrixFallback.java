package com.study.springcloud.service;

import org.springframework.stereotype.Component;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/18 15:13
 * @description PaymentHystrixService服务降级
 */
@Component
public class PaymentHystrixFallback implements PaymentHystrixService{
    @Override
    public String paymentInfo_OK(Integer id) {
        return "paymentInfo_OK 服务降级方法";
    }

    @Override
    public String paymentInfo_TimeOut(Integer id) {
        return "paymentInfo_TimeOut 服务降级方法";
    }
}
