package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.FirmwareParams;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description 读取硬件参数
 * @ClassPath com.fitpolo.support.task.ZReadParamsTask
 */
public class ZReadParamsTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 3;

    private byte[] orderData;

    public ZReadParamsTask(MokoOrderTaskCallback callback) {
        super(OrderType.READ_CHARACTER, OrderEnum.Z_READ_PARAMS, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
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
        if (0x08 != DigitalConver.byte2Int(value[2])) {
            return;
        }
        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        FirmwareParams params = new FirmwareParams();
        params.test = DigitalConver.byte2binaryString(value[3]);
        byte[] reflectiveThreshold = new byte[2];
        System.arraycopy(value, 4, reflectiveThreshold, 0, 2);
        params.reflectiveThreshold = DigitalConver.byteArr2Int(reflectiveThreshold);
        byte[] reflectiveValue = new byte[2];
        System.arraycopy(value, 6, reflectiveValue, 0, 2);
        params.reflectiveValue = DigitalConver.byteArr2Int(reflectiveValue);
        params.batchYear = 2000 + DigitalConver.byte2Int(value[8]);
        params.batchWeek = DigitalConver.byte2Int(value[9]);
        params.speedUnit = DigitalConver.byte2Int(value[10]);

        LogModule.i("flash状态：" + params.test.substring(7, 8));
        LogModule.i("G sensor状态：" + params.test.substring(6, 7));
        LogModule.i("hr状态：" + params.test.substring(5, 6));
        LogModule.i("当前反光阈值：" + params.reflectiveThreshold);
        LogModule.i("当前反光值：" + params.reflectiveValue);
        LogModule.i("生产批次年：" + params.batchYear);
        LogModule.i("生产批次周：" + params.batchWeek);
        LogModule.i("蓝牙连接配速单位：" + params.speedUnit);

        MokoSupport.getInstance().setProductBatch(String.format("%d.%d", params.batchYear, params.batchWeek));
        MokoSupport.getInstance().setParams(params);

        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
