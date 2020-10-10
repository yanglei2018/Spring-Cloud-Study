package com.study.springcloud.service.impl;

import com.study.springcloud.dao.PaymentDao;
import com.study.springcloud.entities.Paymet;
import com.study.springcloud.service.PaymentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/10/9 11:45
 * @description
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    @Resource
    private PaymentDao paymentDao;

    @Override
    public int create(Paymet paymet) {
        int i = paymentDao.create(paymet);
        return i;
    }

    @Override
    public Paymet getPaymentById(Long id) {
        return paymentDao.getPaymentById(id);
    }
}
