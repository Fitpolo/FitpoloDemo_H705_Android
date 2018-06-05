package com.fitpolo.support.entity;


public enum NotifyEnum {
    PHONE_CALL(0X00),
    SMS(0X01),
    WECHAT(0X02),
    QQ(0X03),
    WHATSAPP(0X04),
    FACEBOOK(0X05),
    TWITTER(0X06),
    SKYPE(0X07),
    SNAPCHAT(0X08),
    LINE(0X09),
    ;


    private int notifyType;
    private String firmwareName;

    NotifyEnum(int notifyType) {
        this.notifyType = notifyType;
    }


    public int getNotifyType() {
        return notifyType;
    }
}
