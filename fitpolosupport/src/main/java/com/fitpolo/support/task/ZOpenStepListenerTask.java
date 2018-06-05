package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 监听记步
 * @ClassPath com.fitpolo.support.task.ZOpenStepListenerTask
 */
public class ZOpenStepListenerTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 4;

    private byte[] orderData;

    public ZOpenStepListenerTask(MokoOrderTaskCallback callback) {
        super(OrderType.STEP_CHARACTER, OrderEnum.Z_STEPS_CHANGES_LISTENER, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_SETP_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x01;
        orderData[3] = (byte) 0x01;
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }

    @Override
    public void parseValue(byte[] value) {
        if (order.getOrderHeader() != DigitalConver.byte2Int(value[1])) {
            return;
        }
        if (0x01 != DigitalConver.byte2Int(value[2])) {
            return;
        }
        if (0x00 != DigitalConver.byte2Int(value[3])) {
            return;
        }

        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
