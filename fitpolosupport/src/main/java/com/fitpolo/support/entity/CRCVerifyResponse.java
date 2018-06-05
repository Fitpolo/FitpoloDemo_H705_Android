package com.fitpolo.support.entity;

/**
 * @Date 2017/8/15
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.fitpolo.support.entity.CRCVerifyResponse
 */
public class CRCVerifyResponse extends OrderTaskResponse {
    public byte[] packageResult;
    public int header;
    public int ack;
}
