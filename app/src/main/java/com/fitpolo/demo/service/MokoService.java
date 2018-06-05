package com.fitpolo.demo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;

import com.fitpolo.demo.AppConstants;
import com.fitpolo.demo.utils.Utils;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoConnStateCallback;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.AutoLighten;
import com.fitpolo.support.entity.BandAlarm;
import com.fitpolo.support.entity.CustomScreen;
import com.fitpolo.support.entity.OrderTaskResponse;
import com.fitpolo.support.entity.SitAlert;
import com.fitpolo.support.entity.UserInfo;
import com.fitpolo.support.handler.BaseMessageHandler;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.task.OrderTask;

import java.util.Calendar;
import java.util.List;

/**
 * @Date 2017/5/17
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.fitpolo.demo.service.MokoService
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

    // 连接设备后要先获取固件版本号，确定功能展示和是否升级
    public void getInnerVersion() {
        MokoSupport.getInstance().sendOrder(getInnerVersionTask());
    }

    public void getFirmwareVersion() {
        MokoSupport.getInstance().sendOrder(getFirmwareVersionTask());
    }

    public void getgetBatteryDailyStepsCount() {
        MokoSupport.getInstance().sendOrder(getBatteryDailyStepsCountTask());
    }

    public void setSystemTime() {
        MokoSupport.getInstance().sendOrder(getSystemTimeTask());
    }

    public void setAllAlarms(List<BandAlarm> alarms) {
        OrderTask allAlarmTask = syncAllAlarmTask(alarms);
        MokoSupport.getInstance().sendOrder(allAlarmTask);
    }

    public void setSitAlert(SitAlert sitAlert) {
        OrderTask sitAlertTask = syncSitAlert(sitAlert);
        MokoSupport.getInstance().sendOrder(sitAlertTask);
    }

    public void setLastScreen(boolean lastScreen) {
        OrderTask lastScreenTask = syncLastScreenTask(lastScreen);
        MokoSupport.getInstance().sendOrder(lastScreenTask);
    }

    public void setUnitType(boolean unitTypeBritish) {
        OrderTask unitTypeTask = syncUnitTypeTask(unitTypeBritish);
        MokoSupport.getInstance().sendOrder(unitTypeTask);
    }

    public void setTimeFormat(int timeFormat) {
        OrderTask timeFormatTask = syncTimeFormatTask(timeFormat);
        MokoSupport.getInstance().sendOrder(timeFormatTask);
    }

    public void setUserInfo(UserInfo userInfo) {
        MokoSupport.getInstance().sendOrder(syncUserInfoTask(userInfo));
    }

    public void setAutoLighten(AutoLighten autoLight) {
        OrderTask autoLightenTask = syncAutoLightenTask(autoLight);
        MokoSupport.getInstance().sendOrder(autoLightenTask);
    }

    public void getAllHeartRate() {
        AllHeartRateTask heartRateTask = new AllHeartRateTask(this);
        MokoSupport.getInstance().sendOrder(heartRateTask);
    }

    public void getAllSleeps() {
        AllSleepIndexTask allSleepIndexTask = new AllSleepIndexTask(this);
        MokoSupport.getInstance().sendOrder(allSleepIndexTask);
    }

    public void getSleepHeartCount() {
        SleepHeartCountTask sleepHeartCountTask = new SleepHeartCountTask(this);
        MokoSupport.getInstance().sendOrder(sleepHeartCountTask);
    }

    public void getAllSteps() {
        AllStepsTask allStepsTask = new AllStepsTask(this);
        MokoSupport.getInstance().sendOrder(allStepsTask);
    }

    public void syncFunctionDisplay(CustomScreen functions) {
        FunctionDisplayTask functionDisplayTask = new FunctionDisplayTask(this, functions);
        MokoSupport.getInstance().sendOrder(functionDisplayTask);
    }

    public void syncHeartRateInterval(int interval) {
        OrderTask heartRateIntervalTask = new HeartRateIntervalTask(this, interval);
        MokoSupport.getInstance().sendOrder(heartRateIntervalTask);
    }


    private BatteryDailyStepsCountTask getBatteryDailyStepsCountTask() {
        return new BatteryDailyStepsCountTask(this);
    }

    private FirmwareVersionTask getFirmwareVersionTask() {
        return new FirmwareVersionTask(this);
    }

    public void getFirmwareParams() {
        OrderTask firmwareParamTask = new FirmwareParamTask(this);
        MokoSupport.getInstance().sendOrder(firmwareParamTask);
    }

    private OrderTask syncLastScreenTask(boolean lastScreen) {
        return new LastScreenTask(this, lastScreen ? 1 : 0);
    }

    private OrderTask syncAutoLightenTask(AutoLighten autoLight) {
        return new AutoLightenTask(this, autoLight.autoLighten);
    }

    private OrderTask syncSitAlert(SitAlert sitAlert) {
        return new SitLongTimeAlertTask(this, sitAlert);
    }

    private OrderTask getInnerVersionTask() {
        return new InnerVersionTask(this);
    }

    private OrderTask getSystemTimeTask() {
        return new SystemTimeTask(this);
    }

    public void readAllAlarms() {
        MokoSupport.getInstance().sendOrder(new ReadAlarmsTask(this));
    }

    public void readSettings() {
        MokoSupport.getInstance().sendOrder(new ReadSettingTask(this));
    }

    public void readSitAlert() {
        MokoSupport.getInstance().sendOrder(new ReadSitAlertTask(this));
    }

    private OrderTask syncAllAlarmTask(List<BandAlarm> bandAlarms) {
        return new AllAlarmTask(this, bandAlarms);
    }

    public OrderTask syncUserInfoTask(UserInfo userInfo) {
        return new UserInfoTask(this, userInfo);
    }

    private OrderTask syncTimeFormatTask(int timeFormat) {
        return new TimeFormatTask(this, timeFormat);
    }

    public OrderTask syncUnitTypeTask(boolean unitTypeBritish) {
        return new UnitTypeTask(this, unitTypeBritish ? 1 : 0);
    }

    public ReadSettingTask getReadSettingsTask() {
        return new ReadSettingTask(this);
    }

    public ReadSitAlertTask getReadSitAlert() {
        return new ReadSitAlertTask(this);
    }

    public void shakeBand() {
        OrderTask shakeBandTask = new ShakeBandTask(this);
        MokoSupport.getInstance().sendDirectOrder(shakeBandTask);
    }

    public void setPhoneNotify(String showText, boolean isPhoneNumber) {
        OrderTask shakeBandTask = new NotifyPhoneTask(this, showText, isPhoneNumber);
        MokoSupport.getInstance().sendDirectOrder(shakeBandTask);
    }

    public void setSmsNotify(String showText, boolean isPhoneNumber) {
        OrderTask shakeBandTask = new NotifySmsTask(this, showText, isPhoneNumber);
        MokoSupport.getInstance().sendDirectOrder(shakeBandTask);
    }

    public void getLastestSteps() {
        Calendar lastSyncTime = Utils.strDate2Calendar("2018-06-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        OrderTask stepsTask = new LastestStepsTask(this, lastSyncTime);
        MokoSupport.getInstance().sendOrder(stepsTask);
    }

    public void getLastestSleeps() {
        Calendar lastSyncTime = Utils.strDate2Calendar("2018-06-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        OrderTask sleepGeneral = new LastestSleepIndexTask(this, lastSyncTime);
        MokoSupport.getInstance().sendOrder(sleepGeneral);
    }

    public void getLastestHeartRate() {
        Calendar lastSyncTime = Utils.strDate2Calendar("2018-06-01 00:00", AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        OrderTask heartRateTask = new LastestHeartRateTask(this, lastSyncTime);
        MokoSupport.getInstance().sendOrder(heartRateTask);
    }

    public void sendMultiOrders() {
        LogModule.i("发送多个命令...");
        SystemTimeTask systemTimeTask = new SystemTimeTask(this);
        LastScreenTask lastShowTask = new LastScreenTask(this, 0);
        AutoLightenTask autoLightenTask = new AutoLightenTask(this, 1);
        FirmwareVersionTask firmwareVersionTask = new FirmwareVersionTask(this);
        MokoSupport.getInstance().sendOrder(systemTimeTask, lastShowTask, autoLightenTask, firmwareVersionTask);
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
