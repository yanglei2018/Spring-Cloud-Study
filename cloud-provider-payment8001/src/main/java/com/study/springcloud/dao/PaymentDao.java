package com.study.springcloud.dao;

import com.study.springcloud.entities.Paymet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Yang Lei
 * @version 1.0
 * @date 2020/9/28 20:00
 */
@Mapper
public interface PaymentDao {
    public int create(Paymet paymet);

    public Paymet getPaymentById(@Param("id") Long id);
}
