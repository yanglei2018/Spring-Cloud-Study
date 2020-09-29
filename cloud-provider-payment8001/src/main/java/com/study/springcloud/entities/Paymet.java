package com.study.springcloud.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data //使用这个注解，就不用再去手写Getter,Setter,equals,canEqual,hasCode,toString等方法
@AllArgsConstructor//添加一个有参构造器
@NoArgsConstructor//添加一个无参的构造器
public class Paymet implements Serializable {
    private long id;
    private String serial;
}
