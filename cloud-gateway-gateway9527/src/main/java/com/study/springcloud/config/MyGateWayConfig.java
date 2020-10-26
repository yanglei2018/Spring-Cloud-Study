package com.study.springcloud.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/22 19:05
 * @description
 */
@Configuration
public class MyGateWayConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder){
        RouteLocatorBuilder.Builder routes = builder.routes();
        routes.route("mypath",r->r.path("/guonei").uri("http://news.baidu.com/guonei")).build();

        return routes.build();
    }
}
