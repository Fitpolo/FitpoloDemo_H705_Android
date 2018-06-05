package com.fitpolo.support.entity;

/**
 * @Date 2018/4/9
 * @Author wenzheng.liu
 * @Description 翻腕亮屏
 * @ClassPath com.fitpolo.support.entity.AutoLighten
 */
public class AutoLighten {
    public int autoLighten; // 翻腕亮屏开关，1：开；0：关；
    public String startTime;// 开始时间，格式：HH:mm;
    public String endTime;// 结束时间，格式：HH:mm;


    @Override
    public String toString() {
        return "AutoLighten{" +
                "autoLighten=" + autoLighten +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }
}
