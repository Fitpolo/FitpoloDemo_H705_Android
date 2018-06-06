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

	- 命令类型：每个命令都归属一种类型，目前H703/H705有读、写、记步和心率（后两种支持实时数据变化通知），所有类型都需要连上设备后打开该特征的通知功能，才能收到应答

2. OrderEnum

		public enum OrderEnum implements Serializable {
			READ_NOTIFY("打开读取通知", 0),
			WRITE_NOTIFY("打开设置通知", 0),
			STEP_NOTIFY("打开记步通知", 0),
			HEART_RATE_NOTIFY("打开心率通知", 0),

			Z_READ_ALARMS("读取闹钟", 0x01),
			Z_READ_SIT_ALERT("读取久坐提醒", 0x04),
			Z_READ_STEP_TARGET("读取记步目标", 0x06),
			Z_READ_UNIT_TYPE("读取单位类型", 0x07),
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
		ZReadVersionTask
		返回结果后可获取手环信息，方法如下：
		MokoSupport.versionCode;//获取固件版本
		MokoSupport.firmwareEnum;//获取固件类型;
		MokoSupport.canUpgrade;//是否可升级;
	2.设置系统时间
		ZWriteSystemTimeTask
	3.设置用户信息
		ZWriteUserInfoTask
		入参需传入用户信息UserInfo
		public class UserInfo {
			public int weight;// 体重
			public int height;// 身高
			public int age;// 年龄
			public int birthdayMonth;// 出生月
			public int birthdayDay;// 出生日
			public int gender;// 性别 男：0；女：1
			public int stepExtent;// 步幅
		}
	4.获取用户信息
		ZReadUserInfoTask
		MokoSupport.getInstance().getUserInfo();
	5.设置闹钟数据
		ZWriteAlarmsTask
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
	6.获取闹钟数据
		ZReadAlarmsTask
		MokoSupport.getInstance().getAlarms();
	7.设置单位制式
		ZWriteUnitTypeTask
		入参需传入单位制式
		unitType// 0：中式；1：英式，默认中式
	8.获取单位制式
		ZReadUnitTypeTask
		MokoSupport.getInstance().getUnitTypeBritish();
	9.设置显示时间格式
		ZWriteTimeFormatTask
		入参需传入显示时间格式
		timeFormat;// 0：24；1：12，默认24小时制
	10.获取显示时间格式
		ZReadTimeFormatTask
		MokoSupport.getInstance().getTimeFormat();
	11.设置自动点亮屏幕
		ZWriteAutoLightenTask
		入参需传入AutoLighten
		public class AutoLighten {
			public int autoLighten; // 翻腕亮屏开关，1：开；0：关；
			public String startTime;// 开始时间，格式：HH:mm;
			public String endTime;// 结束时间，格式：HH:mm;	8.设置久坐提醒
		}
	12.获取自动点亮屏幕
		ZReadAutoLightenTask
		MokoSupport.getInstance().getAutoLighten();
	13.设置久坐提醒
		ZWriteSitAlertTask
		入参需传入久坐提醒信息SitAlert
		public class SitAlert {
		    public int alertSwitch; // 久坐提醒开关，1：开；0：关；
		    public String startTime;// 开始时间，格式：HH:mm;
		    public String endTime;// 结束时间，格式：HH:mm;
		｝
	14.获取久坐提醒
		ZReadSitAlertTask
		MokoSupport.getInstance().getSitAlert();
	15.设置上次显示
		ZWriteLastScreenTask
		入参需传入上次显示
		lastScreen;// 1：打开；0：关闭
	16.获取上次显示
		ZReadLastScreenTask
		MokoSupport.getInstance().getLastScreen();
	17.设置心率监测间隔
		ZWriteHeartRateIntervalTask
		入参需传入心率监测间隔
		heartRateInterval;// 0：关闭；1：10分钟；2：20分钟；3：30分钟
	18.获取心率检测间隔
		ZReadHeartRateIntervalTask
		MokoSupport.getInstance().getHeartRateInterval();
	19.设置功能显示
		ZWriteCustomScreenTask
		入参需传入功能显示
		public class CustomScreen {
			public boolean duration;//是否显示运动时长；
			public boolean calorie;//是否显示运动消耗卡路里；
			public boolean distance;//是否显示运动运动距离；
			public boolean heartrate;//是否显示心率；
			public boolean step;//是否显示步数；
			public boolean sleep;//是否显示睡眠；
		}
	20.获取功能显示
		ZReadCustomScreenTask
		MokoSupport.getInstance().getCustomScreen();
	21.设置记步目标
		ZWriteStepTargetTask
		入参需传入目标值
		stepTarget;//取值范围1~60000
	22.获取记步目标
		ZReadStepTargetTask
		MokoSupport.getInstance().getStepTarget();
	23.设置表盘样式
		ZWriteDialTask
		入参需传入表盘样式
		dial;//取值范围1~3
	24.获取表盘样式
		ZReadDialTask
		MokoSupport.getInstance().getDial();
	25.设置勿扰
		ZWriteNoDisturbTask
		入参需传入勿扰信息
		public class NoDisturb {
			public int noDisturb; // 勿扰模式开关，1：开；0：关；
			public String startTime;// 开始时间，格式：HH:mm;
			public String endTime;// 结束时间，格式：HH:mm;
		}
	26.读取勿扰
		ZReadNoDisturbTask
		MokoSupport.getInstance().getNodisturb();
	27.获取未同步的记步数据
		ZReadStepTask
		入参需传入时间戳
		lastSyncTime;// yyyy-MM-dd HH:mm
		返回结果后可查看时间戳后的记步数据
		MokoSupport.getInstance().getDailySteps()
	28.获取未同步的睡眠记录数据
		ZReadSleepGeneralTask
		入参需传入时间戳
		lastSyncTime;// yyyy-MM-dd HH:mm
		返回结果后可查看时间戳后的睡眠数据
		MokoSupport.getInstance().getDailySleeps()
	29.获取未同步的心率数据
		ZReadHeartRateTask
		入参需传入时间戳
		lastSyncTime;// yyyy-MM-dd HH:mm
		返回结果后可查看时间戳后的心率数据
		MokoSupport.getInstance().getHeartRates()
	30.打开记步变化通知
		ZOpenStepListenerTask
		打开后可通过广播接收器接收
		if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
							OrderEnum orderEnum = (OrderEnum) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_CURRENT_DATA_TYPE);
							switch (orderEnum) {
								case Z_STEPS_CHANGES_LISTENER:
									DailyStep dailyStep = MokoSupport.getInstance().getDailyStep();
									LogModule.i(dailyStep.toString());
									break;
							}
						}
	31.获取硬件参数
		ZReadParamsTask
		返回结果后可查看固件参数
		MokoSupport.getInstance().getProductBatch();//生产批号
		MokoSupport.getInstance().getParams();//硬件参数
		public class FirmwareParams {
			public String test; // bit0:flash, bit1:G sensor,bit2: hr检测;
			public int reflectiveThreshold;// 反光阈值,默认1380;
			public int reflectiveValue;// 当前反光值;
			public int batchYear;// 生产批次年;
			public int batchWeek;// 生产批次周;
			public int speedUnit;// 蓝牙连接配速单位是1.25ms;
		}
	32.获取电量
		ZReadBatteryTask
		返回结果后可查看手环电量
		MokoSupport.getInstance().getBatteryQuantity();
	33.获取最后充电时间
		ZReadLastChargeTimeTask
		MokoSupport.getInstance().getLastChargeTime();
	34.设置手环震动
		ZWriteShakeTask
		默认震动2次，震动1秒停1秒
		无应答处理
	35.设置手环通知
		ZWriteNotifyTask
		public enum NotifyEnum {
			PHONE_CALL(0X00),//来电通知
			SMS(0X01),//短信通知
			WECHAT(0X02),//微信通知
			QQ(0X03),//QQ通知
			WHATSAPP(0X04),//Whatsapp通知
			FACEBOOK(0X05),//Facebook通知
			TWITTER(0X06),//Twitter通知
			SKYPE(0X07),//Skype通知
			SNAPCHAT(0X08),//Snapchat通知
			LINE(0X09),//Line通知
			;
		}
		showText;//不超过14个字节
		isOpen;//打开/关闭通知

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
			private static final String TAG = "fitpoloDemoH705";// 文件名
		    private static final String LOG_FOLDER = "fitpoloDemoH705";// 文件夹名
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

- 升级功能基于DFU，使用方法如下：

	- 注册/反注册监听器

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

	- 开启DFU，需传入设备Mac地址和设备名称，固件路径，创建DfuService

            final DfuServiceInitiator starter = new DfuServiceInitiator(mDevice.address)
                    .setDeviceName(mDevice.name)
                    .setKeepBond(false)
                    .setDisableNotification(true);
            starter.setZip(null, firmwarePath);
            starter.start(this, DfuService.class);

	- 监听升级状态

		onProgressChanged获取升级进度
		onError获取失败原因
		onDfuCompleted升级成功

- 注意：
	-	升级时不可对手环发送其他数据;
	-	升级开始时，DFU会先自动断开手环，重连后开始升级;
	-	升级失败或成功后会再次断开手环，需重新连接手环；



