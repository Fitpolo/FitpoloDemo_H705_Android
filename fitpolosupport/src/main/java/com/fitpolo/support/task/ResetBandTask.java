package com.fitpolo.support.task;

import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

/**
 * @Date 2019/2/28
 * @Author wenzheng.liu
 * @Description 恢复出厂设置
 * @ClassPath com.fitpolo.support.task.ResetBandDataTask
 */
public class ResetBandTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 2;
    // 恢复出厂设置
    private static final int HEADER_RESET = 0x16;

    private byte[] orderData;

    public ResetBandTask(MokoOrderTaskCallback callback) {
        super(OrderType.WRITE, OrderEnum.RESET_DATA, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) HEADER_RESET;
        orderData[1] = (byte) order.getOrderHeader();
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
        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
