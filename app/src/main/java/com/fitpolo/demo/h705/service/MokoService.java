package com.fitpolo.demo.h705.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;

import com.fitpolo.demo.h705.AppConstants;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoConnStateCallback;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.OrderTaskResponse;
import com.fitpolo.support.handler.BaseMessageHandler;
import com.fitpolo.support.log.LogModule;

/**
 * @Date 2017/5/17
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.fitpolo.demo.h705.service.MokoService
 */
public class MokoService extends Service implements MokoConnStateCallback, MokoOrderTaskCallback {
    @Override
    public void onConnectSuccess() {
        Intent intent = new Intent(MokoConstants.ACTION_DISCOVER_SUCCESS);
        sendOrderedBroadcast(intent, null);
    }

    @Override
    public void onDisConnected() {
        Intent intent = new Intent(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
        sendOrderedBroadcast(intent, null);
    }

    @Override
    public void onConnTimeout(int reConnCount) {
        Intent intent = new Intent(MokoConstants.ACTION_DISCOVER_TIMEOUT);
        intent.putExtra(AppConstants.EXTRA_CONN_COUNT, reConnCount);
        sendBroadcast(intent);
    }

    @Override
    public void onOrderResult(OrderTaskResponse response) {

        Intent intent = new Intent(new Intent(MokoConstants.ACTION_ORDER_RESULT));
        intent.putExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK, response);
        sendOrderedBroadcast(intent, null);
    }

    @Override
    public void onOrderTimeout(OrderTaskResponse response) {
        Intent intent = new Intent(new Intent(MokoConstants.ACTION_ORDER_TIMEOUT));
        intent.putExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK, response);
        sendBroadcast(intent);
    }

    @Override
    public void onOrderFinish() {
        sendBroadcast(new Intent(MokoConstants.ACTION_ORDER_FINISH));
    }

    @Override
    public void onCreate() {
        LogModule.i("创建MokoService...onCreate");
        super.onCreate();
    }

    public void connectBluetoothDevice(String address) {
        MokoSupport.getInstance().connDevice(this, address, this);
    }

    /**
     * @Date 2017/5/23
     * @Author wenzheng.liu
     * @Description 断开手环
     */
    public void disConnectBle() {
        MokoSupport.getInstance().setReconnectCount(0);
        MokoSupport.getInstance().disConnectBle();
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    public boolean isSyncData() {
        return MokoSupport.getInstance().isSyncData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogModule.i("启动MokoService...onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    private IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        LogModule.i("绑定MokoService...onBind");
        return mBinder;
    }

    @Override
    public void onLowMemory() {
        LogModule.i("内存吃紧，销毁MokoService...onLowMemory");
        disConnectBle();
        super.onLowMemory();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogModule.i("解绑MokoService...onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        LogModule.i("销毁MokoService...onDestroy");
        disConnectBle();
        MokoSupport.getInstance().setOpenReConnect(false);
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public MokoService getService() {
            return MokoService.this;
        }
    }

    public ServiceHandler mHandler;

    public class ServiceHandler extends BaseMessageHandler<MokoService> {

        public ServiceHandler(MokoService service) {
            super(service);
        }

        @Override
        protected void handleMessage(MokoService service, Message msg) {
        }
    }
}
