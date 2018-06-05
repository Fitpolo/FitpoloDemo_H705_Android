package com.fitpolo.demo.h705.activity;

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
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.fitpolo.demo.h705.AppConstants;
import com.fitpolo.demo.h705.R;
import com.fitpolo.demo.h705.service.DfuService;
import com.fitpolo.demo.h705.service.MokoService;
import com.fitpolo.demo.h705.utils.FileUtils;
import com.fitpolo.demo.h705.utils.Utils;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.entity.AutoLighten;
import com.fitpolo.support.entity.BandAlarm;
import com.fitpolo.support.entity.BleDevice;
import com.fitpolo.support.entity.CustomScreen;
import com.fitpolo.support.entity.DailySleep;
import com.fitpolo.support.entity.DailyStep;
import com.fitpolo.support.entity.HeartRate;
import com.fitpolo.support.entity.NoDisturb;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderTaskResponse;
import com.fitpolo.support.entity.SitAlert;
import com.fitpolo.support.entity.UserInfo;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.task.ZReadAlarmsTask;
import com.fitpolo.support.task.ZReadAutoLightenTask;
import com.fitpolo.support.task.ZReadBatteryTask;
import com.fitpolo.support.task.ZReadCustomScreenTask;
import com.fitpolo.support.task.ZReadDialTask;
import com.fitpolo.support.task.ZReadHeartRateIntervalTask;
import com.fitpolo.support.task.ZReadHeartRateTask;
import com.fitpolo.support.task.ZReadLastChargeTimeTask;
import com.fitpolo.support.task.ZReadLastScreenTask;
import com.fitpolo.support.task.ZReadNoDisturbTask;
import com.fitpolo.support.task.ZReadParamsTask;
import com.fitpolo.support.task.ZReadSitAlertTask;
import com.fitpolo.support.task.ZReadSleepGeneralTask;
import com.fitpolo.support.task.ZReadStepTargetTask;
import com.fitpolo.support.task.ZReadStepTask;
import com.fitpolo.support.task.ZReadTimeFormatTask;
import com.fitpolo.support.task.ZReadUnitTypeTask;
import com.fitpolo.support.task.ZReadUserInfoTask;
import com.fitpolo.support.task.ZReadVersionTask;
import com.fitpolo.support.task.ZWriteAlarmsTask;
import com.fitpolo.support.task.ZWriteAutoLightenTask;
import com.fitpolo.support.task.ZWriteCustomScreenTask;
import com.fitpolo.support.task.ZWriteDialTask;
import com.fitpolo.support.task.ZWriteHeartRateIntervalTask;
import com.fitpolo.support.task.ZWriteLastScreenTask;
import com.fitpolo.support.task.ZWriteNoDisturbTask;
import com.fitpolo.support.task.ZWriteShakeTask;
import com.fitpolo.support.task.ZWriteSitAlertTask;
import com.fitpolo.support.task.ZWriteStepTargetTask;
import com.fitpolo.support.task.ZWriteSystemTimeTask;
import com.fitpolo.support.task.ZWriteTimeFormatTask;
import com.fitpolo.support.task.ZWriteUnitTypeTask;
import com.fitpolo.support.task.ZWriteUserInfoTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import butterknife.ButterKnife;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description
 */

public class SendOrderActivity extends BaseActivity {
    private static final String TAG = "SendOrderActivity";
    private MokoService mService;
    private BleDevice mDevice;
    private boolean mIsUpgrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_order_layout);
        ButterKnife.bind(this);
        mDevice = (BleDevice) getIntent().getSerializableExtra("device");
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
                        case Z_READ_VERSION:
                            LogModule.i("Version code：" + MokoSupport.versionCode);
                            LogModule.i("Should upgrade：" + MokoSupport.canUpgrade);
                            break;
                        case Z_READ_USER_INFO:
                            UserInfo userInfo = MokoSupport.getInstance().getUserInfo();
                            LogModule.i(userInfo.toString());
                            break;
                        case Z_READ_ALARMS:
                            ArrayList<BandAlarm> bandAlarms = MokoSupport.getInstance().getAlarms();
                            if (bandAlarms.size() == 0) {
                                return;
                            }
                            for (BandAlarm bandAlarm : bandAlarms) {
                                LogModule.i(bandAlarm.toString());
                            }
                            break;
                        case Z_READ_UNIT_TYPE:
                            boolean unitType = MokoSupport.getInstance().getUnitTypeBritish();
                            LogModule.i("Unit type british:" + unitType);
                            break;
                        case Z_READ_TIME_FORMAT:
                            int timeFormat = MokoSupport.getInstance().getTimeFormat();
                            LogModule.i("Time format:" + timeFormat);
                            break;
                        case Z_READ_AUTO_LIGHTEN:
                            AutoLighten autoLighten = MokoSupport.getInstance().getAutoLighten();
                            LogModule.i(autoLighten.toString());
                            break;
                        case Z_READ_SIT_ALERT:
                            SitAlert sitAlert = MokoSupport.getInstance().getSitAlert();
                            LogModule.i(sitAlert.toString());
                            break;
                        case Z_READ_LAST_SCREEN:
                            boolean lastScreen = MokoSupport.getInstance().getLastScreen();
                            LogModule.i("Last screen:" + lastScreen);
                            break;
                        case Z_READ_HEART_RATE_INTERVAL:
                            int interval = MokoSupport.getInstance().getHeartRateInterval();
                            LogModule.i("Heart rate interval:" + interval);
                            break;
                        case Z_READ_CUSTOM_SCREEN:
                            CustomScreen customScreen = MokoSupport.getInstance().getCustomScreen();
                            LogModule.i(customScreen.toString());
                            break;
                        case Z_READ_STEPS:
                            ArrayList<DailyStep> lastestSteps = MokoSupport.getInstance().getDailySteps();
                            if (lastestSteps == null || lastestSteps.isEmpty()) {
                                return;
                            }
                            for (DailyStep step : lastestSteps) {
                                LogModule.i(step.toString());
                            }
                            break;
                        case Z_READ_SLEEP_GENERAL:
                            ArrayList<DailySleep> lastestSleeps = MokoSupport.getInstance().getDailySleeps();
                            if (lastestSleeps == null || lastestSleeps.isEmpty()) {
                                return;
                            }
                            for (DailySleep sleep : lastestSleeps) {
                                LogModule.i(sleep.toString());
                            }
                            break;
                        case Z_READ_HEART_RATE:
                            ArrayList<HeartRate> lastestHeartRate = MokoSupport.getInstance().getHeartRates();
                            if (lastestHeartRate == null || lastestHeartRate.isEmpty()) {
                                return;
                            }
                            for (HeartRate heartRate : lastestHeartRate) {
                                LogModule.i(heartRate.toString());
                            }
                            break;
                        case Z_READ_STEP_TARGET:
                            LogModule.i("Step target:" + MokoSupport.getInstance().getStepTarget());
                            break;
                        case Z_READ_DIAL:
                            LogModule.i("Dial:" + MokoSupport.getInstance().getDial());
                            break;
                        case Z_READ_NODISTURB:
                            LogModule.i(MokoSupport.getInstance().getNodisturb().toString());
                            break;
                        case Z_READ_PARAMS:
                            LogModule.i("Product batch：" + MokoSupport.getInstance().getProductBatch());
                            LogModule.i("Params：" + MokoSupport.getInstance().getParams().toString());
                            break;
                        case Z_READ_LAST_CHARGE_TIME:
                            LogModule.i("Last charge time：" + MokoSupport.getInstance().getLastChargeTime());
                            break;
                        case Z_READ_BATTERY:
                            LogModule.i("Battery：" + MokoSupport.getInstance().getBatteryQuantity());
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
            MokoSupport.getInstance().sendOrder(new ZReadVersionTask(mService));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };


    public void getInnerVersion(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadVersionTask(mService));
    }

    public void setSystemTime(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteSystemTimeTask(mService));
    }

    public void setUserInfo(View view) {
        UserInfo userInfo = new UserInfo();
        userInfo.age = 23;
        userInfo.gender = 0;
        userInfo.height = 170;
        userInfo.weight = 80;
        userInfo.birthdayMonth = 6;
        userInfo.birthdayDay = 1;
        userInfo.stepExtent = (int) Math.floor(userInfo.height * 0.45);
        MokoSupport.getInstance().sendOrder(new ZWriteUserInfoTask(mService, userInfo));
    }

    public void getUserInfo(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadUserInfoTask(mService));
    }

    public void setAllAlarms(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteAlarmsTask(mService, new ArrayList<BandAlarm>()));
    }

    public void getAllAlarms(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadAlarmsTask(mService));
    }

    public void setUnitType(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteUnitTypeTask(mService, 0));
    }


    public void getUnitType(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadUnitTypeTask(mService));
    }

    public void setTimeFormat(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteTimeFormatTask(mService, 0));
    }

    public void getTimeFormat(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadTimeFormatTask(mService));
    }

    public void setAutoLigten(View view) {
        AutoLighten autoLighten = new AutoLighten();
        autoLighten.autoLighten = 0;
        autoLighten.startTime = "08:00";
        autoLighten.endTime = "23:00";
        MokoSupport.getInstance().sendOrder(new ZWriteAutoLightenTask(mService, autoLighten));
    }

    public void getAutoLigten(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadAutoLightenTask(mService));

    }

    public void setSitAlert(View view) {
        SitAlert alert = new SitAlert();
        alert.alertSwitch = 0;
        alert.startTime = "11:00";
        alert.endTime = "18:00";
        MokoSupport.getInstance().sendOrder(new ZWriteSitAlertTask(mService, alert));
    }

    public void getSitAlert(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadSitAlertTask(mService));
    }

    public void setLastScreen(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteLastScreenTask(mService, 0));
    }

    public void getLastScreen(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadLastScreenTask(mService));
    }

    public void setHeartRateInterval(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteHeartRateIntervalTask(mService, 2));
    }

    public void getHeartRateInterval(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadHeartRateIntervalTask(mService));
    }

    public void setCustomScreen(View view) {
        CustomScreen customScreen = new CustomScreen(true, true, true, true, true, true);
        MokoSupport.getInstance().sendOrder(new ZWriteCustomScreenTask(mService, customScreen));
    }


    public void getCustomScreen(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadCustomScreenTask(mService));
    }

    public void setStepTarget(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteStepTargetTask(mService, 20000));
    }

    public void getStepTarget(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadStepTargetTask(mService));
    }

    public void setDial(View view) {
        MokoSupport.getInstance().sendOrder(new ZWriteDialTask(mService, 2));
    }

    public void getDial(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadDialTask(mService));
    }

    public void setNoDiturb(View view) {
        NoDisturb noDisturb = new NoDisturb();
        noDisturb.noDisturb = 1;
        noDisturb.startTime = "08:00";
        noDisturb.endTime = "23:00";
        MokoSupport.getInstance().sendOrder(new ZWriteNoDisturbTask(mService, noDisturb));
    }

    public void getNoDiturb(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadNoDisturbTask(mService));
    }

    public void getLastestSteps(View view) {
        Calendar calendar = Utils.strDate2Calendar("2018-06-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        MokoSupport.getInstance().sendOrder(new ZReadStepTask(mService, calendar));
    }

    public void getLastestSleeps(View view) {
        Calendar calendar = Utils.strDate2Calendar("2018-06-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        MokoSupport.getInstance().sendOrder(new ZReadSleepGeneralTask(mService, calendar));
    }

    public void getLastestHeartRate(View view) {
        Calendar calendar = Utils.strDate2Calendar("2018-06-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        MokoSupport.getInstance().sendOrder(new ZReadHeartRateTask(mService, calendar));
    }

    public void getFirmwareParams(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadParamsTask(mService));
    }

    public void getBattery(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadBatteryTask(mService));
    }

    public void getLastChargeTime(View view) {
        MokoSupport.getInstance().sendOrder(new ZReadLastChargeTimeTask(mService));
    }

    public void shakeBand(View view) {
        MokoSupport.getInstance().sendDirectOrder(new ZWriteShakeTask(mService));
    }

    public void sendMultiOrders(View view) {
        ZReadUnitTypeTask unitTypeTask = new ZReadUnitTypeTask(mService);
        ZReadTimeFormatTask timeFormatTask = new ZReadTimeFormatTask(mService);
        ZReadSitAlertTask sitAlertTask = new ZReadSitAlertTask(mService);
        MokoSupport.getInstance().sendOrder(unitTypeTask, timeFormatTask, sitAlertTask);
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


    private void upgrade(String firmwarePath) {
        mIsUpgrade = true;
        final File firmwareFile = new File(firmwarePath);
        if (firmwareFile.exists()) {
            final DfuServiceInitiator starter = new DfuServiceInitiator(mDevice.address)
                    .setDeviceName(mDevice.name)
                    .setKeepBond(false)
                    .setDisableNotification(true);
            starter.setZip(null, firmwarePath);
            starter.start(this, DfuService.class);
            showDFUProgressDialog("Waiting...");
        } else {
            Toast.makeText(this, "file is not exists!", Toast.LENGTH_SHORT).show();
        }
    }

    private void back() {
        if (MokoSupport.getInstance().isConnDevice(this, mDevice.address)) {
            mService.disConnectBle();
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

    @Override
    protected void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }

    private int mDeviceConnectCount;


    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            LogModule.w("onDeviceConnecting...");
            mDeviceConnectCount++;
            if (mDeviceConnectCount > 3) {
                Toast.makeText(SendOrderActivity.this, "Error:DFU Failed", Toast.LENGTH_SHORT).show();
                dismissDFUProgressDialog();
                final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(SendOrderActivity.this);
                final Intent abortAction = new Intent(DfuService.BROADCAST_ACTION);
                abortAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT);
                manager.sendBroadcast(abortAction);
            }
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            LogModule.w("onDeviceDisconnecting...");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            mDFUDialog.setMessage("DfuProcessStarting...");
        }


        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            mDFUDialog.setMessage("EnablingDfuMode...");
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            mDFUDialog.setMessage("FirmwareValidating...");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            Toast.makeText(SendOrderActivity.this, "DfuCompleted!", Toast.LENGTH_SHORT).show();
            dismissDFUProgressDialog();
            SendOrderActivity.this.finish();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            mDFUDialog.setMessage("DfuAborted...");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            mDFUDialog.setMessage("Progress:" + percent + "%");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            Toast.makeText(SendOrderActivity.this, "Error:" + message, Toast.LENGTH_SHORT).show();
            LogModule.i("Error:" + message);
            dismissDFUProgressDialog();
        }
    };

    private ProgressDialog mDFUDialog;

    private void showDFUProgressDialog(String tips) {
        mDFUDialog = new ProgressDialog(SendOrderActivity.this);
        mDFUDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDFUDialog.setCanceledOnTouchOutside(false);
        mDFUDialog.setCancelable(false);
        mDFUDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDFUDialog.setMessage(tips);
        if (!isFinishing() && mDFUDialog != null && !mDFUDialog.isShowing()) {
            mDFUDialog.show();
        }
    }

    private void dismissDFUProgressDialog() {
        mDeviceConnectCount = 0;
        mIsUpgrade = false;
        if (!isFinishing() && mDFUDialog != null && mDFUDialog.isShowing()) {
            mDFUDialog.dismiss();
        }
    }
}
