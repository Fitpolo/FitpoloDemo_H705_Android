package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.NoDisturb;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 设置勿扰模式
 * @ClassPath com.fitpolo.support.task.ZWriteNoDisturbTask
 */
public class ZWriteNoDisturbTask extends OrderTask {
    public static final String PATTERN_HH_MM = "HH:mm";

    private static final int ORDERDATA_LENGTH = 8;

    private byte[] orderData;

    public ZWriteNoDisturbTask(MokoOrderTaskCallback callback, NoDisturb noDisturb) {
        super(OrderType.WRITE_CHARACTER, OrderEnum.Z_WRITE_NODISTURB, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_WRITE_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x05;
        orderData[3] = (byte) noDisturb.noDisturb;
        Calendar startCalendar = strDate2Calendar(noDisturb.startTime, PATTERN_HH_MM);
        orderData[4] = (byte) startCalendar.get(Calendar.HOUR_OF_DAY);
        orderData[5] = (byte) startCalendar.get(Calendar.MINUTE);
        Calendar endCalendar = strDate2Calendar(noDisturb.endTime, PATTERN_HH_MM);
        orderData[6] = (byte) endCalendar.get(Calendar.HOUR_OF_DAY);
        orderData[7] = (byte) endCalendar.get(Calendar.MINUTE);
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

    public static Calendar strDate2Calendar(String strDate, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
        try {
            Date date = sdf.parse(strDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
