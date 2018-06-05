## 1.Import and use SDK
### 1.1	import "module" project "fitpolosupport"
### 1.2	settings"settings.gradle"，reference "fitpolosupport" project：

	include ':app',':fitpolosupport'

### 1.3	Edit the 'build.gradle' in the main project：

	dependencies {
	    compile fileTree(dir: 'libs', include: ['*.jar'])  
	    compile project(path: ':fitpolosupport')
	}

### 1.4	import the SDK when initiating：

	public class BaseApplication extends Application {
	    @Override
	    public void onCreate() {
	        super.onCreate();
	        // init
	        Fitpolo.init(this);
	    }
	}


## 2.Function Introduction
- There are many method in the SDK, including san device, pair, send command, get the Bluetooth status of phone, get the connection status, disconnect the band.
- Because the scan and receive data is asynchronous, to APP get the data in background mode, so we suggest that you call all the method in the Service
- the method can be called in the`BluetoothModule.getInstance()`；

### 2.1	startScanDevice

	@Description  scan device
	public void startScanDevice(final ScanDeviceCallback callback) {}

callback function`ScanDeviceCallback`：

	@Description  scan device callback
	public interface ScanDeviceCallback {
	    @Description start scan
	    void onStartScan();
	    @Description scan device
	    void onScanDevice(BleDevice device);
	    @Description stop scan
	    void onStopScan();
	}

- Do prepratiton before scan in`onStartScan()`
- receive the scanned device in`onScanDevice(BleDevice device)`,including MAC address, name, int rssi and scanrecord.

		public class BleDevice implements Serializable, Comparable<BleDevice> {
		    public String address;
		    public String name;
		    public int rssi;
		    public byte[] scanRecord;
			...
		}
	For example：
		BleDevice{address='DA:22:C3:C7:7D', name='FitpolpHR', rssi=-38, scanRecord=[2,1,6,7...]}
- Do the handle work after scan in`onStopScan()`.

Get four verifycode

	public String getVerifyCode(byte[] scanRecord)

incoming parameters: Get four verifycode from the scanRecord in the scanned device, to do pair work.

### 2.2	createBluetoothGatt
	@Description pair device
	public void createBluetoothGatt(Context context, String address, ConnStateCallback connCallBack) {}

incoming parameters：

1. context.

2. Device MAC Address.

3. `ConnStateCallback`.

	@Description Front display connection callback
	public interface ConnStateCallback {
	    @Description connection succeed
	    void onConnSuccess();
	    @Description connection fail
	    void onConnFailure(int errorCode);
	    @Description disconnect
	    void onDisconnect();
	}

- `onConnSuccess()`connect successfully；
- `onConnFailure(int errorCode)`connect failed；

if connect failed, error code will be shown as following：

	public static final int CONN_ERROR_CODE_ADDRESS_NULL = 0;// null address
	public static final int CONN_ERROR_CODE_BLUTOOTH_CLOSE = 1;// blutooth close
	public static final int CONN_ERROR_CODE_CONNECTED = 2;// already connected
	public static final int CONN_ERROR_CODE_FAILURE = 3;// connect failed

- `onDisconnect()`disconnected with the device；

### 2.3	setOpenReConnect

	@Description set re-connect
    public void setOpenReConnect(boolean openReConnect){}

- If `openReConnect` is true，then open the bluetooth,When the connection fails or disconnects, the system will perform connection thread. If close the bluetooth, connection will be done every 30 seconds.If re-connect fail, continue to re-connect.
- If `openReConnect`is false，then close the bluetooth and reconnect.

### 2.4	sendOrder

	@Description //send order
	public void sendOrder(OrderTask... orderTasks){}

- Can send a single order；
- can send several orders, orders are handled in line, first in first out. 

Abstract class order, including order enumeration, command response callback, command response results

	public abstract class OrderTask {
		private OrderEnum order;
	    private OrderCallback callback;
	    private BaseResponse response;
	}

1. OrderEnum

		public enum OrderEnum implements Serializable {
		    getInnerVersion,
		    setSystemTime,
		    setUserInfo,
		    setBandAlarm,
			...
		｝

	- Different orders for different enumerated types, when performing multiple orders, according to the type to judge which command response

2. OrderCallback

		public interface OrderCallback {
			// response success
		    void onOrderResult(OrderEnum order, BaseResponse response);
			// response timeout
		    void onOrderTimeout(OrderEnum order);
			// complete the command
		    void onOrderFinish();
		}

- `onOrderResult(OrderEnum order, BaseResponse response)`
Response success, could judge which command result it is according to order
- `onOrderTimeout(OrderEnum order)`response outtime；could judge which order outtime according to order
- `onOrderFinish()`command completed, when no order in the line, call back the method

3. BaseResponse

		public class BaseResponse implements Serializable{
		    public int code;
		    public String msg;
		}

	The code show the response state, as follows：
		// Get command status code
	    public static final int ORDER_CODE_SUCCESS = 200;// success
	    public static final int ORDER_CODE_ERROR_TIMEOUT = 201;// timeout

OrderTask subclass：

	1.Get innerversion Task
		InnerVersionTask
		After return results, can judge whether bracelet support sync data on the same day, whether to support sync heart rate date, ever support sync those unsynced data, as follows:
		BluetoothModule.getInstance().isSupportTodayData();
		BluetoothModule.getInstance().isSupportHeartRate();
		BluetoothModule.getInstance().isSupportNewData();
	2.set system time task
		SystemTimeTask
	3.set user info task
		UserInfoTask
		incoming parameters need introduce UserInfo
	4.set band alarm task
		BandAlarmTask
		incoming parameters need introduce band alarm info `List<BandAlarm>`
		public class BandAlarm {
		    public String time;// time，format：HH:mm
		    // state
		    // bit[7]：0：close；1：poen；
		    // bit[6]：1：sunday；
		    // bit[5]：1：saturday；
		    // bit[4]：1：friday；
		    // bit[3]：1：thursday；
		    // bit[2]：1：wednesday；
		    // bit[1]：1：tuesday；
		    // bit[0]：1：monday；
		    // ex：Every sunday open：11000000；Every Monday to Friday open 10011111；
		    public String state;
		    public int type;// type，0：medicine；1：water；3：normal；4：sleep；5：exercise；6：sport；
		｝
	5.set unit type task
		UnitTypeTask
		incoming parameters need introduce unit type
		unitType// 0：Chinese；1：English，default chinese
	6.set time format task
		TimeFormatTask
		incoming parameters need introduce time show format
		timeFormat;// 0：24；1：12，default 24-hour
	7.set automatic lighten the screen
		AutoLightenTask
		incoming parameters need introduce if automatic lighten the screen
		autoLighten;// 0：open；1：close，default open
	8.set sitlongtime alert task
		SitLongTimeAlertTask
		 incoming parameters need introduce sit long time alert info: SitLongTimeAlert
		public class SitLongTimeAlert {
		    public int alertSwitch; // sit long time alart switch，1：open；0：close
		    public String startTime;// start time, format：HH:mm;
		    public String endTime;// end time, format：HH:mm;
		｝
	9.set last show task
		LastShowTask
		 incoming parameters need introduce LastShowTask
		lastShow;// 1：open；0：close
	10.set Heart Rate IntervalTask
		HeartRateIntervalTask
		incoming parameters need introduce HeartRateIntervalTask
		heartRateInterval;// 0：close；1：ten min；2：twenty min；3：thirty min
	11.set function display task
		FunctionDisplayTask
		incoming parameters need introduce FunctionDisplayTask
		boolean[] functions;
	    // functions[0]：Whether to show the movement time；
	    // functions[1]：Whether to show burn calories；
	    // functions[2]：Whether to show movement distance；
	    // functions[3]：Whether to show heart rate；
	    // functions[4]：Whether to show steps；
	12.get firmware version task
		FirmwareVersionTask
		After return results, can check the firm version
		String version = BluetoothModule.getInstance().getFirmwareVersion()
		for example：
		version = "2.1.22"
	13.get battery and daily steps count task
		BatteryDailyStepsCountTask
		After return results, can check the battery power
		int batteryQuantity = BluetoothModule.getInstance().getBatteryQuantity()
		for example：
		batteryQuantity = 100
	14.get sleep and heart rate task
		SleepHeartCountTask
	15.get daily steps task
		DailyStepsTask
		After return results, can check the all steps data in the bracelet
		ArrayList<DailyStep> steps = BluetoothModule.getInstance().getDailySteps()
		public class DailyStep {
		    public String date;// date，yyyy-MM-dd
		    public String count;// steps
		    public String duration;// sports time
		    public String distance;// sports distance
		    public String calories;// burnt calories
			...
		}
		examples：
		DailyStep{date='2017-06-05', count='1340', duration='5', distance='0.9', calories='78'}
	16.get sleep date
		DailySleepIndexTask
		After return results, can check the all sleep data in the bracelet
		ArrayList<DailySleep> sleeps = BluetoothModule.getInstance().getDailySleeps()
		public class DailySleep {
		    public String date;// date，yyyy-MM-dd
		    public String startTime;// start time，yyyy-MM-dd HH:mm
		    public String endTime;// end time，yyyy-MM-dd HH:mm
		    public String deepDuration;// deep sleep time，unit: min
		    public String lightDuration;// light sleep time，unit: min
		    public String awakeDuration;// wake up time，unit: min
		    public List<String> records;// sleep record
			...
		}
		for example：
		DailySleep{date='2017-06-05', startTime='2017-06-04 23:00', endTime='2017-06-05 07:00', deepDuration='360', lightDuration='60', awakeDuration='60' records=['01','01','10','10','00',...]}
	17.get heart rate data
		HeartRateTask
		After return results, can check the all heart rate data in the bracelet
		ArrayList<HeartRate> heartRates = BluetoothModule.getInstance().getHeartRates()
		public class HeartRate implements Comparable<HeartRate> {
		    public String time;
		    public String value;
			...
		}
		for example：
		HeartRate{time='2017-06-05 12:00', value='78'}
	18.get today's step, sleep,heart rate data
		TodayDataTask
		After return results, can check today step, sleep, heart rate data in the bracelet
		ArrayList<DailyStep> steps = BluetoothModule.getInstance().getDailySteps()
		ArrayList<DailySleep> sleeps = BluetoothModule.getInstance().getDailySleeps()
		ArrayList<HeartRate> heartRates = BluetoothModule.getInstance().getHeartRates()
	19.Get unsynced steps data task
		NewDailyStepsTask
		incoming parameters need introduce timestamp
		lastSyncTime;// yyyy-MM-dd HH:mm
		After return results, can check step data after special timestamp
		ArrayList<DailyStep> steps = BluetoothModule.getInstance().getDailySteps()
	20.Get unsynced sleep data task
		NewDailySleepIndexTask
		incoming parameters need introduce timestamp
		lastSyncTime;// yyyy-MM-dd HH:mm
		After return results, can check sleep data after special timestamp
		ArrayList<DailySleep> sleeps = BluetoothModule.getInstance().getDailySleeps()
	21.Get unsynced heart rate data task
		NewHeartRateTask
		incoming parameters need introduce timestamp
		lastSyncTime;// yyyy-MM-dd HH:mm
		After return results, can check heart rate data after special timestamp
		ArrayList<HeartRate> heartRates = BluetoothModule.getInstance().getHeartRates()
	22.set shake band task
		ShakeBandTask
		default shake twice, shakes 1 second stop 1 second
		no response to deal with
	23.clear band data task
		ClearBandDataTask
		no response to deal with
	24.set phonecall coming shake task
		PhoneComingShakeTask
		incoming parameters need introduce show text, whether it is cellphone no. or contact person name
		String showText;
    	boolean isPhoneNumber;
		no response to deal with
	25.set SMS coming shake task
		SmsComingShakeTask
		incoming parameters need introduce show text, whether it is cellphone no. or contact person name
		String showText;
    	boolean isPhoneNumber;
		no response to deal with

### 2.5	sendDirectOrder

Send commands directly, when command no need response, this method could be used, only support sending a single command

	public void sendDirectOrder(OrderTask orderTask){}

### 2.6	isBluetoothOpen

judge whether the bluetooth is open or close

	public boolean isBluetoothOpen(){}

### 2.7	isConnDevice

judge whether the braclet is connected or not

	public boolean isConnDevice(Context context, String address){}

入参：address	手环MAC地址

### 2.8	disConnectBle

disconnect wiht bracelet

	public void disConnectBle(){}

## 3.Save Log to SD Card

- SDK integrates the function of save the Log to SD card, is referenced [https://github.com/elvishew/xLog](https://github.com/elvishew/xLog "XLog")
- initialize method could be achieved in`Fitpolo.init(this);`
- Can be modified the document name and folder name saved in the SD card

		public class LogModule {
			private static final String TAG = "fitpoloDemo";// document name
		    private static final String LOG_FOLDER = "fitpoloDemo";// folder name
			...
		}

- storage strategy: only to save today data and the previous day data, the previous day data`.Bak` as suffix 
- calling mode：
	- LogModule.v("log info");
	- LogModule.d("log info");
	- LogModule.i("log info");
	- LogModule.w("log info");
	- LogModule.e("log info");

## 4.Upgrade

- The upgrade function is encapsulated in com.fitpolo.support.handler.UpgradeHandler, using it as following way：


		UpgradeHandler upgradeHandler = new UpgradeHandler(this);
		upgradeHandler.setFilePath(firmwarePath, deviceMacAddress,
		new UpgradeHandler.IUpgradeCallback() {
	        @Override
	        public void onUpgradeError(int errorCode) {
	            switch (errorCode) {
	                case UpgradeHandler.EXCEPTION_FILEPATH_IS_NULL:
	                    break;
	                case UpgradeHandler.EXCEPTION_DEVICE_MAC_ADDRESS_IS_NULL:
	                    break;
	                case UpgradeHandler.EXCEPTION_UPGRADE_FAILURE:
	                    break;
	            }
	        }

	        @Override
	        public void onProgress(int progress) {

	        }

	        @Override
	        public void onUpgradeDone() {

	        }
	    });

- income three parameters while callback：
	- firmwarePath：Upgrade the firmware path；
	- deviceMacAddress：Device Mac address(could get it from the scanned device )；
	- IUpgradeCallback：Upgrade the callback interface to implement the methods in the interface , obtain the upgrade failure, upgrade progress, and upgrade successful callback；

- noted：
	-	When upgrade, can not send other data to the bracelet;
	-	When the upgrade starts, the bracelet will be disconnected first and automatically connected after 4 seconds to make the bracelet enter the high-speed mode for transmitting data.；
	-	After the upgrade fails or succeeds, the bracelet will be disconnected again and needs to be reconnected.；


