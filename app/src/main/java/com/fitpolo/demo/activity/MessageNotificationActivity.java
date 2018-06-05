package com.fitpolo.demo.activity;


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
import android.widget.Toast;

import com.fitpolo.demo.R;
import com.fitpolo.demo.service.MokoService;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.task.OrderTask;

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

    public void notifyWechat(View view) {
        OrderTask orderTask = new NotifyWechatTask(mService, "111");
        MokoSupport.getInstance().sendDirectOrder(orderTask);
    }

    public void notifyQQ(View view) {
        OrderTask orderTask = new NotifyQQTask(mService, "222");
        MokoSupport.getInstance().sendDirectOrder(orderTask);
    }

    public void notifyWhatsapp(View view) {
        OrderTask orderTask = new NotifyWhatsAppTask(mService, "333");
        MokoSupport.getInstance().sendDirectOrder(orderTask);
    }

    public void notifyFacebook(View view) {
        OrderTask orderTask = new NotifyFacebookTask(mService, "aaa");
        MokoSupport.getInstance().sendDirectOrder(orderTask);
    }

    public void notifyTwitter(View view) {
        OrderTask orderTask = new NotifyTwitterTask(mService, "abc");
        MokoSupport.getInstance().sendDirectOrder(orderTask);
    }

    public void notifySkype(View view) {
        OrderTask orderTask = new NotifySkypeTask(mService, "asd123");
        MokoSupport.getInstance().sendDirectOrder(orderTask);
    }

    public void notifySnapchat(View view) {
        OrderTask orderTask = new NotifySnapchatTask(mService, "3dcx3");
        MokoSupport.getInstance().sendDirectOrder(orderTask);
    }

    public void notifyLine(View view) {
        OrderTask orderTask = new NotifyLineTask(mService, "skks8");
        MokoSupport.getInstance().sendDirectOrder(orderTask);
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
                    abortBroadcast();
                    Toast.makeText(MessageNotificationActivity.this, "Connect failed", Toast.LENGTH_SHORT).show();
                    MessageNotificationActivity.this.finish();
                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    abortBroadcast();
                }
                if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                    abortBroadcast();
                    Toast.makeText(MessageNotificationActivity.this, "Success", Toast.LENGTH_SHORT).show();
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
