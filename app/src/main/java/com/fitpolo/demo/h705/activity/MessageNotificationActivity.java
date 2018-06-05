package com.fitpolo.demo.h705.activity;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.fitpolo.demo.h705.R;
import com.fitpolo.demo.h705.service.MokoService;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.entity.NotifyEnum;
import com.fitpolo.support.task.ZWriteNotifyTask;

import butterknife.ButterKnife;

public class MessageNotificationActivity extends BaseActivity {
    private MokoService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_notification);
        ButterKnife.bind(this);
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
            filter.addAction(MokoConstants.ACTION_DISCOVER_TIMEOUT);
            filter.addAction(MokoConstants.ACTION_ORDER_RESULT);
            filter.addAction(MokoConstants.ACTION_ORDER_TIMEOUT);
            filter.addAction(MokoConstants.ACTION_ORDER_FINISH);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.setPriority(300);
            registerReceiver(mReceiver, filter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };


    public void notifyPhoneCall(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteNotifyTask(mService, NotifyEnum.PHONE_CALL, "123123", true));
    }

    public void notifySms(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteNotifyTask(mService, NotifyEnum.SMS, "12jjj213", true));
    }

    public void notifyWechat(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteNotifyTask(mService, NotifyEnum.WECHAT, "SDFS", true));
    }

    public void notifyQQ(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteNotifyTask(mService, NotifyEnum.QQ, "sddsd", true));
    }

    public void notifyWhatsapp(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteNotifyTask(mService, NotifyEnum.WHATSAPP, "1111", true));
    }

    public void notifyFacebook(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteNotifyTask(mService, NotifyEnum.FACEBOOK, "2222", true));
    }

    public void notifyTwitter(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteNotifyTask(mService, NotifyEnum.TWITTER, "3423", true));
    }

    public void notifySkype(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteNotifyTask(mService, NotifyEnum.SKYPE, "52323", true));
    }

    public void notifySnapchat(View view) {
        MokoSupport.getInstance().sendDirectOrder(new ZWriteNotifyTask(mService, NotifyEnum.SNAPCHAT, "vczv", true));
    }

    public void notifyLine(View view) {
        MokoSupport.getInstance().sendDirectOrder(new ZWriteNotifyTask(mService, NotifyEnum.LINE, "asd", true));
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_OFF:
                            MessageNotificationActivity.this.finish();
                            break;
                    }
                }
                if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(action)) {
                    MessageNotificationActivity.this.finish();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConnection);
    }
}
