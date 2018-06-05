package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.BandAlarm;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

import java.util.ArrayList;
import java.util.List;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 设置手环闹钟
 * @ClassPath com.fitpolo.support.task.ZWriteAlarmsTask
 */
public class ZWriteAlarmsTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 4;
    private static final int HEADER_ALARMS_COUNT = 0x01;
    private static final int HEADER_ALARMS = 0x02;

    private byte[] orderData;
    private List<BandAlarm> alarms;
    private int groupCount;

    public ZWriteAlarmsTask(MokoOrderTaskCallback callback, List<BandAlarm> alarms) {
        super(OrderType.WRITE_CHARACTER, OrderEnum.Z_WRITE_ALARMS, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        this.alarms = alarms;
        groupCount = alarms.size() / 4;
        if (alarms.size() % 4 > 0) {
            groupCount++;
        }
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_WRITE_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x01;
        orderData[3] = (byte) groupCount;
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }

    @Override
    public void parseValue(byte[] value) {
        int header = DigitalConver.byte2Int(value[1]);
        LogModule.i(order.getOrderName() + "成功");
        switch (header) {
            case HEADER_ALARMS_COUNT:
            case HEADER_ALARMS:
                if (0x01 != DigitalConver.byte2Int(value[2])) {
                    return;
                }
                if (0x00 != DigitalConver.byte2Int(value[3])) {
                    return;
                }
                break;
            default:
                return;
        }
        if (groupCount > 0) {
            ArrayList<BandAlarm> unSyncAlarms = new ArrayList<>();
            if (groupCount > 1) {
                unSyncAlarms.addAll(alarms.subList(0, 4));
                alarms.removeAll(unSyncAlarms);
            } else {
                unSyncAlarms.addAll(alarms.subList(0, alarms.size()));
            }
            int max = unSyncAlarms.size();
            for (int i = 0; i < max; i++) {
                if (i == 0) {
                    orderData = new byte[3 + max * 4];
                    orderData[0] = (byte) MokoConstants.HEADER_WRITE_SEND;
                    orderData[1] = (byte) HEADER_ALARMS;
                    orderData[2] = (byte) (max * 4);
                }
                BandAlarm alarm = unSyncAlarms.get(i);
                orderData[4 * i + 3] = (byte) alarm.type;
                orderData[4 * i + 4] = (byte) Integer.parseInt(DigitalConver.binaryString2hexString(alarm.state), 16);
                orderData[4 * i + 5] = (byte) Integer.parseInt((alarm.time.split(":")[0]));
                orderData[4 * i + 6] = (byte) Integer.parseInt((alarm.time.split(":")[1]));
            }
            MokoSupport.getInstance().executeTask(null);
            groupCount--;
            return;
        }
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
