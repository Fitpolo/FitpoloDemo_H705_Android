package com.fitpolo.support.entity;

/**
 * @Date 2017/5/14 0014
 * @Author wenzheng.liu
 * @Description 手环闹钟
 * @ClassPath com.fitpolo.support.entity.BandAlarm
 */
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

    @Override
    public String toString() {
        return "BandAlarm{" +
                "time='" + time + '\'' +
                ", state='" + state + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
