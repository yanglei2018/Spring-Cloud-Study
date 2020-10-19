package com.study.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/17 16:04
 * @description
 */
@SpringBootApplication
@EnableFeignClients
@EnableHystrix
//@EnableCircuitBreaker @EnableHystrix继承了@EnableCricuitBreaker
public class OrderHystrixMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderHystrixMain80.class, args);
    }
}
