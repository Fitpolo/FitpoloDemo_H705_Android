package com.fitpolo.support.callback;

/**
 * @Date 2017/5/10
 * @Author wenzheng.liu
 * @Description 前端展示连接回调
 * @ClassPath com.fitpolo.support.callback.MokoConnStateCallback
 */
public interface MokoConnStateCallback {
    /**
     * @Date 2017/5/10
     * @Author wenzheng.liu
     * @Description 连接成功
     */
    void onConnectSuccess();

    /**
     * @Date 2017/5/10
     * @Author wenzheng.liu
     * @Description 断开连接
     */
    void onDisConnected();

    /**
     * @Date 2017/8/29
     * @Author wenzheng.liu
     * @Description 重连超时
     */
    void onConnTimeout(int reConnCount);
}
