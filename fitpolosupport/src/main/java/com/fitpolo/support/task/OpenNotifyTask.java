package com.fitpolo.support.task;

import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;


public class OpenNotifyTask extends OrderTask {
    public byte[] data;

    public OpenNotifyTask(OrderType orderType, OrderEnum orderEnum, MokoOrderTaskCallback callback) {
        super(orderType, orderEnum, callback, OrderTask.RESPONSE_TYPE_NOTIFY);
        data = new byte[0];
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
