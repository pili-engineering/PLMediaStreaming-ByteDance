// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.bytedance.labcv.demo;

import android.app.Application;

import com.bytedance.labcv.demo.utils.ToasUtils;
import com.qiniu.pili.droid.streaming.StreamingEnv;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ToasUtils.init(this);
        StreamingEnv.init(getApplicationContext());
    }
}
