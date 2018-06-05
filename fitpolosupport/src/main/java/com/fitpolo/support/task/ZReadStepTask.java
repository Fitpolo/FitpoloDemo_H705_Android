package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.DailyStep;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.ComplexDataParse;
import com.fitpolo.support.utils.DigitalConver;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 读取记步
 * @ClassPath com.fitpolo.support.task.ZReadStepTask
 */
public class ZReadStepTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 8;
    private static final int HEADER_STEP_COUNT = 0x01;
    private static final int HEADER_STEP = 0x02;

    private byte[] orderData;

    private int stepCount;
    private ArrayList<DailyStep> dailySteps;

    private boolean isCountSuccess;
    private boolean isReceiveDetail;

    public ZReadStepTask(MokoOrderTaskCallback callback, Calendar lastSyncTime) {
        super(OrderType.STEP_CHARACTER, OrderEnum.Z_READ_STEPS, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);

        int year = lastSyncTime.get(Calendar.YEAR) - 2000;
        int month = lastSyncTime.get(Calendar.MONTH) + 1;
        int day = lastSyncTime.get(Calendar.DAY_OF_MONTH);

        int hour = lastSyncTime.get(Calendar.HOUR_OF_DAY);
        int minute = lastSyncTime.get(Calendar.MINUTE);

        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_SETP_SEND;
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
            case HEADER_STEP_COUNT:
                if (data_length != 2) {
                    return;
                }
                isCountSuccess = true;
                byte[] count = new byte[2];
                System.arraycopy(value, 3, count, 0, 2);
                stepCount = DigitalConver.byteArr2Int(count);
                MokoSupport.getInstance().setDailyStepCount(stepCount);
                LogModule.i("有" + stepCount + "条记步数据");
                MokoSupport.getInstance().initStepsList();
                dailySteps = MokoSupport.getInstance().getDailySteps();
                delayTime = stepCount == 0 ? DEFAULT_DELAY_TIME : DEFAULT_DELAY_TIME + 100 * stepCount;
                // 拿到条数后再启动超时任务
                MokoSupport.getInstance().timeoutHandler(this);
                break;
            case HEADER_STEP:
                if (data_length != 14) {
                    return;
                }
                if (stepCount > 0) {
                    if (dailySteps == null) {
                        dailySteps = new ArrayList<>();
                    }
                    dailySteps.add(ComplexDataParse.parseDailyStep(value, 4));
                    stepCount--;
                    MokoSupport.getInstance().setDailySteps(dailySteps);
                    MokoSupport.getInstance().setDailyStepCount(stepCount);
                    if (stepCount > 0) {
                        LogModule.i("还有" + stepCount + "条记步数据未同步");
                        return;
                    }
                }
                break;
            default:
                return;
        }
        if (stepCount != 0) {
            return;
        }
        MokoSupport.getInstance().setDailyStepCount(stepCount);
        MokoSupport.getInstance().setDailySteps(dailySteps);
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
        return super.timeoutPreTask();
    }
}
