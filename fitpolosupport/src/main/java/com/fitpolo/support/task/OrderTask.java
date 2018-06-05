package com.fitpolo.support.task;

import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderTaskResponse;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 发送命令任务
 * @ClassPath com.fitpolo.support.task.OrderTask
 */
public abstract class OrderTask {
    public static final long DEFAULT_DELAY_TIME = 3000;
    public static final int RESPONSE_TYPE_NOTIFY = 2;
    public static final int RESPONSE_TYPE_WRITE_NO_RESPONSE = 3;
    public static final int ORDER_STATUS_SUCCESS = 1;
    public OrderType orderType;
    public OrderEnum order;
    public MokoOrderTaskCallback callback;
    public OrderTaskResponse response;
    public long delayTime = DEFAULT_DELAY_TIME;
    public int orderStatus;

    public OrderTaskResponse getResponse() {
        return response;
    }

    public void setResponse(OrderTaskResponse response) {
        this.response = response;
    }

    public OrderTask(OrderType orderType, OrderEnum order, MokoOrderTaskCallback callback, int responseType) {
        response = new OrderTaskResponse();
        this.orderType = orderType;
        this.order = order;
        this.callback = callback;
        this.response.order = order;
        this.response.responseType = responseType;
    }

    public abstract byte[] assemble();


    public MokoOrderTaskCallback getCallback() {
        return callback;
    }

    public void setCallback(MokoOrderTaskCallback callback) {
        this.callback = callback;
    }


    public OrderEnum getOrder() {
        return order;
    }

    public void setOrder(OrderEnum order) {
        this.order = order;
    }


    public void parseValue(byte[] value) {
    }

    public Runnable timeoutRunner = new Runnable() {
        @Override
        public void run() {
            if (orderStatus != OrderTask.ORDER_STATUS_SUCCESS) {
                if (timeoutPreTask()) {
                    MokoSupport.getInstance().pollTask();
                    callback.onOrderTimeout(response);
                    MokoSupport.getInstance().executeTask(callback);
                }
            }
        }
    };

    public boolean timeoutPreTask() {
        LogModule.i(order.getOrderName() + "超时");
        return true;
    }
}
