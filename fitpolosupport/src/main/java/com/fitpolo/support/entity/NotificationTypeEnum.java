package com.fitpolo.support.entity;

/**
 * @Date 2018/4/12
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.fitpolo.support.entity.NotificationTypeEnum
 */
public enum NotificationTypeEnum {
    WECHAT("微信", "com.tencent.mm"),
    QQ("QQ", "com.tencent.mobileqq"),
    QQHD("QQHD", "com.tencent.minihd.qq"),
    WHATSAPP("WHATSAPP", "com.whatsapp"),
    FACEBOOK("FACEBOOK", "com.facebook.orca"),
    TWITTER("TWITTER", "com.twitter.android"),
    SKYPE("SKYPE", "com.skype.raider"),
    SNAPCHAT("SNAPCHAT", "com.snapchat.android"),
    LINE("LINE", "jp.naver.line.android"),;

    private String notificationName;
    private String packageName;

    NotificationTypeEnum(String notificationName, String packageName) {
        this.notificationName = notificationName;
        this.packageName = packageName;
    }

    public static NotificationTypeEnum fromPackageName(String packageName) {
        for (NotificationTypeEnum notificationTypeEnum : NotificationTypeEnum.values()) {
            if (notificationTypeEnum.getPackageName().equals(packageName)) {
                return notificationTypeEnum;
            }
        }
        return null;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getNotificationName() {
        return notificationName;
    }

    public void setNotificationName(String notificationName) {
        this.notificationName = notificationName;
    }
}
