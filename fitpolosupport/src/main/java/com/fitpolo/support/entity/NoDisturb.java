package com.fitpolo.support.entity;

/**
 * @Date 2018/4/9
 * @Author wenzheng.liu
 * @Description 勿扰模式
 * @ClassPath com.fitpolo.support.entity.NoDisturb
 */
public class NoDisturb {
    public int noDisturb; // 勿扰模式开关，1：开；0：关；
    public String startTime;// 开始时间，格式：HH:mm;
    public String endTime;// 结束时间，格式：HH:mm;

    @Override
    public String toString() {
        return "NoDisturb{" +
                "noDisturb=" + noDisturb +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }
}
