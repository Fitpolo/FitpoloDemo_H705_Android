## 1.Import and use SDK
### 1.1	import module project fitpolosupport
### 1.2	settings.gradle document，quote fitpolosupport project：

	include ':app',':fitpolosupport'

### 1.3	Edit the build.gradle file of the main project:

	dependencies {
	    compile fileTree(dir: 'libs', include: ['*.jar'])
	    compile project(path: ':fitpolosupport')
	}

### 1.4	Import sdk at project initialization

	public class BaseApplication extends Application {
	    @Override
	    public void onCreate() {
	        super.onCreate();
	        // 初始化
	        MokoSupport.getInstance().init(getApplicationContext());
	    }
	}


## 2.Function Introduction

- The methods provided in sdk include: scanning device, pairing device, sending queue command (processing response), sending no queue command (no processing response), open, close, reconnection, obtaining Bluetooth open status, obtaining device connection status, disconnecting from device Connection, etc.
- Since the scanning device and the receiving command are asynchronous thread processing, it is recommended that all methods be called in `Service` to ensure that the app can receive data normally in the background;
- The method can be called with `MokoSupport.getInstance()`;

### 2.1	startScanDevice

	@Description  scanning device
	public void startScanDevice(final MokoScanDeviceCallback callback) {}

`MokoScanDeviceCallback`：call back 

	@Description scan device call back
	public interface ScanDeviceCallback {
	    @Description  start scan
	    void onStartScan();
	    @Description  scanned device
	    void onScanDevice(BleDevice device);
	    @Description  end scan
	    void onStopScan();
	}

- Prepare for the scan in `onStartScan()`;

- Receive scanned devices on `onScanDevice(BleDevice device)`. Scanned devices include MAC address, name, semaphore, and scan history.
		public class BleDevice implements Serializable, Comparable<BleDevice> {
		    public String address;
		    public String name;
		    public int rssi;
		    public String verifyCode;
		    public byte[] scanRecord;
			...
		}
	for example
		BleDevice{address='DA:22:C3:C7:7D', name='FitpolpHR', rssi=-38, verifyCode='0C8D02', scanRecord=[2,1,6,7...]}

- Do the processing work after scanning in `onStopScan()`;

### 2.2	createBluetoothGatt

	@Description  pair device
	public void connDevice(Context context, String address, MokoConnStateCallback mokoConnStateCallback) {}

Enter the parameters:

1. context

2. device MAC address

3. `MokoConnStateCallback`；callback


	@Description  Front end display connection callback
	public interface MokoConnStateCallback {
	    @Description  connect successfully
	    void onConnectSuccess();
	    @Description  disconnect
	    void onDisConnected();
	    @Description  reconnect timeout
	    void onConnTimeout(int reConnCount);
	}

- `onConnectSuccess()` means connect successful

- `onDisConnected()` means connect failed

- `onConnTimeout(int reConnCount)`  means connect failed，reConnCount  reconnect times；

### 2.3	setOpenReConnect

	@Description   setreconnect
    public void setOpenReConnect(boolean openReConnect){}

`openReConnect` is true, then reconnect is opened. When the connection fails or the connection is disconnected, the system will execute the connection thread. If the Bluetooth is turned off, the reconnection will be performed in 5 seconds. If the reconnection fails, continue to reconnect;
-  `openReConnect`is false，then turn it off and reconnect
### 2.4	sendOrder

	@Description   send order
	public void sendOrder(OrderTask... orderTasks){}

- could send singel order
- Can also send multiple commands, the command is processed in a queue, first in, first out

Abstract class command, including command enumeration, command response callback, command response result;

	public abstract class OrderTask {
		public OrderType orderType;
		public OrderEnum order;
	    public MokoOrderTaskCallback callback;
	    public OrderTaskResponse response;
	}

1. OrderType

		public enum OrderType implements Serializable {
			READ_CHARACTER("READ_CHARACTER", "0000ffb0-0000-1000-8000-00805f9b34fb"),
			WRITE_CHARACTER("WRITE_CHARACTER", "0000ffb1-0000-1000-8000-00805f9b34fb"),
			STEP_CHARACTER("STEP_CHARACTER", "0000ffb2-0000-1000-8000-00805f9b34fb"),
			HEART_RATE_CHARACTER("HEART_RATE_CHARACTER", "0000ffb3-0000-1000-8000-00805f9b34fb"),
			;

			private String uuid;
			private String name;

			OrderType(String name, String uuid) {
				this.name = name;
				this.uuid = uuid;
			}

			public String getUuid() {
				return uuid;
			}

			public String getName() {
				return name;
			}
		}

	- Command type: Each command belongs to one type. At present, H703/H705 has read, write, step and heart rate (the latter two support real-time data change notification). All types need to be connected to the device to open the notification Function, then could receive a response

2. OrderEnum

		public enum OrderEnum implements Serializable {
			READ_NOTIFY("turn on read notify", 0),
			WRITE_NOTIFY("turn on write notify", 0) 
			STEP_NOTIFY("turn on step counting notify", 0), 
			HEART_RATE_NOTIFY("turn on heart rate notify", 0),

			Z_READ_ALARMS("read alarm", 0x01), 
			Z_READ_SIT_ALERT("read Sedentary reminder", 0x04),  
			Z_READ_STEP_TARGET("read step target", 0x06),  
			Z_READ_UNIT_TYPE("read unit type", 0x07), 
			...
			private String orderName;
			private int orderHeader;

			OrderEnum(String orderName, int orderHeader) {
				this.orderName = orderName;
				this.orderHeader = orderHeader;
			}

			public int getOrderHeader() {
				return orderHeader;
			}

			public String getOrderName() {
				return orderName;
			}
		｝
	- orderName: order name；
	- orderHeader: Distinguish the header of the command: 
	- 	Different commands correspond to different enumeration types. When multiple commands are executed,  command is answered according to the type;

3. MokoOrderTaskCallback

		public interface MokoOrderTaskCallback {
			//  response success
		    void onOrderResult(OrderTaskResponse response);
			//  response timeout
		    void onOrderTimeout(OrderTaskResponse response);
			//  order executed complete
		    void onOrderFinish();
		}

	- onOrderResult(OrderTaskResponse response) response success，response include OrderEnum，could judge which order's result according to order enum;
	- onOrderTimeout(OrderTaskResponse response) response timeout，response include OrderEnum，could judge which order time out according to order enum;
	- onOrderFinish() order finished executing, when no order in the queue, callback the method.
4. OrderTaskResponse

		public class OrderTaskResponse implements Serializable {
			public OrderEnum order;
			public int responseType;
			public byte[] responseValue;
		}

	- responseType:`RESPONSE_TYPE_NOTIFY`and`RESPONSE_TYPE_WRITE_NO_RESPONSE`two types，distinguish order type；
	- responseValue: response reutuned value

OrderTask：

	1.gain inner version No.
		ZReadVersionTask
		After returning the result, you can get the bracelet information as follows:
		MokoSupport.versionCode;// gain firmware 
		MokoSupport.firmwareEnum;// gain firmware type
		MokoSupport.canUpgrade;// whether could upgrade
	2.set system time
		ZWriteSystemTimeTask
	3. set user information 
		ZWriteUserInfoTask
		UserInfo Incoming users need to pass in user information
		public class UserInfo {
			public int weight;//  weight
			public int height;//  height
			public int age;//  age
			public int birthdayMonth;//  birthday month
			public int birthdayDay;//  birthday date
			public int gender;// Gender Male: 0; Female: 1
			public int stepExtent;//  step extent
		}
	4. gain user information
		ZReadUserInfoTask
		MokoSupport.getInstance().getUserInfo();
	5. set alarm data
		ZWriteAlarmsTask
		List<BandAlarm> Incoming access to the alarm information
		public class BandAlarm {
		    public String time;// time，formate：HH:mm
		    //  state
		    // bit[7]：0：close；1：open；
		    // bit[6]：1：sunday 
		    // bit[5]：1：saturday
		    // bit[4]：1：Friday
		    // bit[3]：1：Thursday
		    // bit[2]：1：Wednesday
		    // bit[1]：1：Tuesday
		    // bit[0]：1：Monday
		    // ex： every Sunday turn on：11000000； every Monday to Friday to trun on: 10011111；
		    public String state;
		    public int type;// type，0：take medicine；1：drink water；3：normaly；4：sleep ；5：take medicine；6： do sports
		｝
	6. gain alarm datas
		ZReadAlarmsTask
		MokoSupport.getInstance().getAlarms();
	7. set unit 
		ZWriteUnitTypeTask
		 Incoming entry unit system
		unitType// 0： Chinese type；1：British type， Default Chinese type
	8. gain unit type 
		ZReadUnitTypeTask
		MokoSupport.getInstance().getUnitTypeBritish();
	9. Set display time format
		ZWriteTimeFormatTask
		 Incoming entry should display time format
		timeFormat;// 0：24；1：12， default 24-hour system
	10. gain time display formate 
		ZReadTimeFormatTask
		MokoSupport.getInstance().getTimeFormat();
	11. set light up the screen  by tap
		ZWriteAutoLightenTask
		incoming entry AutoLighten
		public class AutoLighten {
			public int autoLighten; //  shake screen ，1： on；0： off；
			public String startTime;//  start time， formate：HH:mm;
			public String endTime;// end time，formate：HH:mm;	8. set Sedentary reminder
		}
	12. gain light up the screen by tap
		ZReadAutoLightenTask
		MokoSupport.getInstance().getAutoLighten();
	13. set sendentary reminder
		ZWriteSitAlertTask
		 SitAlert  incoing entry sedentary reminder information
		public class SitAlert {
		    public int alertSwitch; //  advise to sport，1： on；0： off；
		    public String startTime;//  start time， formate：HH:mm;
		    public String endTime;//  end time， formate：HH:mm;
		｝
	14. gain sedentary reminder information
		ZReadSitAlertTask
		MokoSupport.getInstance().getSitAlert();
	15. set last time display
		ZWriteLastScreenTask
		 Incoming parameters need pass last time display
		lastScreen;// 1： on；0： off
	16. gain last time display
		ZReadLastScreenTask
		MokoSupport.getInstance().getLastScreen();
	17. set heart rate  meansure intervial
		ZWriteHeartRateIntervalTask
		 Incoming parameters need pass heart rate intervial
		heartRateInterval;// 0： off；1： 10mins；2： 20mins；3： 30mins
	18. gain heart rate measure intrvial
		ZReadHeartRateIntervalTask
		MokoSupport.getInstance().getHeartRateInterval();
	19. set functions display
		ZWriteCustomScreenTask
		 Incoming parameters need pass functions display
		public class CustomScreen {
			public boolean duration;//whether display sports time
			public boolean calorie;//whether display calories burnt
			public boolean distance;//whether display sports distance
			public boolean heartrate;//whether display heart rate
			public boolean step;//whether display steps
			public boolean sleep;//whether display sleep
		}
	20. gain functions display
		ZReadCustomScreenTask
		MokoSupport.getInstance().getCustomScreen();
	21. set target steps
		ZWriteStepTargetTask
		 incoming parameter need pass target steps
		stepTarget;// value range 1~60000
	22. gain target step
		ZReadStepTargetTask
		MokoSupport.getInstance().getStepTarget();
	23. set watch face
		ZWriteDialTask
		 incoming parameter need pass watch face
		dial;// value range 1~3
	24. gain watch face setting
		ZReadDialTask
		MokoSupport.getInstance().getDial();
	25. set do not disturb
		ZWriteNoDisturbTask
		 incoming parameter need pass do not disturb
		public class NoDisturb {
			public int noDisturb; //  do not disturb，1： on；0： off；
			public String startTime;//  start time， formate：HH:mm;
			public String endTime;//  end time， formate：HH:mm;
		}
	26. Read not disturb
		ZReadNoDisturbTask
		MokoSupport.getInstance().getNodisturb();
	27. Get unsynchronized step data
		ZReadStepTask
		 incoming parameter need timestamp
		lastSyncTime;// yyyy-MM-dd HH:mm
		 After returning the result, you can view the step data after the timestamp.
		MokoSupport.getInstance().getDailySteps()
	28. Get unsynchronized sleep record data
		ZReadSleepGeneralTask
		 incoming parameter need timestamp
		lastSyncTime;// yyyy-MM-dd HH:mm
		 After returning the result, you can view the sleep data after the timestamp.
		MokoSupport.getInstance().getDailySleeps()
	29. get unsymchronized heart rate datas
		ZReadHeartRateTask
		 incoming parameter need timestamp
		lastSyncTime;// yyyy-MM-dd HH:mm
		 After returning the result, you can view the heart rate after the timestamp.
		MokoSupport.getInstance().getHeartRates()
	30. turn steps change notification
		ZOpenStepListenerTask
		 Can be received by the broadcast receiver when turned on
		if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
							OrderEnum orderEnum = (OrderEnum) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_CURRENT_DATA_TYPE);
							switch (orderEnum) {
								case Z_STEPS_CHANGES_LISTENER:
									DailyStep dailyStep = MokoSupport.getInstance().getDailyStep();
									LogModule.i(dailyStep.toString());
									break;
							}
						}
	31. gain Hardware parameter
		ZReadParamsTask
		 Check the firmware parameters after returning the result
		MokoSupport.getInstance().getProductBatch();// produce batch
		MokoSupport.getInstance().getParams();// hardware parameter
		public class FirmwareParams {
			public String test; // bit0:flash, bit1:G sensor,bit2: hr  measure;
			public int reflectiveThreshold;// Reflective threshold, default1380;
			public int reflectiveValue;// present reflective threshold
			public int batchYear;//  produce batch year
			public int batchWeek;//  produce batch week
			public int speedUnit;// Bluetooth connection speed unit is 1.25ms
		}
	32. gain battery power
		ZReadBatteryTask
		 check battery power after returning the result
		MokoSupport.getInstance().getBatteryQuantity();
	33. gain last time charge time
		ZReadLastChargeTimeTask
		MokoSupport.getInstance().getLastChargeTime();
	34. set bracelet vibrate
		ZWriteShakeTask
		default vibrate twice, vibrate 1 second then stop 1 second
		 no response handle
	35. set bracelet notification
		ZWriteNotifyTask
		public enum NotifyEnum {
			PHONE_CALL(0X00),// call notification
			SMS(0X01),// SMS notification
			WECHAT(0X02),// wechat notification
			QQ(0X03),// qq notification
			WHATSAPP(0X04),//Whatsapp  notification
			FACEBOOK(0X05),//Facebook  notification
			TWITTER(0X06),//Twitter  notification
			SKYPE(0X07),//Skype  notification
			SNAPCHAT(0X08),//Snapchat  notification
			LINE(0X09),//Line  notification
			;
		}
		showText;// no more than 14 bytes
		isOpen;// on/off notification

### 2.5	sendDirectOrder

Send commands directly, this method can be used when the command does not need to answer, only supports the sending of a single command.

	public void sendDirectOrder(OrderTask orderTask){}

### 2.6	isBluetoothOpen

Judge if Bluetooth is turned on

	public boolean isBluetoothOpen(){}

### 2.7	isConnDevice

judge if the bracelet is connected with app

	public boolean isConnDevice(Context context, String address){}

 incominf parameter：address	bracelet mac address

### 2.8	disConnectBle

disconnect the bracelet

	public void disConnectBle(){}

## 3.Save Log to SD Card

- The SDK integrates the function of saving the log to the SD card.  reference from:[https://github.com/elvishew/xLog](https://github.com/elvishew/xLog "XLog")
-  Initialization method could be available from `MokoSupport.getInstance().init(getApplicationContext())`
- The file name folder name and file name saved on the SD card can be modified.

		public class LogModule {
			private static final String TAG = "fitpoloDemoH705";//  file name
		    private static final String LOG_FOLDER = "fitpoloDemoH705";//  file folder name
			...
		}

- Storage policy: only save the current day data and the previous day data, the previous day data is suffixed with .bak
- Call method：
	- LogModule.v("log info");
	- LogModule.d("log info");
	- LogModule.i("log info");
	- LogModule.w("log info");
	- LogModule.e("log info");

## 4.Upgrade

- The upgrade function is based on DFU and is used as follows:

	- Register/anti-register listener

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

	- Turn on DFU, you need to pass the device Mac address and device name, firmware path, create DfuService

            final DfuServiceInitiator starter = new DfuServiceInitiator(mDevice.address)
                    .setDeviceName(mDevice.name)
                    .setKeepBond(false)
                    .setDisableNotification(true);
            starter.setZip(null, firmwarePath);
            starter.start(this, DfuService.class);

	-  Monitor upgrade status

		onProgressChanged  gain upgrade progess
		onError  gain fail reason
		onDfuCompleted  upgrade succeed

- note：
	-	can not send datas to the bracelet  during the upgrade;
	-	When the upgrade starts, DFU will automatically disconnect the bracelet first, and then restart after the reconnection;
	-	After the upgrade fails or succeeds, the bracelet will be disconnected again, and the bracelet needs to be reconnected;




