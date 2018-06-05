package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.entity.BandAlarm;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 读取闹钟
 * @ClassPath com.fitpolo.support.task.ZReadAlarmsTask
 */
public class ZReadAlarmsTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;
    // 读取闹钟
    private static final int READ_ALARMS = 0x01;
    // 读取闹钟详情
    private static final int READ_ALARMS_DETAILS = 0x02;

    private byte[] orderData;

    private int groupIndex;

    private ArrayList<BandAlarm> alarms;

    public ZReadAlarmsTask(MokoOrderTaskCallback callback) {
        super(OrderType.READ_CHARACTER, OrderEnum.Z_READ_ALARMS, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_READ_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x00;
        alarms = new ArrayList<>();
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }

    @Override
    public void parseValue(byte[] value) {
        int order_type = DigitalConver.byte2Int(value[1]);
        int data_length = DigitalConver.byte2Int(value[2]);
        LogModule.i(order.getOrderName() + "成功");
        switch (order_type) {
            case READ_ALARMS:
                if (data_length != 1) {
                    return;
                }
                groupIndex = DigitalConver.byte2Int(value[3]);
                if (groupIndex > 0) {
                    return;
                }
            case READ_ALARMS_DETAILS:
                if (groupIndex > 0) {
                    for (int i = 0; i < data_length; i++) {
                        BandAlarm alarm = new BandAlarm();
                        alarm.type = DigitalConver.byte2Int(value[3 + i]);
                        i++;
                        alarm.state = DigitalConver.byte2binaryString((value[3 + i]));
                        i++;
                        int hour = DigitalConver.byte2Int(value[3 + i]);
                        i++;
                        int min = DigitalConver.byte2Int(value[3 + i]);
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        calendar.set(Calendar.MINUTE, min);
                        alarm.time = new SimpleDateFormat("HH:mm").format(calendar.getTime());
                        alarms.add(alarm);
                    }
                    groupIndex--;
                    if (groupIndex > 0) {
                        return;
                    }
                }
                break;
            default:
                return;
        }
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        MokoSupport.getInstance().setAlarms(alarms);

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
