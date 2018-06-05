package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.AutoLighten;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 读取翻腕亮屏
 * @ClassPath com.fitpolo.support.task.ZReadAutoLightenTask
 */
public class ZReadAutoLightenTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    private byte[] orderData;

    public ZReadAutoLightenTask(MokoOrderTaskCallback callback) {
        super(OrderType.READ_CHARACTER, OrderEnum.Z_READ_AUTO_LIGHTEN, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_READ_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x00;
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
        if (0x05 != DigitalConver.byte2Int(value[2])) {
            return;
        }
        AutoLighten autoLighten = new AutoLighten();
        autoLighten.autoLighten = DigitalConver.byte2Int(value[3]);
        int startHour = DigitalConver.byte2Int(value[4]);
        int startMin = DigitalConver.byte2Int(value[5]);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, startMin);
        autoLighten.startTime = new SimpleDateFormat("HH:mm").format(calendar.getTime());
        int endHour = DigitalConver.byte2Int(value[6]);
        int endMin = DigitalConver.byte2Int(value[7]);
        calendar.set(Calendar.HOUR_OF_DAY, endHour);
        calendar.set(Calendar.MINUTE, endMin);
        autoLighten.endTime = new SimpleDateFormat("HH:mm").format(calendar.getTime());
        MokoSupport.getInstance().setAutoLighten(autoLighten);

        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
