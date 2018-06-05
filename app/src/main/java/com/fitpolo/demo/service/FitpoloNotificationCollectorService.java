package com.fitpolo.demo.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.fitpolo.support.log.LogModule;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class FitpoloNotificationCollectorService extends NotificationListenerService {

    private static final String WECHAT = "com.tencent.mm";
    private static final String QQ = "com.tencent.mobileqq";
    private static final String WHATSAPP = "com.whatsapp";
    private static final String FACEBOOK = "com.facebook.katana";
    private static final String TWITTER = "com.twitter.android";
    private static final String SKYPE = "com.skype.raider";
    private static final String SNAPCHAT = "com.snapchat.android";
    private static final String LINE = "jp.naver.line.android";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        LogModule.i("onNotificationPosted");
        String packageName = sbn.getPackageName();
        if (WECHAT.equals(packageName)) {
            LogModule.i("收到微信通知");
        } else if (QQ.equals(packageName)) {
            LogModule.i("收到QQ通知");
        } else if (WHATSAPP.equals(packageName)) {
            LogModule.i("收到WhatsApp通知");
        } else if (FACEBOOK.equals(packageName)) {
            LogModule.i("收到Facebook通知");
        } else if (TWITTER.equals(packageName)) {
            LogModule.i("收到Twitter通知");
        } else if (SKYPE.equals(packageName)) {
            LogModule.i("收到Skype通知");
        } else if (SNAPCHAT.equals(packageName)) {
            LogModule.i("收到Snapchat通知");
        } else if (LINE.equals(packageName)) {
            LogModule.i("收到Line通知");
        }
        if (WECHAT.equals(packageName) || QQ.equals(packageName) || WHATSAPP.equals(packageName)
                || FACEBOOK.equals(packageName) || TWITTER.equals(packageName) || SKYPE.equals(packageName)
                || SNAPCHAT.equals(packageName) || LINE.equals(packageName)) {
            Notification notification = sbn.getNotification();
            if (notification == null) {
                return;
            }
            // 当 API > 18 时，使用 extras 获取通知的详细信息
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Bundle extras = notification.extras;
                if (extras != null) {
                    // 获取通知标题
                    String title = extras.getString(Notification.EXTRA_TITLE, "");
                    LogModule.i("通知标题：" + title);
                }
            }
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        LogModule.i("onNotificationRemoved");
        LogModule.i(sbn.getPackageName());
    }

    @Override
    public void onListenerConnected() {
        LogModule.i("onListenerConnected");
    }

    @Override
    public void onCreate() {
        LogModule.i("onCreate");
        super.onCreate();

    }

    @Override
    public void onDestroy() {
        LogModule.i("onDestroy");
        super.onDestroy();
    }
}
