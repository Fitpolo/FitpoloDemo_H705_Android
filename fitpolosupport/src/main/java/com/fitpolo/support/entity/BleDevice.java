package com.fitpolo.support.entity;

import java.io.Serializable;

/**
 * @Date 2017/5/14 0014
 * @Author wenzheng.liu
 * @Description 蓝牙设备
 * @ClassPath com.fitpolo.support.entity.BleDevice
 */
public class BleDevice implements Serializable, Comparable<BleDevice> {

    private static final long serialVersionUID = 1L;
    public String address;
    public String name;
    public int rssi;
    public String verifyCode;
    public byte[] scanRecord;

    @Override
    public int compareTo(BleDevice another) {
        if (this.rssi > another.rssi) {
            return -1;
        } else if (this.rssi < another.rssi) {
            return 1;
        }
        return 0;
    }
}
