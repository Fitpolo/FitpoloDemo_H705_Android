## 1.Import and use SDK
### 1.1	导入module工程fitpolosupport
### 1.2	配置settings.gradle文件，引用fitpolosupport工程：

	include ':app',':fitpolosupport'

### 1.3	编辑主工程的build.gradle文件：

	dependencies {
	    compile fileTree(dir: 'libs', include: ['*.jar'])
	    compile project(path: ':fitpolosupport')
	}

### 1.4	在工程初始化时导入sdk：

	public class BaseApplication extends Application {
	    @Override
	    public void onCreate() {
	        super.onCreate();
	        // 初始化
	        MokoSupport.getInstance().init(getApplicationContext());
	    }
	}


## 2.Function Introduction

- sdk中提供的方法包括：扫描设备、配对设备、发送队列命令（处理应答）、发送无队列命令（不处理应答）、打开关闭重连、获取蓝牙打开状态、获取设备连接状态、与设备断开连接等；
- 由于扫描设备和接收命令时是异步线程处理，所以建议所有方法在`Service`里调用，可保证app在后台时也能正常接收数据；
- 方法可通过`MokoSupport.getInstance()`调用；

### 2.1	startScanDevice

	@Description 扫描设备
	public void startScanDevice(final MokoScanDeviceCallback callback) {}

回调函数`MokoScanDeviceCallback`：

	@Description 扫描设备回调
	public interface ScanDeviceCallback {
	    @Description 开始扫描
	    void onStartScan();
	    @Description 扫描的设备
	    void onScanDevice(BleDevice device);
	    @Description 结束扫描
	    void onStopScan();
	}

- 在`onStartScan()`里做扫描前的准备工作；

- 在`onScanDevice(BleDevice device)`接收扫描到的设备，扫描到的设备包括MAC地址、名称、信号量和扫描记录；

		public class BleDevice implements Serializable, Comparable<BleDevice> {
		    public String address;
		    public String name;
		    public int rssi;
		    public String verifyCode;
		    public byte[] scanRecord;
			...
		}
	示例：
		BleDevice{address='DA:22:C3:C7:7D', name='FitpolpHR', rssi=-38, verifyCode='0C8D02', scanRecord=[2,1,6,7...]}

- 在`onStopScan()`里做扫描后的处理工作；

### 2.2	createBluetoothGatt

	@Description 配对设备
	public void connDevice(Context context, String address, MokoConnStateCallback mokoConnStateCallback) {}

入参：

1. 上下文；

2. 设备MAC地址；

3. 回调函数`MokoConnStateCallback`；


	@Description 前端展示连接回调
	public interface MokoConnStateCallback {
	    @Description 连接成功
	    void onConnectSuccess();
	    @Description 断开连接
	    void onDisConnected();
	    @Description 重连超时
	    void onConnTimeout(int reConnCount);
	}

- `onConnectSuccess()`表示连接成功；

- `onDisConnected()`表示连接失败；

- `onConnTimeout(int reConnCount)`表示连接失败，reConnCount重连次数；

### 2.3	setOpenReConnect

	@Description 设置重连
    public void setOpenReConnect(boolean openReConnect){}

- `openReConnect`为true，则打开重连，当连接失败或连接断开后，系统会执行连接线程，若蓝牙关闭，则5秒执行一次重连。若重连失败，则继续重连；
- `openReConnect`为false，则关闭重连

### 2.4	sendOrder

	@Description 发送命令
	public void sendOrder(OrderTask... orderTasks){}

- 可发送单个命令；
- 也可发送多个命令，命令按队列方式处理，先进先出；

抽象类命令，包含命令枚举、命令应答回调、命令应答结果；

	public abstract class OrderTask {
		public OrderType orderType;
		public OrderEnum order;
	    public MokoOrderTaskCallback callback;
	    public OrderTaskResponse response;
	}

1. OrderType

		public enum OrderType implements Serializable {
			NOTIFY("NOTIFY", "0000ffc2-0000-1000-8000-00805f9b34fb"),
			WRITE("WRITE", "0000ffc1-0000-1000-8000-00805f9b34fb"),
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

	- 命令类型：每个命令都归属一种类型，目前H701只有通知和写两种，通知类型需要脸上设备后发送，打开设备的通知功能，其余命令可通过写类型发送给设备收到应答

2. OrderEnum

		public enum OrderEnum implements Serializable {
			getInnerVersion("获取内部版本", 0x09),
			setSystemTime("设置手环时间", 0x11),
			setUserInfo("设置用户信息", 0x12),
			setBandAlarm("设置闹钟数据", 0x26),
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
	- orderName:命令的名字；
	- orderHeader:区分命令的头字段；
	- 不同的命令对应不同的枚举类型，当执行多个命令时，可根据类型判断应答的是哪个命令；

3. MokoOrderTaskCallback

		public interface MokoOrderTaskCallback {
			// 应答成功
		    void onOrderResult(OrderTaskResponse response);
			// 应答超时
		    void onOrderTimeout(OrderTaskResponse response);
			// 命令执行完成
		    void onOrderFinish();
		}

	- onOrderResult(OrderTaskResponse response)应答成功，response中包含OrderEnum，可根据OrderEnum判断是哪个命令的结果；
	- onOrderTimeout(OrderTaskResponse response)应答超时，response中包含OrderEnum，可根据OrderEnum判断是那个命令超时；
	- onOrderFinish()命令执行完毕，当队列中没有命令时，则回调该方法；

4. OrderTaskResponse

		public class OrderTaskResponse implements Serializable {
			public OrderEnum order;
			public int responseType;
			public byte[] responseValue;
		}

	- responseType:`RESPONSE_TYPE_NOTIFY`和`RESPONSE_TYPE_WRITE_NO_RESPONSE`l两种，区分命令类型；
	- responseValue:应答返回的数据

OrderTask的子类：

	1.获取内部版本号
		InnerVersionTask
		返回结果后可获取手环信息，方法如下：
		MokoSupport.showHeartRate;//是否支持同步心率;
		MokoSupport.supportNewData;//是否支持同步最新的数据;
		MokoSupport.supportNotifyAndRead;//是否支持读取数据和消息通知;
		MokoSupport.firmwareEnum;//获取固件类型;
		MokoSupport.canUpgrade;//是否可升级;
	2.设置系统时间
		SystemTimeTask
	3.设置用户信息
		UserInfoTask
		入参需传入用户信息UserInfo
	4.设置闹钟数据
		AllAlarmTask
		入参需传入闹钟信息List<BandAlarm>
		public class BandAlarm {
		    public String time;// 时间，格式：HH:mm
		    // 状态
		    // bit[7]：0：关闭；1：打开；
		    // bit[6]：1：周日；
		    // bit[5]：1：周六；
		    // bit[4]：1：周五；
		    // bit[3]：1：周四；
		    // bit[2]：1：周三；
		    // bit[1]：1：周二；
		    // bit[0]：1：周一；
		    // ex：每周日打开：11000000；每周一到周五打开10011111；
		    public String state;
		    public int type;// 类型，0：吃药；1：喝水；3：普通；4：睡觉；5：吃药；6：锻炼
		｝
	5.设置单位制式
		UnitTypeTask
		入参需传入单位制式
		unitType// 0：中式；1：英式，默认中式
	6.设置显示时间格式
		TimeFormatTask
		入参需传入显示时间格式
		timeFormat;// 0：24；1：12，默认24小时制
	7.设置自动点亮屏幕
		AutoLightenTask
		入参需传入是否自动点亮屏幕
		autoLighten;// 0：打开；1：关闭，默认打开
	8.设置久坐提醒
		SitLongTimeAlertTask
		入参需传入久坐提醒信息SitAlert
		public class SitAlert {
		    public int alertSwitch; // 久坐提醒开关，1：开；0：关；
		    public String startTime;// 开始时间，格式：HH:mm;
		    public String endTime;// 结束时间，格式：HH:mm;
		｝
	9.设置上次显示
		LastScreenTask
		入参需传入上次显示
		lastScreen;// 1：打开；0：关闭
	10.设置心率监测间隔
		HeartRateIntervalTask
		入参需传入心率监测间隔
		heartRateInterval;// 0：关闭；1：10分钟；2：20分钟；3：30分钟
	11.设置功能显示
		FunctionDisplayTask
		入参需传入功能显示
		CustomScreen;
	    // duration：是否显示运动时长；
	    // calorie：是否显示运动消耗卡路里；
	    // distance：是否显示运动运动距离；
	    // heartrate：是否显示心率；
	    // step：是否显示步数；
	12.获取获取固件版本号
		FirmwareVersionTask
		返回结果后可查看手环固件版本
		MokoSupport.versionCodeShow
		示例：
		MokoSupport.versionCodeShow = "2.1.32"
	13.获取电量和记步总数
		BatteryDailyStepsCountTask
		返回结果后可查看手环电量
		MokoSupport.getInstance().getBatteryQuantity()
	14.获取睡眠和心率总数
		SleepHeartCountTask
	15.获取记步数据
		AllStepsTask
		返回结果后可查看手环中的全部记步数据
		MokoSupport.getInstance().getDailySteps();
		public class DailyStep {
		    public String date;// 日期，yyyy-MM-dd
		    public String count;// 步数
		    public String duration;// 运动时间
		    public String distance;// 运动距离
		    public String calories;// 运动消耗卡路里
			...
		}
		示例：
		DailyStep{date='2017-06-05', count='1340', duration='5', distance='0.9', calories='78'}
	16.获取睡眠数据
		AllSleepIndexTask
		返回结果后可查看手环中的全部睡眠数据
		MokoSupport.getInstance().getDailySleeps()
		public class DailySleep {
		    public String date;// 日期，yyyy-MM-dd
		    public String startTime;// 开始时间，yyyy-MM-dd HH:mm
		    public String endTime;// 结束时间，yyyy-MM-dd HH:mm
		    public String deepDuration;// 深睡时长，单位min
		    public String lightDuration;// 浅睡时长，单位min
		    public String awakeDuration;// 清醒时长，单位min
		    public List<String> records;// 睡眠记录
			...
		}
		示例：
		DailySleep{date='2017-06-05', startTime='2017-06-04 23:00', endTime='2017-06-05 07:00', deepDuration='360', lightDuration='60', awakeDuration='60' records=['01','01','10','10','00',...]}
	17.获取心率
		AllHeartRateTask
		返回结果后可查看手环中的全部心率数据
		MokoSupport.getInstance().getHeartRates();
		public class HeartRate implements Comparable<HeartRate> {
		    public String time;
		    public String value;
			...
		}
		示例：
		HeartRate{time='2017-06-05 12:00', value='78'}
	18.获取硬件参数
		FirmwareParamTask
		返回结果后可查看固件参数
		MokoSupport.getInstance().getLastChargeTime();//最后充电时间
		MokoSupport.getInstance().getProductBatch();//生产批号
	19.获取未同步的记步数据
		LastestStepsTask
		入参需传入时间戳
		lastSyncTime;// yyyy-MM-dd HH:mm
		返回结果后可查看时间戳后的记步数据
		MokoSupport.getInstance().getDailySteps()
	20.获取未同步的睡眠记录数据
		LastestSleepIndexTask
		入参需传入时间戳
		lastSyncTime;// yyyy-MM-dd HH:mm
		返回结果后可查看时间戳后的睡眠数据
		MokoSupport.getInstance().getDailySleeps()
	21.获取未同步的心率数据
		LastestHeartRateTask
		入参需传入时间戳
		lastSyncTime;// yyyy-MM-dd HH:mm
		返回结果后可查看时间戳后的心率数据
		MokoSupport.getInstance().getHeartRates()
	22.设置手环震动
		ShakeBandTask
		默认震动2次，震动1秒停1秒
		无应答处理
	23.设置来电震动
		PhoneComingShakeTask
		入参需传入显示文案，是否是电话号码
		String showText;// 显示文案（电话号码or联系人）
    	boolean isPhoneNumber;// 是否是电话号码
		无应答处理
	24.设置短信震动
		SmsComingShakeTask
		入参需传入显示文案，是否是电话号码
		String showText;// 显示文案（电话号码or联系人）
    	boolean isPhoneNumber;// 是否是电话号码
		无应答处理

	以下功能只有固件版本是32及以上才能使用

	25.读取闹钟数据
		ReadAlarmsTask
		MokoSupport.getInstance().getAlarms()
	26.读取久坐提醒数据
		ReadSitAlertTask
		MokoSupport.getInstance().getSitAlert()
	27.读取手环配置数据
		ReadSettingTask
		MokoSupport.getInstance().getUnitTypeBritish();//单位类型
		MokoSupport.getInstance().getTimeFormat();//时间格式
		MokoSupport.getInstance().getCustomScreen();//功能显示
		MokoSupport.getInstance().getLastScreen();//是否开启最后显示
		MokoSupport.getInstance().getHeartRateInterval();//心率间隔
		MokoSupport.getInstance().getAutoLighten();//是否开启翻腕亮屏
	28.微信通知
		NotifyWechatTask
		入参需传入显示文案
		String showText;// 显示文案
		无应答处理
	29.QQ通知
		NotifyQQTask
		入参需传入显示文案
		String showText;// 显示文案
		无应答处理
	30.WhatsApp通知
		NotifyWhatsAppTask
		入参需传入显示文案
		String showText;// 显示文案
		无应答处理
	31.Facebook通知
		NotifyFacebookTask
		入参需传入显示文案
		String showText;// 显示文案
		无应答处理
	32.Twitter通知
		NotifyTwitterTask
		入参需传入显示文案
		String showText;// 显示文案
		无应答处理
	33.Skype通知
		NotifySkypeTask
		入参需传入显示文案
		String showText;// 显示文案
		无应答处理
	34.Snapchat通知
		NotifySnapchatTask
		入参需传入显示文案
		String showText;// 显示文案
		无应答处理
	35.Line通知
		NotifyLineTask
		入参需传入显示文案
		String showText;// 显示文案
		无应答处理

### 2.5	sendDirectOrder

直接发送命令，当命令无需应答时可用该方法，只支持单个命令的发送

	public void sendDirectOrder(OrderTask orderTask){}

### 2.6	isBluetoothOpen

判断蓝牙是否打开

	public boolean isBluetoothOpen(){}

### 2.7	isConnDevice

判断手环是否连接

	public boolean isConnDevice(Context context, String address){}

入参：address	手环MAC地址

### 2.8	disConnectBle

与手环断开连接

	public void disConnectBle(){}

## 3.Save Log to SD Card

- SDK中集成了Log保存到SD卡的功能，引用的是[https://github.com/elvishew/xLog](https://github.com/elvishew/xLog "XLog")
- 初始化方法在`MokoSupport.getInstance().init(getApplicationContext())`中实现
- 可修改在SD卡上保存的文件名夹名和文件名

		public class LogModule {
			private static final String TAG = "fitpoloDemo";// 文件名
		    private static final String LOG_FOLDER = "fitpoloDemo";// 文件夹名
			...
		}

- 存储策略：仅保存当天数据和前一天数据，前一天数据以.bak为后缀
- 调用方式：
	- LogModule.v("log info");
	- LogModule.d("log info");
	- LogModule.i("log info");
	- LogModule.w("log info");
	- LogModule.e("log info");

## 4.Upgrade

- 升级功能封装在com.fitpolo.support.handler.UpgradeHandler类中，使用方法如下：


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

- 调用时需要传入三个参数：
	- firmwarePath：升级固件路径；
	- deviceMacAddress：设备Mac地址（可从扫描到的设备中获得）；
	- IUpgradeCallback：升级回调接口，实现接口中的方法，可获得升级失败，升级进度和升级成功回调；

- 注意：
	-	升级时不可对手环发送其他数据;
	-	升级开始时，会先断开手环，4秒后自动连接手环，使手环进入传输数据的高速模式；
	-	升级失败或成功后会再次断开手环，需重新连接手环；


