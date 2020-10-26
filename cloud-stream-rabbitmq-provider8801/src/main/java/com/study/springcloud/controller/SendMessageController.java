package com.study.springcloud.controller;

import com.study.springcloud.service.IMessageProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/26 21:17
 * @description
 */
@RestController
public class SendMessageController {
    @Resource
    private IMessageProvider messageProvider;

    @GetMapping(value = "/send")
    public String send(){
        return messageProvider.send();
    }
}
