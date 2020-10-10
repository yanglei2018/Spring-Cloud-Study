package com.study.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/9 11:36
 * @description
 */
@SpringBootApplication
@EnableEurekaClient
@ComponentScan("com.study.springcloud.dao")
public class PaymentMain8004 {

    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8004.class, args);
    }
}
