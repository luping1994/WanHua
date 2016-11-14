package com.suntrans.wanhua;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;

import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by Looney on 2016/10/18.
 */

public class MyApplication extends Application {
    private static int mainTid;
    private static Handler mHandler;
    private static SharedPreferences msharedPreferences;
    private static MyApplication application;
    @Override
    public void onCreate() {
        super.onCreate();
        mainTid=android.os.Process.myTid();
        mHandler=new Handler();
        application=this;
        CrashReport.initCrashReport(this, "900057580", true);

    }

    public static Application getApplication1() {
        return application;
    }
    public static SharedPreferences getSharedPreferences(){
        if (msharedPreferences==null){
            msharedPreferences = getApplication1().getSharedPreferences("config", Context.MODE_PRIVATE);
        }
        return msharedPreferences;
    }
    public static int getMainTid() {
        return mainTid;
    }
    public static Handler getHandler() {
        return mHandler;
    }
}
