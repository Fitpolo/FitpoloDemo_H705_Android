package com.fitpolo.support.entity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @Date 2017/5/15
 * @Author wenzheng.liu
 * @Description 记步数据
 * @ClassPath com.fitpolo.support.entity.DailyStep
 */
public class DailyStep implements Comparable<DailyStep> {
    public String date;// 日期，yyyy-MM-dd
    public String count;// 步数
    public String duration;// 运动时间
    public String distance;// 运动距离
    public String calories;// 运动消耗卡路里

    @Override
    public String toString() {
        return "DailyStep{" +
                "date='" + date + '\'' +
                ", count='" + count + '\'' +
                ", duration='" + duration + '\'' +
                ", distance='" + distance + '\'' +
                ", calories='" + calories + '\'' +
                '}';
    }

    public Calendar strDate2Calendar(String strDate, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            Date date = sdf.parse(strDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int compareTo(DailyStep another) {
        Calendar calendar = strDate2Calendar(date, "yyyy-MM-dd");
        Calendar anotherCalendar = strDate2Calendar(another.date, "yyyy-MM-dd");
        if (calendar.getTime().getTime() > anotherCalendar.getTime().getTime()) {
            return 1;
        }
        if (calendar.getTime().getTime() < anotherCalendar.getTime().getTime()) {
            return -1;
        }
        return 0;
    }
}
