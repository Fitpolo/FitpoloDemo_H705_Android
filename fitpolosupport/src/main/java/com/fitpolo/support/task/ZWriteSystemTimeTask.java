package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

import java.util.Calendar;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 设置系统时间
 * @ClassPath com.fitpolo.support.task.ZWriteSystemTimeTask
 */
public class ZWriteSystemTimeTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 9;

    private byte[] orderData;

    public ZWriteSystemTimeTask(MokoOrderTaskCallback callback) {
        super(OrderType.WRITE_CHARACTER, OrderEnum.Z_WRITE_SYSTEM_TIME, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_WRITE_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x06;
        orderData[3] = (byte) (year - 2000);
        orderData[4] = (byte) month;
        orderData[5] = (byte) date;
        orderData[6] = (byte) hour;
        orderData[7] = (byte) minute;
        orderData[8] = (byte) second;
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
