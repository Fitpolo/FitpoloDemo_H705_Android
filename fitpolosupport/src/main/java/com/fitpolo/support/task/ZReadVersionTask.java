package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.FirmwareEnum;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 读取版本
 * @ClassPath com.fitpolo.support.task.ZReadVersionTask
 */
public class ZReadVersionTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    private byte[] orderData;

    public ZReadVersionTask(MokoOrderTaskCallback callback) {
        super(OrderType.READ_CHARACTER, OrderEnum.Z_READ_VERSION, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        if (0x06 != DigitalConver.byte2Int(value[2])) {
            return;
        }
        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        byte[] preCode = new byte[2];
        System.arraycopy(value, 3, preCode, 0, 2);
        byte[] middleCode = new byte[2];
        System.arraycopy(value, 5, middleCode, 0, 2);
        byte[] lastCode = new byte[2];
        System.arraycopy(value, 7, lastCode, 0, 2);

        String versionStr = DigitalConver.bytesToHexString(preCode) + "." + DigitalConver.bytesToHexString(middleCode) + "." + DigitalConver.bytesToHexString(lastCode);
        // 内部版本，判断升级用
        MokoSupport.versionCode = versionStr;
        // 外部版本，客户用
        MokoSupport.versionCodeShow = versionStr;
        // 大版本号，区分升级固件
        MokoSupport.firmwareEnum = FirmwareEnum.fromHeader(DigitalConver.bytesToHexString(preCode));
        if (MokoSupport.firmwareEnum == null) {
            return;
        }
        // 小版本号，判断部分功能有无
        MokoSupport.versionCodeLast = DigitalConver.byteArr2Int(lastCode);
        LogModule.i("版本号末尾：" + MokoSupport.versionCodeLast);
        // 判断是否升级
        MokoSupport.canUpgrade = MokoSupport.versionCodeLast < MokoSupport.firmwareEnum.getLastestVersion();

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
