package com.fitpolo.support.callback;

import com.fitpolo.support.entity.OrderTaskResponse;

/**
 * @Date 2017/5/10
 * @Author wenzheng.liu
 * @Description 返回数据回调类
 * @ClassPath com.fitpolo.support.callback.MokoOrderTaskCallback
 */
public interface MokoOrderTaskCallback {

    void onOrderResult(OrderTaskResponse response);

    void onOrderTimeout(OrderTaskResponse response);

    void onOrderFinish();
}
