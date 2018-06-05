package com.fitpolo.demo.activity;

import android.app.ProgressDialog;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.fitpolo.demo.R;
import com.fitpolo.demo.adapter.DeviceAdapter;
import com.fitpolo.demo.service.MokoService;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoScanDeviceCallback;
import com.fitpolo.support.entity.BleDevice;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description
 */

public class MainActivity extends BaseActivity implements AdapterView.OnItemClickListener, MokoScanDeviceCallback {
    private static final String TAG = "MainActivity";
    @Bind(R.id.lv_device)
    ListView lvDevice;

    private ArrayList<BleDevice> mDatas;
    private DeviceAdapter mAdapter;
    private ProgressDialog mDialog;
    private MokoService mService;
    private String deviceMacAddress;
    private HashMap<String, BleDevice> deviceMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        ButterKnife.bind(this);
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
        initContentView();
    }

    private void initContentView() {
        mDialog = new ProgressDialog(this);
        mDatas = new ArrayList<>();
        deviceMap = new HashMap<>();
        mAdapter = new DeviceAdapter(this);
        mAdapter.setItems(mDatas);
        lvDevice.setAdapter(mAdapter);
        lvDevice.setOnItemClickListener(this);
    }

    public void searchDevices(View view) {
        MokoSupport.getInstance().startScanDevice(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mDialog.setMessage("Connect...");
        mDialog.show();
        BleDevice device = (BleDevice) parent.getItemAtPosition(position);
        mService.connectBluetoothDevice(device.address);
        deviceMacAddress = device.address;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(intent.getAction())) {
                    abortBroadcast();
                    if (!MainActivity.this.isFinishing() && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "Connect success", Toast.LENGTH_SHORT).show();
                    Intent orderIntent = new Intent(MainActivity.this, SendOrderActivity.class);
                    orderIntent.putExtra("deviceMacAddress", deviceMacAddress);
                    startActivity(orderIntent);
                }
                if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(intent.getAction())) {
                    abortBroadcast();
                    if (MokoSupport.getInstance().isBluetoothOpen() && MokoSupport.getInstance().getReconnectCount() > 0) {
                        return;
                    }
                    if (!MainActivity.this.isFinishing() && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "Connect failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConnection);
        stopService(new Intent(this, MokoService.class));
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
            filter.addAction(MokoConstants.ACTION_DISCOVER_SUCCESS);
            filter.setPriority(100);
            registerReceiver(mReceiver, filter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    public void onStartScan() {
        deviceMap.clear();
        mDialog.setMessage("Scanning...");
        mDialog.show();
    }

    @Override
    public void onScanDevice(BleDevice device) {
        deviceMap.put(device.address, device);
        mDatas.clear();
        mDatas.addAll(deviceMap.values());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStopScan() {
        if (!MainActivity.this.isFinishing() && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDatas.clear();
        mDatas.addAll(deviceMap.values());
        mAdapter.notifyDataSetChanged();
    }
}
