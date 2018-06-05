package com.fitpolo.support.handler;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.text.TextUtils;

import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.entity.MokoCharacteristic;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;

import java.util.HashMap;
import java.util.List;

public class MokoCharacteristicHandler {
    private static MokoCharacteristicHandler INSTANCE;

    public static final String SERVICE_UUID_HEADER_NEW = "0000ffb0";

    public HashMap<OrderType, MokoCharacteristic> mokoCharacteristicMap;

    private MokoCharacteristicHandler() {
        //no instance
        mokoCharacteristicMap = new HashMap<>();
    }

    public static MokoCharacteristicHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (MokoCharacteristicHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MokoCharacteristicHandler();
                }
            }
        }
        return INSTANCE;
    }

    public HashMap<OrderType, MokoCharacteristic> getCharacteristics(BluetoothGatt gatt) {
        if (mokoCharacteristicMap != null && !mokoCharacteristicMap.isEmpty()) {
            mokoCharacteristicMap.clear();
        }
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
            String serviceUuid = service.getUuid().toString();
            if (TextUtils.isEmpty(serviceUuid)) {
                continue;
            }
            if (serviceUuid.startsWith("00001800") || serviceUuid.startsWith("00001801")) {
                continue;
            }
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            if (service.getUuid().toString().startsWith(SERVICE_UUID_HEADER_NEW)) {
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    String characteristicUuid = characteristic.getUuid().toString();
                    if (TextUtils.isEmpty(characteristicUuid)) {
                        continue;
                    }
                    if (characteristicUuid.equals(OrderType.READ_CHARACTER.getUuid())) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        mokoCharacteristicMap.put(OrderType.READ_CHARACTER, new MokoCharacteristic(characteristic, OrderType.READ_CHARACTER));
                        continue;
                    }
                    if (characteristicUuid.equals(OrderType.WRITE_CHARACTER.getUuid())) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        mokoCharacteristicMap.put(OrderType.WRITE_CHARACTER, new MokoCharacteristic(characteristic, OrderType.WRITE_CHARACTER));
                        continue;
                    }
                    if (characteristicUuid.equals(OrderType.STEP_CHARACTER.getUuid())) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        mokoCharacteristicMap.put(OrderType.STEP_CHARACTER, new MokoCharacteristic(characteristic, OrderType.STEP_CHARACTER));
                        continue;
                    }
                    if (characteristicUuid.equals(OrderType.HEART_RATE_CHARACTER.getUuid())) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        mokoCharacteristicMap.put(OrderType.HEART_RATE_CHARACTER, new MokoCharacteristic(characteristic, OrderType.HEART_RATE_CHARACTER));
                        continue;
                    }
                }
            }
        }
        return mokoCharacteristicMap;
    }
}
