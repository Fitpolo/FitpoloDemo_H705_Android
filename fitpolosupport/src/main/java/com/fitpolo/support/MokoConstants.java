package com.fitpolo.support;

/**
 * @Date 2017/5/10
 * @Author wenzheng.liu
 * @Description 蓝牙常量
 */
public class MokoConstants {
    // 读取发送头
    public static final int HEADER_READ_SEND = 0xB0;
    // 读取接收头
    public static final int HEADER_READ_GET = 0xB1;
    // 设置发送头
    public static final int HEADER_WRITE_SEND = 0xB2;
    // 设置接收头
    public static final int HEADER_WRITE_GET = 0xB3;
    // 记步发送头
    public static final int HEADER_SETP_SEND = 0xB4;
    // 记步接收头
    public static final int HEADER_STEP_GET = 0xB5;
    // 心率发送头
    public static final int HEADER_HEARTRATE_SEND = 0xB6;
    // 心率接收头
    public static final int HEADER_HEARTRATE_GET = 0xB7;

    // 发现状态
    public static final String ACTION_DISCOVER_SUCCESS = "com.moko.fitpolo.ACTION_DISCOVER_SUCCESS";
    public static final String ACTION_DISCOVER_TIMEOUT = "com.moko.fitpolo.ACTION_DISCOVER_TIMEOUT";
    // 断开连接
    public static final String ACTION_CONN_STATUS_DISCONNECTED = "com.moko.fitpolo.ACTION_CONN_STATUS_DISCONNECTED";
    // 命令结果
    public static final String ACTION_ORDER_RESULT = "com.moko.fitpolo.ACTION_ORDER_RESULT";
    public static final String ACTION_ORDER_TIMEOUT = "com.moko.fitpolo.ACTION_ORDER_TIMEOUT";
    public static final String ACTION_ORDER_FINISH = "com.moko.fitpolo.ACTION_ORDER_FINISH";
    public static final String ACTION_CURRENT_DATA = "com.moko.fitpolo.ACTION_CURRENT_DATA";

    // extra_key
    public static final String EXTRA_KEY_RESPONSE_ORDER_TASK = "EXTRA_KEY_RESPONSE_ORDER_TASK";
    public static final String EXTRA_KEY_CURRENT_DATA_TYPE = "EXTRA_KEY_CURRENT_DATA_TYPE";
}
