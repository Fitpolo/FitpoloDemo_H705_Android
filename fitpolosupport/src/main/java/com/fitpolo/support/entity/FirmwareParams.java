package com.fitpolo.support.entity;

/**
 * @Date 2018/4/10
 * @Author wenzheng.liu
 * @Description 硬件参数
 * @ClassPath com.fitpolo.support.entity.FirmwareParams
 */
public class FirmwareParams {
    public String test; // bit0:flash, bit1:G sensor,bit2: hr检测;
    public int reflectiveThreshold;// 反光阈值,默认1380;
    public int reflectiveValue;// 当前反光值;
    public int batchYear;// 生产批次年;
    public int batchWeek;// 生产批次周;
    public int speedUnit;// 蓝牙连接配速单位是1.25ms;

    @Override
    public String toString() {
        return "FirmwareParams{" +
                "test='" + test + '\'' +
                ", reflectiveThreshold=" + reflectiveThreshold +
                ", reflectiveValue=" + reflectiveValue +
                ", batchYear=" + batchYear +
                ", batchWeek=" + batchWeek +
                ", speedUnit=" + speedUnit +
                '}';
    }
}
