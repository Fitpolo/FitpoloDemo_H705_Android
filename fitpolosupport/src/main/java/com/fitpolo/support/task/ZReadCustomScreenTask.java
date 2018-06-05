package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.CustomScreen;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 读取自定义屏幕
 * @ClassPath com.fitpolo.support.task.ZReadCustomScreenTask
 */
public class ZReadCustomScreenTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    private byte[] orderData;

    public ZReadCustomScreenTask(MokoOrderTaskCallback callback) {
        super(OrderType.READ_CHARACTER, OrderEnum.Z_READ_CUSTOM_SCREEN, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        if (0x04 != DigitalConver.byte2Int(value[2])) {
            return;
        }

        byte[] data = new byte[3];
        System.arraycopy(value, 4, data, 0, 3);
        String binaryStr = DigitalConver.hexString2binaryString(DigitalConver.bytesToHexString(data));
        CustomScreen customScreen = new CustomScreen(
                str2Boolean(binaryStr, 16, 17),
                str2Boolean(binaryStr, 18, 19),
                str2Boolean(binaryStr, 17, 18),
                str2Boolean(binaryStr, 21, 22),
                str2Boolean(binaryStr, 19, 20),
                str2Boolean(binaryStr, 15, 16));
        MokoSupport.getInstance().setCustomScreen(customScreen);


        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }

    private boolean str2Boolean(String str, int start, int end) {
        return "1".equals(str.substring(start, end));
    }

}
