package com.fitpolo.support.utils;

import android.bluetooth.BluetoothGattCharacteristic;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Date 2017/12/5 0005
 * @Author wenzheng.liu
 * @Description fitpolo工具类
 * @ClassPath com.fitpolo.support.utils.MokoUtils
 */
public class MokoUtils {

    private static HashMap<Integer, String> charProperties = new HashMap();

    static {
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_BROADCAST, "BROADCAST");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS, "EXTENDED_PROPS");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_INDICATE, "INDICATE");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_NOTIFY, "NOTIFY");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_READ, "READ");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE, "SIGNED_WRITE");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_WRITE, "WRITE");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, "WRITE_NO_RESPONSE");
    }

    public static String getCharPropertie(int property) {
        return getHashMapValue(charProperties, property);
    }

    private static String getHashMapValue(HashMap<Integer, String> hashMap, int number) {
        String result = hashMap.get(number);
        if (TextUtils.isEmpty(result)) {
            List<Integer> numbers = getElement(number);
            StringBuilder resultStr = new StringBuilder();
            for (int i = 0; i < numbers.size(); i++) {
                resultStr.append(hashMap.get(numbers.get(i)));
                if (i < numbers.size() - 1) {
                    resultStr.append("|");
                }
            }
            return resultStr.toString();
        }
        return result;
    }

    /**
     * 位运算结果的反推函数10 -> 2 | 8;
     */
    static private List<Integer> getElement(int number) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            int b = 1 << i;
            if ((number & b) > 0)
                result.add(b);
        }

        return result;
    }
}
