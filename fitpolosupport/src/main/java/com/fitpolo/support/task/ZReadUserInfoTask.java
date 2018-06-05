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
 * @Description 读取个人信息
 * @ClassPath com.fitpolo.support.task.ZReadUserInfoTask
 */
public class ZReadUserInfoTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    private byte[] orderData;

    public ZReadUserInfoTask(MokoOrderTaskCallback callback) {
        super(OrderType.READ_CHARACTER, OrderEnum.Z_READ_USER_INFO, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        UserInfo userInfo = new UserInfo();

        if (0x05 == DigitalConver.byte2Int(value[2])) {
            userInfo.weight = DigitalConver.byte2Int(value[3]);
            userInfo.height = DigitalConver.byte2Int(value[4]);
            userInfo.age = DigitalConver.byte2Int(value[5]);
            userInfo.gender = DigitalConver.byte2Int(value[6]);
            userInfo.stepExtent = DigitalConver.byte2Int(value[7]);
        }
        if (0x07 == DigitalConver.byte2Int(value[2])) {
            userInfo.weight = DigitalConver.byte2Int(value[3]);
            userInfo.height = DigitalConver.byte2Int(value[4]);
            userInfo.age = DigitalConver.byte2Int(value[5]);
            userInfo.birthdayMonth = DigitalConver.byte2Int(value[6]);
            userInfo.birthdayDay = DigitalConver.byte2Int(value[7]);
            userInfo.gender = DigitalConver.byte2Int(value[8]);
            userInfo.stepExtent = DigitalConver.byte2Int(value[9]);
        }



        MokoSupport.getInstance().setUserInfo(userInfo);

        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
