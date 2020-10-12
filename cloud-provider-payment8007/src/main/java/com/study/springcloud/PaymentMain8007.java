package com.study.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/12 20:13
 * @description
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentMain8007 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8007.class, args);
    }
}
