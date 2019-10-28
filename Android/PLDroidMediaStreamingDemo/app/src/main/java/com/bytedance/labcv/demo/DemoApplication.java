// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.bytedance.labcv.demo;

import android.app.Application;

import com.bytedance.labcv.demo.utils.ToasUtils;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ToasUtils.init(this);

    }
}
