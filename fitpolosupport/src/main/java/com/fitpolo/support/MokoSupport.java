package com.fitpolo.support;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.text.TextUtils;

import com.fitpolo.support.callback.MokoConnStateCallback;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.callback.MokoResponseCallback;
import com.fitpolo.support.callback.MokoScanDeviceCallback;
import com.fitpolo.support.entity.AutoLighten;
import com.fitpolo.support.entity.BandAlarm;
import com.fitpolo.support.entity.BleDevice;
import com.fitpolo.support.entity.CustomScreen;
import com.fitpolo.support.entity.DailySleep;
import com.fitpolo.support.entity.DailyStep;
import com.fitpolo.support.entity.DeviceTypeEnum;
import com.fitpolo.support.entity.FirmwareEnum;
import com.fitpolo.support.entity.FirmwareParams;
import com.fitpolo.support.entity.HeartRate;
import com.fitpolo.support.entity.MokoCharacteristic;
import com.fitpolo.support.entity.NoDisturb;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.entity.SitAlert;
import com.fitpolo.support.entity.UserInfo;
import com.fitpolo.support.handler.MokoCharacteristicHandler;
import com.fitpolo.support.handler.MokoConnStateHandler;
import com.fitpolo.support.handler.MokoLeScanHandler;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.task.OpenNotifyTask;
import com.fitpolo.support.task.OrderTask;
import com.fitpolo.support.task.ZReadSleepGeneralTask;
import com.fitpolo.support.utils.BaseHandler;
import com.fitpolo.support.utils.BleConnectionCompat;
import com.fitpolo.support.utils.ComplexDataParse;
import com.fitpolo.support.utils.DigitalConver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

/**
 * @Date 2017/5/10
 * @Author wenzheng.liu
 * @Description 蓝牙工具类
 * @ClassPath com.fitpolo.support.MokoSupport
 */
public class MokoSupport implements MokoResponseCallback {
    public static final int HANDLER_MESSAGE_WHAT_CONNECTED = 1;
    public static final int HANDLER_MESSAGE_WHAT_DISCONNECTED = 2;
    public static final int HANDLER_MESSAGE_WHAT_SERVICES_DISCOVERED = 3;
    public static final int HANDLER_MESSAGE_WHAT_DISCONNECT = 4;
    public static final int HANDLER_MESSAGE_WHAT_RECONNECT = 5;
    // 扫描结束时间
    private static final long SCAN_PERIOD = 5000;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BlockingQueue<OrderTask> mQueue;

    private Context mContext;
    private MokoConnStateCallback mMokoConnStateCallback;
    private HashMap<OrderType, MokoCharacteristic> mCharacteristicMap;
    private static final UUID DESCRIPTOR_UUID_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID SERVICE_UUID = UUID.fromString("0000ffc0-0000-1000-8000-00805f9b34fb");

    ///////////////////////////////////////////////////////////////////////////
    // version
    ///////////////////////////////////////////////////////////////////////////
    public static boolean canUpgrade;
    public static int versionCodeLast;
    public static FirmwareEnum firmwareEnum;
    public static String versionCodeShow;
    public static String versionCode;
    ///////////////////////////////////////////////////////////////////////////
    // type
    ///////////////////////////////////////////////////////////////////////////
    public static DeviceTypeEnum deviceTypeEnum;

    private static volatile MokoSupport INSTANCE;

    private MokoSupport() {
        mQueue = new LinkedBlockingQueue<>();
        isOpenReConnect = true;
    }

    public static MokoSupport getInstance() {
        if (INSTANCE == null) {
            synchronized (MokoSupport.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MokoSupport();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * @Date 2017/5/10
     * @Author wenzheng.liu
     * @Description 创建蓝牙适配器
     */
    public void init(Context context) {
        LogModule.init(context);
        mContext = context;
        mHandler = new ServiceMessageHandler(this);
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    public void startScanDevice(final MokoScanDeviceCallback mokoScanDeviceCallback) {
        if (!isBluetoothOpen()) {
            mokoScanDeviceCallback.onStopScan();
            return;
        }
        LogModule.i("开始扫描");
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        List<ScanFilter> scanFilterList = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setServiceUuid(new ParcelUuid(SERVICE_UUID));
        scanFilterList.add(builder.build());
        final MokoLeScanHandler mokoLeScanHandler = new MokoLeScanHandler(mokoScanDeviceCallback);
        scanner.startScan(scanFilterList, settings, mokoLeScanHandler);
        mokoScanDeviceCallback.onStartScan();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanDevice(mokoLeScanHandler, mokoScanDeviceCallback);
            }
        }, SCAN_PERIOD);
    }

    private void stopScanDevice(MokoLeScanHandler mokoLeScanHandler, MokoScanDeviceCallback mokoScanDeviceCallback) {
        if (mokoLeScanHandler != null && mokoScanDeviceCallback != null) {
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(mokoLeScanHandler);
            mokoScanDeviceCallback.onStopScan();
        }
    }

    /**
     * @Date 2017/5/10
     * @Author wenzheng.liu
     * @Description 连接gatt
     */
    public synchronized void connDevice(final Context context, String address, final MokoConnStateCallback mokoConnStateCallback) {
        setConnStateCallback(mokoConnStateCallback);
        if (isReConnecting) {
            LogModule.i("正在重连中...");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            LogModule.i("connDevice: 地址为空");
            return;
        }
        if (!isBluetoothOpen()) {
            LogModule.i("connDevice: 蓝牙未打开");
            return;
        }
        if (isConnDevice(context, address)) {
            LogModule.i("connDevice: 设备已连接");
            return;
        }
        final MokoConnStateHandler gattCallback = MokoConnStateHandler.getInstance();
        gattCallback.setMokoResponseCallback(this);
        gattCallback.setMessageHandler(mHandler);
        mDeviceAddress = address;
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        reConnectCount = 2;
        if (device != null) {
            isReConnecting = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    LogModule.i("开始尝试连接");
                    mBluetoothGatt = (new BleConnectionCompat(context)).connectGatt(device, false, gattCallback);
                }
            });
        } else {
            LogModule.i("获取蓝牙设备失败");
        }
    }

    private void setConnStateCallback(final MokoConnStateCallback mokoConnStateCallback) {
        mHandler.setMokoConnStateCallback(mokoConnStateCallback);
        mMokoConnStateCallback = mokoConnStateCallback;
    }

    /**
     * @Date 2017/5/11
     * @Author wenzheng.liu
     * @Description 发送命令
     */
    public void sendOrder(OrderTask... orderTasks) {
        if (orderTasks.length == 0) {
            return;
        }
        if (!isSyncData()) {
            for (OrderTask orderTask : orderTasks) {
                if (orderTask == null) {
                    continue;
                }
                mQueue.offer(orderTask);
            }
            executeTask(null);
        } else {
            for (OrderTask orderTask : orderTasks) {
                if (orderTask == null) {
                    continue;
                }
                mQueue.offer(orderTask);
            }
        }
    }

    private String mDeviceAddress;
    private boolean isOpenReConnect;
    private boolean isReConnectLimited;
    private int reConnectCount;
    private boolean isReConnecting;

    /**
     * @param callback
     * @Date 2017/5/11
     * @Author wenzheng.liu
     * @Description 执行命令
     */
    public void executeTask(MokoOrderTaskCallback callback) {
        if (callback != null && !isSyncData()) {
            callback.onOrderFinish();
            return;
        }
        if (mQueue.isEmpty()) {
            return;
        }
        final OrderTask orderTask = mQueue.peek();
        if (mBluetoothGatt == null) {
            LogModule.i("executeTask : BluetoothGatt is null");
            return;
        }
        if (orderTask == null) {
            LogModule.i("executeTask : orderTask is null");
            return;
        }
        if (mCharacteristicMap == null || mCharacteristicMap.isEmpty()) {
            LogModule.i("executeTask : characteristicMap is null");
            disConnectBle();
            return;
        }
        final MokoCharacteristic mokoCharacteristic = mCharacteristicMap.get(orderTask.orderType);
        if (mokoCharacteristic == null) {
            LogModule.i("executeTask : mokoCharacteristic is null");
            return;
        }
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE) {
            sendWriteNoResponseOrder(orderTask, mokoCharacteristic);
            timeoutHandler(orderTask);
        }
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_NOTIFY) {
            sendNotifyOrder(orderTask, mokoCharacteristic);
        }
    }

    /**
     * @Date 2017/5/10
     * @Author wenzheng.liu
     * @Description 判断是否已连接手环
     */
    public boolean isConnDevice(Context context, String address) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        int connState = bluetoothManager.getConnectionState(mBluetoothAdapter.getRemoteDevice(address), BluetoothProfile.GATT);
        return connState == BluetoothProfile.STATE_CONNECTED;
    }

    /**
     * @Date 2017/6/22
     * @Author wenzheng.liu
     * @Description 正在同步
     */
    public synchronized boolean isSyncData() {
        return mQueue != null && !mQueue.isEmpty();
    }

    /**
     * @Date 2017/5/14 0014
     * @Author wenzheng.liu
     * @Description 设置重连
     */
    public void setOpenReConnect(boolean openReConnect) {
        LogModule.i(openReConnect ? "打开重连" : "关闭重连");
        isOpenReConnect = openReConnect;
    }

    /**
     * @Date 2018/3/3
     * @Author wenzheng.liu
     * @Description
     */
    public void setReConnectLimited(boolean reConnectLimited) {
        isReConnectLimited = reConnectLimited;
    }

    /**
     * @Date 2017/8/29
     * @Author wenzheng.liu
     * @Description 获取重连次数
     */
    public int getReconnectCount() {
        return reConnectCount;
    }

    /**
     * @Date 2017/8/29
     * @Author wenzheng.liu
     * @Description 设置重连次数
     */
    public void setReconnectCount(int reConnectCount) {
        this.reConnectCount = reConnectCount;
    }


    /**
     * @Date 2017/5/10
     * @Author wenzheng.liu
     * @Description 蓝牙是否开启
     */
    public boolean isBluetoothOpen() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    /**
     * @Date 2017/5/10
     * @Author wenzheng.liu
     * @Description 断开gattt
     */
    public void disConnectBle() {
        mHandler.sendEmptyMessage(HANDLER_MESSAGE_WHAT_DISCONNECT);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!isSyncData()) {
            OrderType orderType = null;
            if (characteristic.getUuid().toString().equals(OrderType.STEP_CHARACTER.getUuid())) {
                // 记步变化
                orderType = OrderType.STEP_CHARACTER;
            }
            if (orderType != null) {
                DailyStep dailyStep = ComplexDataParse.parseCurrentStep(value);
                if (dailyStep != null) {
                    setDailyStep(dailyStep);
                    Intent intent = new Intent(MokoConstants.ACTION_CURRENT_DATA);
                    intent.putExtra(MokoConstants.EXTRA_KEY_CURRENT_DATA_TYPE, OrderEnum.Z_STEPS_CHANGES_LISTENER);
                    mContext.sendBroadcast(intent);
                }
            }
            return;
        }
        // 非延时应答
        OrderTask orderTask = mQueue.peek();
        String characteristicUuid = characteristic.getUuid().toString();
        if (value != null && value.length > 0 && orderTask != null) {
            if (characteristicUuid.equals(OrderType.READ_CHARACTER) && MokoConstants.HEADER_READ_GET != (value[0] & 0xFF)) {
                return;
            }
            if (characteristicUuid.equals(OrderType.WRITE_CHARACTER) && MokoConstants.HEADER_WRITE_GET != (value[0] & 0xFF)) {
                return;
            }
            if (characteristicUuid.equals(OrderType.STEP_CHARACTER) && MokoConstants.HEADER_STEP_GET != (value[0] & 0xFF)) {
                return;
            }
            if (characteristicUuid.equals(OrderType.HEART_RATE_CHARACTER) && MokoConstants.HEADER_HEARTRATE_GET != (value[0] & 0xFF)) {
                return;
            }
            OrderEnum orderEnum = orderTask.getOrder();
            switch (orderEnum) {
                case Z_READ_SLEEP_GENERAL:
                    ZReadSleepGeneralTask zReadSleepGeneralTask = (ZReadSleepGeneralTask) orderTask;
                    if (mSleepIndexCount == 0 && mSleepsMap != null && !mSleepsMap.isEmpty()) {
                        zReadSleepGeneralTask.parseRecordValue(value);
                    } else {
                        zReadSleepGeneralTask.parseValue(value);
                    }
                    return;
            }
            orderTask.parseValue(value);
        }
    }

    @Override
    public void onCharacteristicWrite(byte[] value) {

    }

    @Override
    public void onCharacteristicRead(byte[] value) {

    }

    @Override
    public void onDescriptorWrite() {
        if (!isSyncData()) {
            return;
        }
        OrderTask orderTask = mQueue.peek();
        LogModule.i("device to app NOTIFY : " + orderTask.orderType.getName());
        LogModule.i(orderTask.order.getOrderName());
        orderTask.orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        mQueue.poll();
        executeTask(orderTask.callback);
        if (mQueue.isEmpty()) {
            mMokoConnStateCallback.onConnectSuccess();
            isReConnecting = false;
            reConnectCount = 2;
        }
    }

    public void pollTask() {
        mQueue.poll();
    }

    public void timeoutHandler(OrderTask orderTask) {
        mHandler.postDelayed(orderTask.timeoutRunner, orderTask.delayTime);
    }


    ///////////////////////////////////////////////////////////////////////////
    // handler
    ///////////////////////////////////////////////////////////////////////////
    private ServiceMessageHandler mHandler;

    public Handler getHandler() {
        return mHandler;
    }

    public class ServiceMessageHandler extends BaseHandler<MokoSupport> {
        private MokoConnStateCallback mokoConnStateCallback;

        public ServiceMessageHandler(MokoSupport module) {
            super(module);
        }

        @Override
        protected void handleMessage(MokoSupport module, Message msg) {
            switch (msg.what) {
                case HANDLER_MESSAGE_WHAT_CONNECTED:
                    mBluetoothGatt.discoverServices();
                    break;
                case HANDLER_MESSAGE_WHAT_DISCONNECTED:
                    disConnectBle();
                    isReConnecting = false;
                    break;
                case HANDLER_MESSAGE_WHAT_SERVICES_DISCOVERED:
                    LogModule.i("连接成功！");
                    mCharacteristicMap = MokoCharacteristicHandler.getInstance().getCharacteristics(mBluetoothGatt);
                    sendOrder(new OpenNotifyTask(OrderType.NOTIFY, OrderEnum.openNotify, null),
                            new OpenNotifyTask(OrderType.READ_CHARACTER, OrderEnum.READ_NOTIFY, null),
                            new OpenNotifyTask(OrderType.WRITE_CHARACTER, OrderEnum.WRITE_NOTIFY, null),
                            new OpenNotifyTask(OrderType.STEP_CHARACTER, OrderEnum.STEP_NOTIFY, null),
                            new OpenNotifyTask(OrderType.HEART_RATE_CHARACTER, OrderEnum.HEART_RATE_NOTIFY, null));
                    break;
                case HANDLER_MESSAGE_WHAT_DISCONNECT:
                    if (mQueue != null && !mQueue.isEmpty()) {
                        mQueue.clear();
                    }
                    if (mBluetoothGatt != null) {
                        if (refreshDeviceCache()) {
                            LogModule.i("清理GATT层蓝牙缓存");
                        }
                        LogModule.i("断开连接");
                        mBluetoothGatt.close();
                        mBluetoothGatt.disconnect();
                        if (reConnectCount > 0) {
                            mokoConnStateCallback.onDisConnected();
                            mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_WHAT_RECONNECT, 3000);
                        } else {
                            reConnectCount--;
                            LogModule.i("重连失败，需要用户手动连接");
                            isReConnecting = false;
                            mMokoConnStateCallback.onDisConnected();
                        }
                    }
                    break;
                case HANDLER_MESSAGE_WHAT_RECONNECT:
                    startReConnect();
                    break;
            }
        }

        public void setMokoConnStateCallback(MokoConnStateCallback mokoConnStateCallback) {
            this.mokoConnStateCallback = mokoConnStateCallback;
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    private ExecutorService mExecutorService;
    private ReConnRunnable mRunnableReconnect;

    private void startReConnect() {
        if (isOpenReConnect) {
            LogModule.e("开始重连...");
            mRunnableReconnect = new ReConnRunnable();
            mExecutorService.execute(mRunnableReconnect);
        }
    }

    private class ReConnRunnable implements Runnable {

        private ReConnRunnable() {
        }

        @Override
        public void run() {
            try {
                if (!isConnDevice(mContext, mDeviceAddress)) {
                    if (!isOpenReConnect) {
                        isReConnecting = false;
                        return;
                    }
                    isReConnecting = true;
                    if (isBluetoothOpen()) {
                        if (isReConnectLimited) {
                            reConnectCount--;
                            if (reConnectCount == 0) {
                                LogModule.i("提示重连超时...");
                                mMokoConnStateCallback.onConnTimeout(reConnectCount);
                            }
                        }
                        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
                        final MokoScanDeviceCallback mokoScanDeviceCallback = new MokoScanDeviceCallback() {
                            private String mReConnDeviceAddress;

                            @Override
                            public void onStartScan() {
                                mReConnDeviceAddress = "";
                                LogModule.i("重连开始扫描:" + reConnectCount);
                            }

                            @Override
                            public void onScanDevice(BleDevice device) {
                                if (mDeviceAddress.equals(device.address) && TextUtils.isEmpty(mReConnDeviceAddress)) {
                                    mReConnDeviceAddress = device.address;
                                    LogModule.i("扫描到设备，开始连接...");
                                    final MokoConnStateHandler gattCallback = MokoConnStateHandler.getInstance();
                                    gattCallback.setMokoResponseCallback(MokoSupport.this);
                                    setConnStateCallback(mMokoConnStateCallback);
                                    gattCallback.setMessageHandler(mHandler);
                                    final BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mReConnDeviceAddress);
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mBluetoothGatt = (new BleConnectionCompat(mContext)).connectGatt(bluetoothDevice, false, gattCallback);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onStopScan() {
                                if (TextUtils.isEmpty(mReConnDeviceAddress)) {
                                    LogModule.i("未扫描到设备...");
                                    if (reConnectCount > 0) {
                                        mExecutorService.execute(mRunnableReconnect);
                                    } else {
                                        mReConnDeviceAddress = "";
                                        reConnectCount--;
                                        LogModule.i("重连失败，需要用户手动连接");
                                        isReConnecting = false;
                                        mMokoConnStateCallback.onDisConnected();
                                    }
                                }
                            }
                        };
                        final MokoLeScanHandler mokoLeScanHandler = new MokoLeScanHandler(mokoScanDeviceCallback);
                        ScanSettings settings = new ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                .build();
                        List<ScanFilter> scanFilterList = new ArrayList<>();
                        ScanFilter.Builder builder = new ScanFilter.Builder();
                        builder.setServiceUuid(new ParcelUuid(SERVICE_UUID));
                        scanFilterList.add(builder.build());
                        scanner.startScan(scanFilterList, settings, mokoLeScanHandler);
                        mokoScanDeviceCallback.onStartScan();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                stopScanDevice(mokoLeScanHandler, mokoScanDeviceCallback);
                            }
                        }, 10000);
                    } else {
                        LogModule.i("蓝牙未开启...");
                        Thread.sleep(5000);
                        mExecutorService.execute(mRunnableReconnect);
                    }
                } else {
                    isReConnecting = false;
                    LogModule.i("设备已连接...");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 发送可监听命令
    private void sendNotifyOrder(OrderTask orderTask, final MokoCharacteristic mokoCharacteristic) {
        LogModule.i("app set device NOTIFY : " + orderTask.orderType.getName());
        mokoCharacteristic.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        final BluetoothGattDescriptor descriptor = mokoCharacteristic.characteristic.getDescriptor(DESCRIPTOR_UUID_NOTIFY);
        if (descriptor == null) {
            return;
        }
        if ((mokoCharacteristic.characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else if ((mokoCharacteristic.characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        });
    }

    // 发送可写无应答命令
    private void sendWriteNoResponseOrder(OrderTask orderTask, final MokoCharacteristic mokoCharacteristic) {
        LogModule.i("app to device WRITE no response : " + orderTask.orderType.getName());
        LogModule.i(DigitalConver.bytesToHexString(orderTask.assemble()));
        mokoCharacteristic.characteristic.setValue(orderTask.assemble());
        mokoCharacteristic.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.writeCharacteristic(mokoCharacteristic.characteristic);
            }
        });
    }

    // 发送自定义命令（无队列）
    public void sendCustomOrder(OrderTask orderTask) {
        final MokoCharacteristic mokoCharacteristic = mCharacteristicMap.get(orderTask.orderType);
        if (mokoCharacteristic == null) {
            LogModule.i("executeTask : mokoCharacteristic is null");
            return;
        }
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE) {
            sendWriteNoResponseOrder(orderTask, mokoCharacteristic);
        }
    }

    // 直接发送命令
    public void sendDirectOrder(OrderTask orderTask) {
        final MokoCharacteristic mokoCharacteristic = mCharacteristicMap.get(orderTask.orderType);
        if (mokoCharacteristic == null) {
            LogModule.i("sendDirectOrder: mokoCharacteristic is null");
            return;
        }
        LogModule.i("app to device WRITE no response : " + orderTask.orderType.getName());
        LogModule.i(DigitalConver.bytesToHexString(orderTask.assemble()));
        mokoCharacteristic.characteristic.setValue(orderTask.assemble());
        mokoCharacteristic.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.writeCharacteristic(mokoCharacteristic.characteristic);
            }
        });
    }

    /**
     * Clears the internal cache and forces a refresh of the services from the
     * remote device.
     */
    public boolean refreshDeviceCache() {
        if (mBluetoothGatt != null) {
            try {
                BluetoothGatt localBluetoothGatt = mBluetoothGatt;
                Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                    return bool;
                }
            } catch (Exception localException) {
                LogModule.i("An exception occured while refreshing device");
            }
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // step
    ///////////////////////////////////////////////////////////////////////////
    private int mDailyStepCount;
    private ArrayList<DailyStep> mDailySteps;
    private DailyStep mDailyStep;

    public void initStepsList() {
        if (mDailySteps != null) {
            mDailySteps.clear();
        } else {
            mDailySteps = new ArrayList<>();
        }
    }

    public void setDailyStepCount(int dailyStepCount) {
        this.mDailyStepCount = dailyStepCount;
    }

    public int getDailyStepCount() {
        return mDailyStepCount;
    }

    public ArrayList<DailyStep> getDailySteps() {
        return mDailySteps;
    }

    public void setDailySteps(ArrayList<DailyStep> dailySteps) {
        this.mDailySteps = dailySteps;
    }

    public DailyStep getDailyStep() {
        return mDailyStep;
    }

    public void setDailyStep(DailyStep dailyStep) {
        this.mDailyStep = dailyStep;
    }

    ///////////////////////////////////////////////////////////////////////////
    // sleep
    ///////////////////////////////////////////////////////////////////////////
    private int mSleepIndexCount;
    private int mSleepRecordCount;
    private ArrayList<DailySleep> mDailySleeps;
    private HashMap<Integer, DailySleep> mSleepsMap;

    public void initSleepIndexList() {
        if (mDailySleeps != null) {
            mDailySleeps.clear();
        } else {
            mDailySleeps = new ArrayList<>();
        }
        if (mSleepsMap != null) {
            mSleepsMap.clear();
        } else {
            mSleepsMap = new HashMap<>();
        }
    }

    public void setSleepIndexCount(int sleepIndexCount) {
        this.mSleepIndexCount = sleepIndexCount;
    }


    public int getSleepIndexCount() {
        return mSleepIndexCount;
    }

    public ArrayList<DailySleep> getDailySleeps() {
        return mDailySleeps;
    }

    public void setDailySleeps(ArrayList<DailySleep> dailySleeps) {
        this.mDailySleeps = dailySleeps;
    }

    public void setSleepRecordCount(int sleepRecordCount) {
        this.mSleepRecordCount = sleepRecordCount;
    }

    public int getSleepRecordCount() {
        return mSleepRecordCount;
    }

    public HashMap<Integer, DailySleep> getSleepsMap() {
        return mSleepsMap;
    }

    public void setSleepsMap(HashMap<Integer, DailySleep> mSleepsMap) {
        this.mSleepsMap = mSleepsMap;
    }

    ///////////////////////////////////////////////////////////////////////////
    // heartRate
    ///////////////////////////////////////////////////////////////////////////
    private int mHeartRateCount;
    private ArrayList<HeartRate> mHeartRates;
    private HashMap<Integer, Boolean> mHeartRatesMap;

    public void initHeartRatesList() {
        if (mHeartRates != null) {
            mHeartRates.clear();
        } else {
            mHeartRates = new ArrayList<>();
        }
        if (mHeartRatesMap != null) {
            mHeartRatesMap.clear();
        } else {
            mHeartRatesMap = new HashMap<>();
        }
    }

    public void setHeartRatesCount(int heartRatesCount) {
        this.mHeartRateCount = heartRatesCount;
    }

    public int getHeartRateCount() {
        return mHeartRateCount;
    }

    public void setHeartRates(ArrayList<HeartRate> heartRates) {
        this.mHeartRates = heartRates;
    }

    public ArrayList<HeartRate> getHeartRates() {
        return mHeartRates;
    }

    public HashMap<Integer, Boolean> getHeartRatesMap() {
        return mHeartRatesMap;
    }

    public void setHeartRatesMap(HashMap<Integer, Boolean> heartRatesMap) {
        this.mHeartRatesMap = heartRatesMap;
    }

    ///////////////////////////////////////////////////////////////////////////
    // firmware
    ///////////////////////////////////////////////////////////////////////////
    private String mLastChargeTime;
    private String mProductBatch;
    private int mBatteryQuantity;

    public void setBatteryQuantity(int batteryQuantity) {
        this.mBatteryQuantity = batteryQuantity;
    }

    public int getBatteryQuantity() {
        return mBatteryQuantity;
    }

    public void setLastChargeTime(String lastChargeTime) {
        this.mLastChargeTime = lastChargeTime;
    }

    public String getLastChargeTime() {
        return mLastChargeTime;
    }

    public void setProductBatch(String productBatch) {
        this.mProductBatch = productBatch;
    }

    public String getProductBatch() {
        return mProductBatch;
    }

    ///////////////////////////////////////////////////////////////////////////
    // alarm
    ///////////////////////////////////////////////////////////////////////////
    private ArrayList<BandAlarm> mAlarms;

    public void setAlarms(ArrayList<BandAlarm> alarms) {
        mAlarms = alarms;
    }

    public ArrayList<BandAlarm> getAlarms() {
        return mAlarms;
    }

    ///////////////////////////////////////////////////////////////////////////
    // sit alert
    ///////////////////////////////////////////////////////////////////////////
    private SitAlert mSitAlert;

    public void setSitAlert(SitAlert sitAlert) {
        mSitAlert = sitAlert;
    }

    public SitAlert getSitAlert() {
        return mSitAlert;
    }

    ///////////////////////////////////////////////////////////////////////////
    // step target
    ///////////////////////////////////////////////////////////////////////////
    private int mStepTarget = 8000;

    public void setStepTarget(int stepTarget) {
        mStepTarget = stepTarget;
    }

    public int getStepTarget() {
        return mStepTarget;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////
    private boolean mUnitTypeBritish;

    public void setUnitTypeBritish(boolean unitTypeBritish) {
        mUnitTypeBritish = unitTypeBritish;
    }

    public boolean getUnitTypeBritish() {
        return mUnitTypeBritish;
    }

    ///////////////////////////////////////////////////////////////////////////
    // time format
    ///////////////////////////////////////////////////////////////////////////
    private int mTimeFormat;

    public void setTimeFormat(int timeFormat) {
        mTimeFormat = timeFormat;
    }

    public int getTimeFormat() {
        return mTimeFormat;
    }

    ///////////////////////////////////////////////////////////////////////////
    // custom screen
    ///////////////////////////////////////////////////////////////////////////
    private CustomScreen mCustomScreen;

    public void setCustomScreen(CustomScreen customScreen) {
        mCustomScreen = customScreen;
    }

    public CustomScreen getCustomScreen() {
        return mCustomScreen;
    }

    ///////////////////////////////////////////////////////////////////////////
    // last show
    ///////////////////////////////////////////////////////////////////////////
    private boolean mLastScreen;


    public void setLastScreen(boolean lastScreen) {
        mLastScreen = lastScreen;
    }

    public boolean getLastScreen() {
        return mLastScreen;
    }


    ///////////////////////////////////////////////////////////////////////////
    // heart rate interval
    ///////////////////////////////////////////////////////////////////////////
    private int mHeartRateInterval;

    public int getHeartRateInterval() {
        return mHeartRateInterval;
    }

    public void setHeartRateInterval(int heartRateInterval) {
        mHeartRateInterval = heartRateInterval;
    }


    ///////////////////////////////////////////////////////////////////////////
    // auto lighten
    ///////////////////////////////////////////////////////////////////////////
    private AutoLighten mAutoLighten;

    public AutoLighten getAutoLighten() {
        return mAutoLighten;
    }

    public void setAutoLighten(AutoLighten autoLighten) {
        this.mAutoLighten = autoLighten;
    }

    ///////////////////////////////////////////////////////////////////////////
    // user info
    ///////////////////////////////////////////////////////////////////////////
    private UserInfo mUserInfo;

    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.mUserInfo = userInfo;
    }

    ///////////////////////////////////////////////////////////////////////////
    // firmware params
    ///////////////////////////////////////////////////////////////////////////
    private FirmwareParams mParams;

    public FirmwareParams getParams() {
        return mParams;
    }

    public void setParams(FirmwareParams params) {
        this.mParams = params;
    }

    ///////////////////////////////////////////////////////////////////////////
    // dial
    ///////////////////////////////////////////////////////////////////////////
    private int mDial;

    public int getDial() {
        return mDial;
    }

    public void setDial(int dial) {
        this.mDial = dial;
    }

    ///////////////////////////////////////////////////////////////////////////
    // no disturb
    ///////////////////////////////////////////////////////////////////////////
    private NoDisturb mNodisturb;

    public NoDisturb getNodisturb() {
        return mNodisturb;
    }

    public void setNodisturb(NoDisturb nodisturb) {
        this.mNodisturb = nodisturb;
    }

}
