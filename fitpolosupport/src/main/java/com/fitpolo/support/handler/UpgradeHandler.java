package com.fitpolo.support.handler;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoConnStateCallback;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.CRCVerifyResponse;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderTaskResponse;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class UpgradeHandler implements MokoConnStateCallback, MokoOrderTaskCallback {
    public static final int EXCEPTION_FILEPATH_IS_NULL = 0x01;
    public static final int EXCEPTION_DEVICE_MAC_ADDRESS_IS_NULL = 0x02;
    public static final int EXCEPTION_UPGRADE_FAILURE = 0x03;
    // 返回数据包头
    public static final int RESPONSE_HEADER_PACKAGE = 0xA6;
    // 返回数据包结果
    public static final int RESPONSE_HEADER_PACKAGE_RESULT = 0xA7;
    private InputStream in;
    private Context context;
    private boolean isConnSuccess;
    private boolean isUpgradeDone;
    private boolean isStop;
    private String mFilePath;
    private String mDeviceMacAddress;
    private IUpgradeCallback mCallback;


    public UpgradeHandler(Context context) {
        this.context = context;
    }

    public void setFilePath(String filePath, String deviceMacAddress, IUpgradeCallback callback) {
        this.mCallback = callback;
        if (TextUtils.isEmpty(filePath)) {
            callback.onUpgradeError(EXCEPTION_FILEPATH_IS_NULL);
            return;
        }
        this.mFilePath = filePath;
        if (TextUtils.isEmpty(deviceMacAddress)) {
            callback.onUpgradeError(EXCEPTION_DEVICE_MAC_ADDRESS_IS_NULL);
            return;
        }
        this.mDeviceMacAddress = deviceMacAddress;
        // first:disConnect
        MokoSupport.getInstance().setReconnectCount(0);
        MokoSupport.getInstance().disConnectBle();
        new Handler(context.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                MokoSupport.getInstance().connDevice(context, mDeviceMacAddress, UpgradeHandler.this);
            }
        }, 4000);
    }

    ///////////////////////////////////////////////////////////////////////////
    // connect callback
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onConnectSuccess() {
        isConnSuccess = true;
        final File file = new File(mFilePath);
        new Handler(context.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    getCRCVerifyResult(CalcCrc16(mFilePath), (int) file.length());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 1500);
    }

    @Override
    public void onDisConnected() {
        onDeviceDisconnected();
    }

    @Override
    public void onConnTimeout(int reConnCount) {

    }

    private void onDeviceDisconnected() {
        if (!isConnSuccess) {
            if (isConnDevice()) {
                MokoSupport.getInstance().setReconnectCount(0);
                MokoSupport.getInstance().disConnectBle();
            }
        }
        if (isUpgradeDone) {
            return;
        }
        if (MokoSupport.getInstance().isBluetoothOpen() && MokoSupport.getInstance().getReconnectCount() > 0) {
            return;
        }
        onUpgradeFailure();
    }

    private void onUpgradeFailure() {
        isStop = true;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mCallback.onUpgradeError(EXCEPTION_UPGRADE_FAILURE);
            }
        });
    }


    public void getCRCVerifyResult(int fileCRCResult, int fileLengthResult) {
        CRCVerifyTask task = new CRCVerifyTask(this, fileCRCResult, fileLengthResult);
        MokoSupport.getInstance().sendOrder(task);
    }

    public void upgradeBand(byte[] packageIndex, byte[] fileBytes) {
        UpgradeBandTask task = new UpgradeBandTask(this, packageIndex, fileBytes);
        MokoSupport.getInstance().sendUpgradeOrder(task);
    }

    ///////////////////////////////////////////////////////////////////////////
    // order callback
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onOrderResult(OrderTaskResponse response) {
        OrderEnum orderEnum = response.order;
        switch (orderEnum) {
            case getCRCVerifyResult:
                CRCVerifyResponse crcResponse = (CRCVerifyResponse) response;
                if (crcResponse.header == RESPONSE_HEADER_PACKAGE) {
                    if (DigitalConver.byteArr2Int(crcResponse.packageResult) == 0) {
                        LogModule.i("upgrade start！");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int i = 0;
                                try {
                                    final File file = new File(mFilePath);
                                    if (in == null) {
                                        in = new FileInputStream(file);
                                    }
                                    while (in.available() > 0 && !isStop) {
                                        byte[] indexByte = DigitalConver.int2ByteArr(i, 2);
                                        byte b[] = new byte[17];
                                        in.read(b);
                                        upgradeBand(indexByte, b);
                                        i++;
                                        if (in == null) {
                                            return;
                                        }
                                        int unread = in.available();
                                        long length = file.length();
                                        int read = (int) (length - unread);
                                        final int percent = (int) (((float) read / (float) length) * 100);
                                        new Handler(context.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mCallback.onProgress(percent);
                                            }
                                        });
                                        Thread.sleep(20);
                                    }
                                    in.close();
                                    in = null;
                                    new Handler(context.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isStop && !isUpgradeDone) {
                                                LogModule.i("device error！");
                                                onUpgradeFailure();
                                            }
                                        }
                                    }, 5000);
                                } catch (Exception e) {
                                    onUpgradeFailure();
                                }
                            }
                        }).start();
                    }
                } else if (crcResponse.header == RESPONSE_HEADER_PACKAGE_RESULT) {
                    switch (crcResponse.ack) {
                        case 0:
                            LogModule.i("upgrade success！");
                            new Handler(context.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mCallback.onUpgradeDone();
                                }
                            });
                            MokoSupport.canUpgrade = false;
                            isUpgradeDone = true;
                            MokoSupport.getInstance().setReconnectCount(0);
                            MokoSupport.getInstance().disConnectBle();
                            isStop = true;
                            break;
                        case 1:
                            LogModule.i("upgrade timeout！");
                            onUpgradeFailure();
                            break;
                        case 2:
                            LogModule.i("upgrade verify error！");
                            onUpgradeFailure();
                            break;
                        case 3:
                            LogModule.i("upgrade file error！");
                            onUpgradeFailure();
                            break;
                        default:
                            LogModule.i("unknow error！");
                            onUpgradeFailure();
                            break;
                    }
                }
                break;
        }
    }

    @Override
    public void onOrderTimeout(OrderTaskResponse response) {

    }

    @Override
    public void onOrderFinish() {

    }

    ///////////////////////////////////////////////////////////////////////////
    // 
    ///////////////////////////////////////////////////////////////////////////
    public int CalcCrc16(String filePath) throws Exception {
        File file = new File(filePath);
        InputStream in = new FileInputStream(file);
        byte[] pchMsg = new byte[512 * 1024];
        int wDataLen = in.available();
        in.read(pchMsg);
        int crc = 0xffff;
        int c;
        for (int i = 0; i < wDataLen; i++) {
            c = pchMsg[i] & 0x00FF;
            crc ^= c;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        crc = (crc >> 8) + (crc << 8);
        in.close();
        return (crc);
    }

    public boolean isConnDevice() {
        return MokoSupport.getInstance().isConnDevice(context, mDeviceMacAddress);
    }

    public interface IUpgradeCallback {
        void onUpgradeError(int errorCode);

        void onProgress(int progress);

        void onUpgradeDone();
    }

    public void setStop(boolean stop) {
        isStop = stop;
    }
}

