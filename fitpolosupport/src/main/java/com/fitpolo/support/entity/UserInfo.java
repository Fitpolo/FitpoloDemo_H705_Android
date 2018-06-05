package com.fitpolo.support.entity;

/**
 * @Date 2017/5/14 0014
 * @Author wenzheng.liu
 * @Description 个人信息
 * @ClassPath com.fitpolo.support.entity.UserInfo
 */
public class UserInfo {
    public int weight;// 体重
    public int height;// 身高
    public int age;// 年龄
    public int birthdayMonth;// 出生月
    public int birthdayDay;// 出生日
    public int gender;// 性别 男：0；女：1
    public int stepExtent;// 步幅

    @Override
    public String toString() {
        return "UserInfo{" +
                "weight=" + weight +
                ", height=" + height +
                ", age=" + age +
                ", birthdayMonth=" + birthdayMonth +
                ", birthdayDay=" + birthdayDay +
                ", gender=" + gender +
                ", stepExtent=" + stepExtent +
                '}';
    }
}
