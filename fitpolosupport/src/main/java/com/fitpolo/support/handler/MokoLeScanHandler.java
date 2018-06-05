package com.fitpolo.support.handler;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.util.SparseArray;

import com.fitpolo.support.callback.MokoScanDeviceCallback;
import com.fitpolo.support.entity.BleDevice;
import com.fitpolo.support.utils.DigitalConver;

import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

/**
 * @Date 2017/12/12 0012
 * @Author wenzheng.liu
 * @Description 搜索设备回调类
 * @ClassPath com.moko.support.handler.MokoLeScanHandler
 */
public class MokoLeScanHandler extends ScanCallback {
    private MokoScanDeviceCallback callback;

    public MokoLeScanHandler(MokoScanDeviceCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        if (result != null) {
            BluetoothDevice device = result.getDevice();
            byte[] scanRecord = result.getScanRecord().getBytes();
            int rssi = result.getRssi();
            if (TextUtils.isEmpty(device.getName()) || scanRecord.length == 0 || rssi < -127 || rssi == 127) {
                return;
            }
            SparseArray<byte[]> arrays = result.getScanRecord().getManufacturerSpecificData();
            if (arrays == null || arrays.size() == 0) {
                return;
            }
            byte[] manufacturerSpecificData = result.getScanRecord().getManufacturerSpecificData(arrays.keyAt(0));
            if (manufacturerSpecificData.length < 7) {
                return;
            }
            String verifyCode = DigitalConver.byte2HexString(manufacturerSpecificData[2])
                    + DigitalConver.byte2HexString(manufacturerSpecificData[3])
                    + DigitalConver.byte2HexString(manufacturerSpecificData[4]);
            BleDevice bleDevice = new BleDevice();
            bleDevice.name = device.getName();
            bleDevice.address = device.getAddress();
            bleDevice.rssi = rssi;
            bleDevice.scanRecord = scanRecord;
            bleDevice.verifyCode = verifyCode;
            callback.onScanDevice(bleDevice);
        }
    }
}