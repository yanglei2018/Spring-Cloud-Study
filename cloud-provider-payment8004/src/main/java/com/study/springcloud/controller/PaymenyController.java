package com.study.springcloud.controller;

import com.study.springcloud.entities.CommonResult;
import com.study.springcloud.entities.Paymet;
import com.study.springcloud.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/9 11:46
 * @description
 */
@Controller
@Slf4j
public class PaymenyController {
    @Resource
    private PaymentService paymentService;

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
    @GetMapping(value = "payment/lb")
    public String getPaymentLB(){
        return serverPort;
    }
}
