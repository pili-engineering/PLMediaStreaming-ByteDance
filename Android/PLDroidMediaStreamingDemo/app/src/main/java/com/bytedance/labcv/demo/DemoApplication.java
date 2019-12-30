// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.bytedance.labcv.demo;

import android.app.Application;

import com.bytedance.labcv.demo.utils.ToasUtils;
//import com.tencent.bugly.crashreport.CrashReport;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ToasUtils.init(this);
//        CrashReport.initCrashReport(getApplicationContext(), "2f0fc1f6c2", true);
    }
}
