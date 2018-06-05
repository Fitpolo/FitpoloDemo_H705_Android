package com.fitpolo.demo.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fitpolo.demo.R;
import com.fitpolo.demo.service.MokoService;
import com.fitpolo.demo.utils.FileUtils;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.entity.AutoLighten;
import com.fitpolo.support.entity.BandAlarm;
import com.fitpolo.support.entity.CustomScreen;
import com.fitpolo.support.entity.DailySleep;
import com.fitpolo.support.entity.DailyStep;
import com.fitpolo.support.entity.HeartRate;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderTaskResponse;
import com.fitpolo.support.entity.SitAlert;
import com.fitpolo.support.entity.UserInfo;
import com.fitpolo.support.handler.UpgradeHandler;
import com.fitpolo.support.log.LogModule;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description
 */

public class SendOrderActivity extends BaseActivity {
    private static final String TAG = "SendOrderActivity";
    @Bind(R.id.btn_inner_version)
    Button btnInnerVersion;
    @Bind(R.id.btn_system_time)
    Button btnSystemTime;
    @Bind(R.id.btn_user_info)
    Button btnUserInfo;
    @Bind(R.id.btn_band_alarm)
    Button btnBandAlarm;
    @Bind(R.id.btn_unit_type)
    Button btnUnitType;
    @Bind(R.id.btn_time_format)
    Button btnTimeFormat;
    @Bind(R.id.btn_auto_lighten)
    Button btnAutoLighten;
    @Bind(R.id.btn_sit_alert)
    Button btnSitAlert;
    @Bind(R.id.btn_last_screen)
    Button btnLastScreen;
    @Bind(R.id.btn_multi_orders)
    Button btnMultiOrders;
    @Bind(R.id.btn_heart_rate_interval)
    Button btnHeartRateInterval;
    @Bind(R.id.btn_function_display)
    Button btnFunctionDisplay;
    @Bind(R.id.btn_firmware_version)
    Button btnFirmwareVersion;
    @Bind(R.id.btn_battery_step_count)
    Button btnBatteryStepCount;
    @Bind(R.id.btn_sleep_heart_rate_count)
    Button btnSleepHeartRateCount;
    @Bind(R.id.btn_all_steps)
    Button btnAllSteps;
    @Bind(R.id.btn_all_sleeps)
    Button btnAllSleeps;
    @Bind(R.id.btn_phone_notify)
    Button btnPhoneNotify;
    @Bind(R.id.btn_sms_notify)
    Button btnSmsNotify;
    @Bind(R.id.btn_shake_band)
    Button btnShakeBand;
    @Bind(R.id.btn_lastest_steps)
    Button btnLastestSteps;
    @Bind(R.id.btn_lastest_sleeps)
    Button btnLastestSleeps;
    @Bind(R.id.btn_lastest_heart_rate)
    Button btnLastestHeartRate;
    @Bind(R.id.btn_upgrade_firmware)
    Button btnUpgradeFirmware;
    @Bind(R.id.btn_all_heart_rate)
    Button btnAllHeartRate;
    @Bind(R.id.btn_read_all_alarms)
    Button btnReadAllAlarms;
    @Bind(R.id.btn_read_sit_alert)
    Button btnReadSitAlert;
    @Bind(R.id.btn_read_settings)
    Button btnReadSettings;
    @Bind(R.id.btn_notification)
    Button btnNotification;
    @Bind(R.id.btn_firmware_params)
    Button btnFirmwareParams;
    private MokoService mService;
    private String deviceMacAddress;
    private boolean mIsUpgrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_order_layout);
        ButterKnife.bind(this);
        deviceMacAddress = getIntent().getStringExtra("deviceMacAddress");
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
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
                            SendOrderActivity.this.finish();
                            break;
                    }
                }
                if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(action)) {
                    abortBroadcast();
                    if (!mIsUpgrade) {
                        Toast.makeText(SendOrderActivity.this, "Connect failed", Toast.LENGTH_SHORT).show();
                        SendOrderActivity.this.finish();
                    }
                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    switch (orderEnum) {
                        case getInnerVersion:

                            btnAllHeartRate.setVisibility(MokoSupport.showHeartRate ? View.VISIBLE : View.GONE);
                            btnHeartRateInterval.setVisibility(MokoSupport.showHeartRate ? View.VISIBLE : View.GONE);

                            btnLastestSteps.setVisibility(MokoSupport.supportNewData ? View.VISIBLE : View.GONE);
                            btnLastestSleeps.setVisibility(MokoSupport.supportNewData ? View.VISIBLE : View.GONE);
                            btnLastestHeartRate.setVisibility(MokoSupport.showHeartRate && MokoSupport.supportNewData ? View.VISIBLE : View.GONE);

                            btnFirmwareParams.setVisibility(MokoSupport.versionCodeLast >= 28 ? View.VISIBLE : View.GONE);

                            btnReadAllAlarms.setVisibility(MokoSupport.supportNotifyAndRead ? View.VISIBLE : View.GONE);
                            btnReadSettings.setVisibility(MokoSupport.supportNotifyAndRead ? View.VISIBLE : View.GONE);
                            btnReadSitAlert.setVisibility(MokoSupport.supportNotifyAndRead ? View.VISIBLE : View.GONE);
                            btnNotification.setVisibility(MokoSupport.supportNotifyAndRead ? View.VISIBLE : View.GONE);

                            LogModule.i("Support heartRate：" + MokoSupport.showHeartRate);
                            LogModule.i("Support newData：" + MokoSupport.supportNewData);
                            LogModule.i("Support notify and read：" + MokoSupport.supportNotifyAndRead);
                            LogModule.i("Version code：" + MokoSupport.versionCode);
                            LogModule.i("Should upgrade：" + MokoSupport.canUpgrade);
                            break;
                        case setSystemTime:
                            break;
                        case setUserInfo:
                            break;
                        case setBandAlarm:
                            break;
                        case setUnitType:
                            break;
                        case setTimeFormat:
                            break;
                        case setAutoLigten:
                            break;
                        case setSitLongTimeAlert:
                            break;
                        case setLastScreen:
                            break;
                        case setHeartRateInterval:
                            break;
                        case setFunctionDisplay:
                            break;
                        case getFirmwareVersion:
                            LogModule.i("firmware version：" + MokoSupport.versionCodeShow);
                            break;
                        case getBatteryDailyStepCount:
                            LogModule.i("battery：" + MokoSupport.getInstance().getBatteryQuantity());
                            break;
                        case getSleepHeartCount:
                            break;
                        case getAllSteps:
                            ArrayList<DailyStep> steps = MokoSupport.getInstance().getDailySteps();
                            if (steps == null || steps.isEmpty()) {
                                return;
                            }
                            for (DailyStep step : steps) {
                                LogModule.i(step.toString());
                            }
                            break;
                        case getAllSleepIndex:
                            ArrayList<DailySleep> sleeps = MokoSupport.getInstance().getDailySleeps();
                            if (sleeps == null || sleeps.isEmpty()) {
                                return;
                            }
                            for (DailySleep sleep : sleeps) {
                                LogModule.i(sleep.toString());
                            }
                            break;
                        case getAllHeartRate:
                            ArrayList<HeartRate> heartRates = MokoSupport.getInstance().getHeartRates();
                            if (heartRates == null || heartRates.isEmpty()) {
                                return;
                            }
                            for (HeartRate heartRate : heartRates) {
                                LogModule.i(heartRate.toString());
                            }
                            break;
                        case getLastestSteps:
                            ArrayList<DailyStep> lastestSteps = MokoSupport.getInstance().getDailySteps();
                            if (lastestSteps == null || lastestSteps.isEmpty()) {
                                return;
                            }
                            for (DailyStep step : lastestSteps) {
                                LogModule.i(step.toString());
                            }
                            break;
                        case getLastestSleepIndex:
                            ArrayList<DailySleep> lastestSleeps = MokoSupport.getInstance().getDailySleeps();
                            if (lastestSleeps == null || lastestSleeps.isEmpty()) {
                                return;
                            }
                            for (DailySleep sleep : lastestSleeps) {
                                LogModule.i(sleep.toString());
                            }
                            break;
                        case getLastestHeartRate:
                            ArrayList<HeartRate> lastestHeartRate = MokoSupport.getInstance().getHeartRates();
                            if (lastestHeartRate == null || lastestHeartRate.isEmpty()) {
                                return;
                            }
                            for (HeartRate heartRate : lastestHeartRate) {
                                LogModule.i(heartRate.toString());
                            }
                            break;
                        case getFirmwareParam:
                            LogModule.i("Last charge time：" + MokoSupport.getInstance().getLastChargeTime());
                            LogModule.i("Product batch：" + MokoSupport.getInstance().getProductBatch());
                            break;
                        case READ_ALARMS:
                            ArrayList<BandAlarm> bandAlarms = MokoSupport.getInstance().getAlarms();
                            for (BandAlarm bandAlarm : bandAlarms) {
                                LogModule.i(bandAlarm.toString());
                            }
                            break;
                        case READ_SETTING:
                            boolean unitType = MokoSupport.getInstance().getUnitTypeBritish();
                            int timeFormat = MokoSupport.getInstance().getTimeFormat();
                            CustomScreen customScreen = MokoSupport.getInstance().getCustomScreen();
                            boolean lastScreen = MokoSupport.getInstance().getLastScreen();
                            int interval = MokoSupport.getInstance().getHeartRateInterval();
                            AutoLighten autoLighten = MokoSupport.getInstance().getAutoLighten();
                            LogModule.i("Unit type:" + unitType);
                            LogModule.i("Time format:" + timeFormat);
                            LogModule.i("Function display:" + customScreen.toString());
                            LogModule.i("Last screen:" + lastScreen);
                            LogModule.i("HeartRate interval:" + interval);
                            LogModule.i("Auto light:" + autoLighten.toString());

                            break;
                        case READ_SIT_ALERT:
                            SitAlert sitAlert = MokoSupport.getInstance().getSitAlert();
                            LogModule.i("Sit alert:" + sitAlert.toString());
                            break;
                    }

                }
                if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
                    Toast.makeText(SendOrderActivity.this, "Timeout", Toast.LENGTH_SHORT).show();
                }
                if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                    Toast.makeText(SendOrderActivity.this, "Success", Toast.LENGTH_SHORT).show();
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
            filter.setPriority(200);
            registerReceiver(mReceiver, filter);
            // first
            mService.getInnerVersion();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };


    public void getInnerVersion(View view) {
        mService.getInnerVersion();
    }

    public void setSystemTime(View view) {
        mService.setSystemTime();
    }

    public void setUserInfo(View view) {
        UserInfo userInfo = new UserInfo();
        userInfo.age = 23;
        userInfo.gender = 0;
        userInfo.height = 170;
        userInfo.weight = 80;
        userInfo.stepExtent = (int) Math.floor(userInfo.height * 0.45);
        mService.setUserInfo(userInfo);
    }

    public void setAllAlarms(View view) {
        mService.setAllAlarms(new ArrayList<BandAlarm>());
    }

    public void setUnitType(View view) {
        mService.setUnitType(false);
    }

    public void setTimeFormat(View view) {
        mService.setTimeFormat(0);
    }

    public void setAutoLigten(View view) {
        AutoLighten autoLighten = new AutoLighten();
        autoLighten.autoLighten = 0;
        autoLighten.startTime = "08:00";
        autoLighten.endTime = "23:00";
        mService.setAutoLighten(autoLighten);
    }

    public void setSitAlert(View view) {
        SitAlert alert = new SitAlert();
        alert.alertSwitch = 0;
        alert.startTime = "11:00";
        alert.endTime = "18:00";
        mService.setSitAlert(alert);
    }

    public void setLastScreen(View view) {
        mService.setLastScreen(true);
    }

    public void setHeartRateInterval(View view) {
        mService.syncHeartRateInterval(3);
    }

    public void setFunctionDisplay(View view) {
        CustomScreen customScreen = new CustomScreen(true, true, true, true, true);
        mService.syncFunctionDisplay(customScreen);
    }

    public void getFirmwareVersion(View view) {
        mService.getFirmwareVersion();
    }

    public void getBatteryDailyStepCount(View view) {
        mService.getgetBatteryDailyStepsCount();
    }

    public void getSleepHeartCount(View view) {
        mService.getSleepHeartCount();
    }

    public void getAllSteps(View view) {
        if (MokoSupport.getInstance().getDailyStepCount() == 0) {
            Toast.makeText(this, "Get step count first", Toast.LENGTH_SHORT).show();
            return;
        }
        mService.getAllSteps();
    }

    public void getAllSleeps(View view) {
        if (MokoSupport.getInstance().getSleepIndexCount() == 0) {
            Toast.makeText(this, "Get sleep count first", Toast.LENGTH_SHORT).show();
            return;
        }
        mService.getAllSleeps();
    }

    public void getAllHeartRate(View view) {
        if (MokoSupport.getInstance().getHeartRateCount() == 0) {
            Toast.makeText(this, "Get heartrate count first", Toast.LENGTH_SHORT).show();
            return;
        }
        mService.getAllHeartRate();
    }

    public void sendMultiOrders(View view) {
        mService.sendMultiOrders();
    }

    public void shakeBand(View view) {
        mService.shakeBand();
    }

    public void setPhoneNotify(View view) {
        mService.setPhoneNotify("1234567", true);
    }

    public void setSmsNotify(View view) {
        mService.setSmsNotify("abcdef", false);
    }

    public void getLastestSteps(View view) {
        mService.getLastestSteps();
    }

    public void getLastestSleeps(View view) {
        mService.getLastestSleeps();
    }

    public void getLastestHeartRate(View view) {
        mService.getLastestHeartRate();
    }


    public void getFirmwareParams(View view) {
        mService.getFirmwareParams();
    }


    public void readAllAlarms(View view) {
        mService.readAllAlarms();
    }

    public void readSitAlert(View view) {
        mService.readSitAlert();
    }

    public void readSettings(View view) {
        mService.readSettings();
    }

    public void notification(View view) {
        startActivity(new Intent(this, MessageNotificationActivity.class));
    }

    private static final int REQUEST_CODE_FILE = 2;

    public void upgradeFirmware(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_FILE);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "install file manager app", Toast.LENGTH_SHORT).show();
        }
    }

    private ProgressDialog mDialog;

    private void upgrade(String firmwarePath) {
        mIsUpgrade = true;
        if (!isFinishing()) {
            mDialog = ProgressDialog.show(this, null, "upgrade...", false, false);
        }
        UpgradeHandler upgradeHandler = new UpgradeHandler(this);
        upgradeHandler.setFilePath(firmwarePath, deviceMacAddress, new UpgradeHandler.IUpgradeCallback() {
            @Override
            public void onUpgradeError(int errorCode) {
                if (mDialog != null && mDialog.isShowing() && !isFinishing()) {
                    mDialog.dismiss();
                }
                switch (errorCode) {
                    case UpgradeHandler.EXCEPTION_FILEPATH_IS_NULL:
                        Toast.makeText(SendOrderActivity.this, "file is not exist！", Toast.LENGTH_SHORT).show();
                        break;
                    case UpgradeHandler.EXCEPTION_DEVICE_MAC_ADDRESS_IS_NULL:
                        Toast.makeText(SendOrderActivity.this, "mac address is null！", Toast.LENGTH_SHORT).show();
                        break;
                    case UpgradeHandler.EXCEPTION_UPGRADE_FAILURE:
                        Toast.makeText(SendOrderActivity.this, "upgrade failed！", Toast.LENGTH_SHORT).show();
                        back();
                        break;
                }
                mIsUpgrade = false;
            }

            @Override
            public void onProgress(int progress) {
                if (mDialog != null && mDialog.isShowing() && !isFinishing()) {
                    mDialog.setMessage("upgrade progress:" + progress + "%");
                }
            }

            @Override
            public void onUpgradeDone() {
                if (mDialog != null && mDialog.isShowing() && !isFinishing()) {
                    mDialog.dismiss();
                }
                Toast.makeText(SendOrderActivity.this, "upgrade success", Toast.LENGTH_SHORT).show();
                SendOrderActivity.this.finish();
            }
        });
    }

    private void back() {
        if (MokoSupport.getInstance().isConnDevice(this, deviceMacAddress)) {
            MokoSupport.getInstance().disConnectBle();
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_FILE:
                    Uri uri = data.getData();
                    String path = FileUtils.getPath(this, uri);
                    upgrade(path);
                    break;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            back();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
