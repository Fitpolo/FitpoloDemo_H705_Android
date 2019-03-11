package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.HeartRate;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.ComplexDataParse;
import com.fitpolo.support.utils.DigitalConver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 读取心率
 * @ClassPath com.fitpolo.support.task.ZReadHeartRateTask
 */
public class ZReadHeartRateTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 8;
    private static final int HEADER_HEART_RATE_COUNT = 0x01;
    private static final int HEADER_HEART_RATE = 0x02;

    private byte[] orderData;

    private HashMap<Integer, Boolean> heartRatesMap;
    private int heartRateCount;
    private ArrayList<HeartRate> heartRates;

    private boolean isCountSuccess;

    public ZReadHeartRateTask(MokoOrderTaskCallback callback, Calendar lastSyncTime) {
        super(OrderType.HEART_RATE_CHARACTER, OrderEnum.Z_READ_HEART_RATE, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);

        int year = lastSyncTime.get(Calendar.YEAR) - 2000;
        int month = lastSyncTime.get(Calendar.MONTH) + 1;
        int day = lastSyncTime.get(Calendar.DAY_OF_MONTH);

        int hour = lastSyncTime.get(Calendar.HOUR_OF_DAY);
        int minute = lastSyncTime.get(Calendar.MINUTE);

        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_HEARTRATE_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x05;
        orderData[3] = (byte) year;
        orderData[4] = (byte) month;
        orderData[5] = (byte) day;
        orderData[6] = (byte) hour;
        orderData[7] = (byte) minute;
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
            case HEADER_HEART_RATE_COUNT:
                isCountSuccess = true;
                byte[] count = new byte[2];
                System.arraycopy(value, 2, count, 0, 2);
                heartRateCount = DigitalConver.byteArr2Int(count);
                MokoSupport.getInstance().setHeartRatesCount(heartRateCount);
                LogModule.i("有" + heartRateCount + "条心率数据");
                MokoSupport.getInstance().initHeartRatesList();
                heartRatesMap = MokoSupport.getInstance().getHeartRatesMap();
                heartRates = MokoSupport.getInstance().getHeartRates();
                if (heartRateCount != 0) {
                    // 拿到条数后再启动超时任务
                    heartRatesMap.put(heartRateCount, false);
                    MokoSupport.getInstance().setHeartRatesMap(heartRatesMap);
                    MokoSupport.getInstance().timeoutHandler(this);
                }
                break;
            case HEADER_HEART_RATE:
                if (heartRateCount > 0) {
                    if (value.length <= 2 || orderStatus == OrderTask.ORDER_STATUS_SUCCESS)
                        return;
                    if (heartRates == null) {
                        heartRates = new ArrayList<>();
                    }
                    if (heartRatesMap == null) {
                        heartRatesMap = new HashMap<>();
                    }
                    heartRatesMap.put(heartRateCount, true);
                    ComplexDataParse.parseHeartRate(value, heartRates);
                    heartRateCount--;

                    MokoSupport.getInstance().setHeartRatesCount(heartRateCount);
                    MokoSupport.getInstance().setHeartRates(heartRates);
                    if (heartRateCount > 0) {
                        LogModule.i("还有" + heartRateCount + "条心率数据未同步");
                        heartRatesMap.put(heartRateCount, false);
                        MokoSupport.getInstance().setHeartRatesMap(heartRatesMap);
                        orderTimeoutHandler(heartRateCount);
                        return;
                    }
                }
                break;
            default:
                return;
        }
        if (heartRateCount != 0) {
            return;
        }
        // 对心率数据做判重处理，避免时间重复造成的数据问题
        HashMap<String, HeartRate> removeRepeatMap = new HashMap<>();
        for (HeartRate heartRate : heartRates) {
            removeRepeatMap.put(heartRate.time, heartRate);
        }
        if (heartRates.size() != removeRepeatMap.size()) {
            heartRates.clear();
            heartRates.addAll(removeRepeatMap.values());
        }
        MokoSupport.getInstance().setHeartRatesCount(heartRateCount);
        MokoSupport.getInstance().setHeartRates(heartRates);
        MokoSupport.getInstance().setHeartRatesMap(heartRatesMap);
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }

    private void orderTimeoutHandler(final int heartRateCount) {
        MokoSupport.getInstance().getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (heartRatesMap != null
                        && !heartRatesMap.isEmpty()
                        && heartRatesMap.get(heartRateCount) != null
                        && !heartRatesMap.get(heartRateCount)) {
                    orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
                    LogModule.i("获取心率第" + heartRateCount + "个数据超时");
                    MokoSupport.getInstance().pollTask();
                    callback.onOrderTimeout(response);
                    MokoSupport.getInstance().executeTask(callback);
                }
            }
        }, delayTime);
    }

    @Override
    public boolean timeoutPreTask() {
        if (!isCountSuccess) {
            LogModule.i(order.getOrderName() + "个数超时");
        } else {
            return false;
        }
        return super.timeoutPreTask();
    }
}
