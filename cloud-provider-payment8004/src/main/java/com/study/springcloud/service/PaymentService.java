package com.study.springcloud.service;

import com.study.springcloud.entities.Paymet;
import org.apache.ibatis.annotations.Param;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/9 11:44
 * @description
 */
public interface PaymentService {
    public int create(Paymet paymet);

    public Paymet getPaymentById(@Param("id") Long id);
}
