package com.fitpolo.support.entity;

import android.bluetooth.BluetoothGattCharacteristic;

import com.fitpolo.support.utils.MokoUtils;

import java.io.Serializable;

public class MokoCharacteristic implements Serializable {
    public BluetoothGattCharacteristic characteristic;
    public String charPropertie;
    public OrderType orderType;

    public MokoCharacteristic(BluetoothGattCharacteristic characteristic, String charPropertie, OrderType orderType) {
        this.characteristic = characteristic;
        this.charPropertie = charPropertie;
        this.orderType = orderType;
    }

    public MokoCharacteristic(BluetoothGattCharacteristic characteristic, OrderType orderType) {
        this(characteristic, MokoUtils.getCharPropertie(characteristic.getProperties()), orderType);
    }
}
