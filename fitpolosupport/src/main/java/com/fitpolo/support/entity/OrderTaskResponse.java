package com.fitpolo.support.entity;

import java.io.Serializable;

/**
 * @Date 2018/2/8
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.fitpolo.support.entity.OrderTaskResponse
 */
public class OrderTaskResponse implements Serializable {
    public OrderEnum order;
    public int responseType;
    public byte[] responseValue;
}
