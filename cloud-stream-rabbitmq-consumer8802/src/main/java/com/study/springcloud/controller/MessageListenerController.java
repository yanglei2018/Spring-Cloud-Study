package com.study.springcloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/26 21:34
 * @description
 */
@Component
@EnableBinding(Sink.class)
public class MessageListenerController {
    @Value("${server.port}")
    private  String serverPort;

    @StreamListener(Sink.INPUT)
    public void input(Message<String> message) {
        System.out.println("消费者1号， -----> 接受到的消息： " + message.getPayload()
                + "\t port: " + serverPort);
    }
}
