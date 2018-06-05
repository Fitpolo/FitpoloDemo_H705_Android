package com.fitpolo.support.entity;

import java.util.List;

/**
 * @Date 2017/5/15
 * @Author wenzheng.liu
 * @Description 睡眠数据
 * @ClassPath com.fitpolo.support.entity.DailySleep
 */
public class DailySleep {
    public String date;// 日期，yyyy-MM-dd
    public String startTime;// 开始时间，yyyy-MM-dd HH:mm
    public String endTime;// 结束时间，yyyy-MM-dd HH:mm
    public String deepDuration;// 深睡时长，单位min
    public String lightDuration;// 浅睡时长，单位min
    public String awakeDuration;// 清醒时长，单位min
    public List<String> records;// 睡眠记录


    @Override
    public String toString() {
        return "DailySleep{" +
                "date='" + date + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", deepDuration='" + deepDuration + '\'' +
                ", lightDuration='" + lightDuration + '\'' +
                ", awakeDuration='" + awakeDuration + '\'' +
                ", record='" + records + '\'' +
                '}';
    }
}
