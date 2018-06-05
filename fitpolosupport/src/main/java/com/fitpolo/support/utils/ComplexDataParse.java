package com.fitpolo.support.utils;

import com.fitpolo.support.entity.DailySleep;
import com.fitpolo.support.entity.DailyStep;
import com.fitpolo.support.entity.HeartRate;
import com.fitpolo.support.log.LogModule;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * @Date 2018/4/6
 * @Author wenzheng.liu
 * @Description 复杂数据解析类
 * @ClassPath com.fitpolo.support.utils.ComplexDataParse
 */
public class ComplexDataParse {
    public static DailyStep parseDailyStep(byte[] value, int index) {
        // 日期
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000 + DigitalConver.byte2Int(value[index]),
                DigitalConver.byte2Int(value[index + 1]) - 1,
                DigitalConver.byte2Int(value[index + 2]));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = calendar.getTime();
        String dateStr = sdf.format(date);
        // 步数
        byte[] step = new byte[4];
        System.arraycopy(value, index + 3, step, 0, 4);
        String stepStr = DigitalConver.byteArr2Str(step);

        // 时长
        byte[] duration = new byte[2];
        System.arraycopy(value, index + 7, duration, 0, 2);
        String durationStr = DigitalConver.byteArr2Str(duration);

        // 距离
        byte[] distance = new byte[2];
        System.arraycopy(value, index + 9, distance, 0, 2);
        String distanceStr = new DecimalFormat().format(DigitalConver.byteArr2Int(distance) * 0.1);
        // 卡路里
        byte[] calories = new byte[2];
        System.arraycopy(value, index + 11, calories, 0, 2);
        String caloriesStr = DigitalConver.byteArr2Str(calories);

        DailyStep dailyStep = new DailyStep();
        dailyStep.date = dateStr;
        dailyStep.count = stepStr;
        dailyStep.duration = durationStr;
        dailyStep.distance = distanceStr;
        dailyStep.calories = caloriesStr;
        LogModule.i(dailyStep.toString());
        return dailyStep;
    }


    public static DailyStep parseCurrentStep(byte[] value) {
        if (0xb5 != DigitalConver.byte2Int(value[0])
                || 0x04 != DigitalConver.byte2Int(value[1])
                || 0x0a != DigitalConver.byte2Int(value[2])) {
            return null;
        }
        // 日期
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = calendar.getTime();
        String dateStr = sdf.format(date);
        // 步数
        byte[] step = new byte[4];
        System.arraycopy(value, 3, step, 0, 4);
        String stepStr = DigitalConver.byteArr2Str(step);

        // 时长
        byte[] duration = new byte[2];
        System.arraycopy(value, 7, duration, 0, 2);
        String durationStr = DigitalConver.byteArr2Str(duration);

        // 距离
        byte[] distance = new byte[2];
        System.arraycopy(value, 9, distance, 0, 2);
        String distanceStr = new DecimalFormat().format(DigitalConver.byteArr2Int(distance) * 0.1);
        // 卡路里
        byte[] calories = new byte[2];
        System.arraycopy(value, 11, calories, 0, 2);
        String caloriesStr = DigitalConver.byteArr2Str(calories);

        DailyStep dailyStep = new DailyStep();
        dailyStep.date = dateStr;
        dailyStep.count = stepStr;
        dailyStep.duration = durationStr;
        dailyStep.distance = distanceStr;
        dailyStep.calories = caloriesStr;
        return dailyStep;
    }

    public static DailySleep parseDailySleepIndex(byte[] value, HashMap<Integer, DailySleep> sleepsMap, int index) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Calendar calendar = Calendar.getInstance();
        // 起始时间
        calendar.set(2000 + DigitalConver.byte2Int(value[index + 1]),
                DigitalConver.byte2Int(value[index + 2]) - 1,
                DigitalConver.byte2Int(value[index + 3]),
                DigitalConver.byte2Int(value[index + 4]),
                DigitalConver.byte2Int(value[index + 5]));
        Date startDate = calendar.getTime();
        String startDateStr = sdf.format(startDate);
        // 结束时间
        calendar.set(2000 + DigitalConver.byte2Int(value[index + 6]),
                DigitalConver.byte2Int(value[index + 7]) - 1,
                DigitalConver.byte2Int(value[index + 8]),
                DigitalConver.byte2Int(value[index + 9]),
                DigitalConver.byte2Int(value[index + 10]));
        Date endDate = calendar.getTime();
        String endDateStr = sdf.format(endDate);
        // 深睡
        byte[] deep = new byte[2];
        System.arraycopy(value, index + 11, deep, 0, 2);
        String deepStr = DigitalConver.byteArr2Str(deep);
        // 浅睡
        byte[] light = new byte[2];
        System.arraycopy(value, index + 13, light, 0, 2);
        String lightStr = DigitalConver.byteArr2Str(light);
        // 清醒
        byte[] awake = new byte[2];
        System.arraycopy(value, index + 15, awake, 0, 2);
        String awakeStr = DigitalConver.byteArr2Str(awake);

        // 记录睡眠日期
        String date = new SimpleDateFormat("yyy-MM-dd").format(endDate);

        // 构造睡眠数据
        DailySleep dailySleep = new DailySleep();
        dailySleep.date = date;
        dailySleep.startTime = startDateStr;
        dailySleep.endTime = endDateStr;
        dailySleep.deepDuration = deepStr;
        dailySleep.lightDuration = lightStr;
        dailySleep.awakeDuration = awakeStr;
        dailySleep.records = new ArrayList<>();
        LogModule.i(dailySleep.toString());
        // 暂存睡眠数据，以index为key，以实例为value，方便更新record;
        sleepsMap.put(DigitalConver.byte2Int(value[index]), dailySleep);
        return dailySleep;
    }

    public static void parseDailySleepRecord(byte[] value, HashMap<Integer, DailySleep> mSleepsMap, int index) {
        DailySleep dailySleep = mSleepsMap.get(DigitalConver.byte2Int(value[index]));
        if (dailySleep != null) {
            int len = DigitalConver.byte2Int(value[index + 2]);
            if (dailySleep.records == null) {
                dailySleep.records = new ArrayList<>();
            }
            for (int i = 0; i < len && index + 3 + i < value.length; i++) {
                String hex = DigitalConver.byte2HexString(value[index + 3 + i]);
                // 转换为二进制
                String binary = DigitalConver.hexString2binaryString(hex);
                for (int j = binary.length(); j > 0; ) {
                    j -= 2;
                    String status = binary.substring(j, j + 2);
                    dailySleep.records.add(status);
                }
            }
            LogModule.i(dailySleep.toString());
        }
    }

    public static void parseHeartRate(byte[] value, ArrayList<HeartRate> heartRates) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 3; i++) {
            byte year = value[i * 6 + 2];
            byte month = value[i * 6 + 3];
            byte day = value[i * 6 + 4];
            byte hour = value[i * 6 + 5];
            byte min = value[i * 6 + 6];
            byte heartRateValue = value[i * 6 + 7];
            if (DigitalConver.byte2Int(year) == 0) {
                continue;
            }
            calendar.set(2000 + DigitalConver.byte2Int(year),
                    DigitalConver.byte2Int(month) - 1,
                    DigitalConver.byte2Int(day),
                    DigitalConver.byte2Int(hour),
                    DigitalConver.byte2Int(min));
            Date time = calendar.getTime();
            String heartRateTime = sdf.format(time);
            String heartRateStr = DigitalConver.byte2Int(heartRateValue) + "";
            HeartRate heartRate = new HeartRate();
            heartRate.time = heartRateTime;
            heartRate.value = heartRateStr;
            LogModule.i(heartRate.toString());
            heartRates.add(heartRate);
        }
    }
}
