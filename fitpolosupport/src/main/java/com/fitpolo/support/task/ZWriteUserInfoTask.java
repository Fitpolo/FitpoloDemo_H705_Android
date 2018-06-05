package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.entity.UserInfo;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 设置个人信息
 * @ClassPath com.fitpolo.support.task.ZWriteUserInfoTask
 */
public class ZWriteUserInfoTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 10;

    private byte[] orderData;

    public ZWriteUserInfoTask(MokoOrderTaskCallback callback, UserInfo userInfo) {
        super(OrderType.WRITE_CHARACTER, OrderEnum.Z_WRITE_USER_INFO, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_WRITE_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x07;
        orderData[3] = (byte) userInfo.weight;
        orderData[4] = (byte) userInfo.height;
        orderData[5] = (byte) userInfo.age;
        orderData[6] = (byte) userInfo.birthdayMonth;
        orderData[7] = (byte) userInfo.birthdayDay;
        orderData[8] = (byte) userInfo.gender;
        orderData[9] = (byte) userInfo.stepExtent;
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
