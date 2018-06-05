package com.fitpolo.support.entity;

/**
 * @Date 2017/5/14 0014
 * @Author wenzheng.liu
 * @Description 久坐提醒
 * @ClassPath com.fitpolo.support.entity.SitAlert
 */
public class SitAlert {
    public int alertSwitch; // 久坐提醒开关，1：开；0：关；
    public String startTime;// 开始时间，格式：HH:mm;
    public String endTime;// 结束时间，格式：HH:mm;

    @Override
    public String toString() {
        return "SitAlert{" +
                "alertSwitch=" + alertSwitch +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }
}
