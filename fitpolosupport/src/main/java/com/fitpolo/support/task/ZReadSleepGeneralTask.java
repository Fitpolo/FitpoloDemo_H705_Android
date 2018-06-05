package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.DailySleep;
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
 * @Description 读取睡眠概况
 * @ClassPath com.fitpolo.support.task.ZReadSleepGeneralTask
 */
public class ZReadSleepGeneralTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 8;
    private static final int HEADER_SLEEP_GENERAL_COUNT = 0x12;
    private static final int HEADER_SLEEP_GENERAL = 0x13;
    private static final int HEADER_SLEEP_DETAIL_COUNT = 0x14;
    private static final int HEADER_SLEEP_DETAIL = 0x15;

    private byte[] orderData;

    private HashMap<Integer, DailySleep> sleepsMap;
    private ArrayList<DailySleep> dailySleeps;
    private int sleepGeneralCount;
    private int sleepDetailCount;

    private boolean isCountSuccess;
    private boolean isReceiveDetail;
    private Calendar lastSyncTime;// yyyy-MM-dd HH:mm

    public ZReadSleepGeneralTask(MokoOrderTaskCallback callback, Calendar lastSyncTime) {
        super(OrderType.READ_CHARACTER, OrderEnum.Z_READ_SLEEP_GENERAL, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        this.lastSyncTime = lastSyncTime;

        int year = lastSyncTime.get(Calendar.YEAR) - 2000;
        int month = lastSyncTime.get(Calendar.MONTH) + 1;
        int day = lastSyncTime.get(Calendar.DAY_OF_MONTH);

        int hour = lastSyncTime.get(Calendar.HOUR_OF_DAY);
        int minute = lastSyncTime.get(Calendar.MINUTE);

        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_READ_SEND;
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
        int data_length = DigitalConver.byte2Int(value[2]);
        LogModule.i(order.getOrderName() + "成功");
        switch (header) {
            case HEADER_SLEEP_GENERAL_COUNT:
                if (data_length != 2) {
                    return;
                }
                isCountSuccess = true;
                byte[] count = new byte[2];
                System.arraycopy(value, 3, count, 0, 2);
                sleepGeneralCount = DigitalConver.byteArr2Int(count);
                MokoSupport.getInstance().setSleepIndexCount(sleepGeneralCount);
                MokoSupport.getInstance().setSleepRecordCount(sleepGeneralCount * 2);
                LogModule.i("有" + sleepGeneralCount + "条睡眠概况");
                MokoSupport.getInstance().initSleepIndexList();
                sleepsMap = MokoSupport.getInstance().getSleepsMap();
                dailySleeps = MokoSupport.getInstance().getDailySleeps();
                delayTime = sleepGeneralCount == 0 ? DEFAULT_DELAY_TIME : DEFAULT_DELAY_TIME + 100 * (sleepGeneralCount + sleepGeneralCount * 2);
                // 拿到条数后再启动超时任务
                MokoSupport.getInstance().timeoutHandler(this);
                break;
            case HEADER_SLEEP_GENERAL:
                if (data_length != 0x11) {
                    return;
                }
                if (sleepGeneralCount > 0) {
                    if (dailySleeps == null) {
                        dailySleeps = new ArrayList<>();
                    }
                    if (sleepsMap == null) {
                        sleepsMap = new HashMap<>();
                    }
                    dailySleeps.add(ComplexDataParse.parseDailySleepIndex(value, sleepsMap, 3));
                    sleepGeneralCount--;

                    MokoSupport.getInstance().setDailySleeps(dailySleeps);
                    MokoSupport.getInstance().setSleepIndexCount(sleepGeneralCount);
                    MokoSupport.getInstance().setSleepsMap(sleepsMap);
                    if (sleepGeneralCount > 0) {
                        LogModule.i("还有" + sleepGeneralCount + "条睡眠概况数据未同步");
                        return;
                    }
                }
                break;
            default:
                return;
        }
        if (!dailySleeps.isEmpty()) {
            // 请求完index后请求record
            ZReadSleepDetailTask sleepDetailTask = new ZReadSleepDetailTask(callback, lastSyncTime);
            MokoSupport.getInstance().sendCustomOrder(sleepDetailTask);
        } else {
            if (sleepGeneralCount != 0) {
                return;
            }
            MokoSupport.getInstance().setSleepIndexCount(sleepGeneralCount);
            MokoSupport.getInstance().setDailySleeps(dailySleeps);
            MokoSupport.getInstance().setSleepsMap(sleepsMap);

            orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
            MokoSupport.getInstance().pollTask();
            callback.onOrderResult(response);
            MokoSupport.getInstance().executeTask(callback);
        }
    }

    public void parseRecordValue(byte[] value) {
        int header = DigitalConver.byte2Int(value[1]);
        int data_length = DigitalConver.byte2Int(value[2]);
        LogModule.i("读取未同步的睡眠详情数据成功");
        switch (header) {
            case HEADER_SLEEP_DETAIL_COUNT:
                if (data_length != 2) {
                    return;
                }
                byte[] count = new byte[2];
                System.arraycopy(value, 3, count, 0, 2);
                sleepDetailCount = DigitalConver.byteArr2Int(count);
                LogModule.i("有" + sleepDetailCount + "条睡眠详情");
                MokoSupport.getInstance().setSleepRecordCount(sleepDetailCount);
                break;
            case HEADER_SLEEP_DETAIL:
                if (data_length != (value.length - 3)) {
                    return;
                }
                if (sleepDetailCount > 0) {
                    ComplexDataParse.parseDailySleepRecord(value, sleepsMap, 3);
                    sleepDetailCount--;
                    MokoSupport.getInstance().setSleepRecordCount(sleepDetailCount);
                    if (sleepDetailCount > 0) {
                        LogModule.i("还有" + sleepDetailCount + "条睡眠详情数据未同步");
                        return;
                    }
                }
                break;
            default:
                return;
        }
        if (sleepDetailCount != 0) {
            return;
        }
        MokoSupport.getInstance().setSleepRecordCount(sleepDetailCount);
        MokoSupport.getInstance().setDailySleeps(dailySleeps);
        sleepsMap.clear();
        MokoSupport.getInstance().setSleepsMap(sleepsMap);
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }

    @Override
    public boolean timeoutPreTask() {
        if (!isReceiveDetail) {
            if (!isCountSuccess) {
                LogModule.i(order.getOrderName() + "个数超时");
            } else {
                isReceiveDetail = true;
                return false;
            }
        }
        if (sleepsMap != null) {
            sleepsMap.clear();
            MokoSupport.getInstance().setSleepsMap(sleepsMap);
        }
        return super.timeoutPreTask();
    }
}
