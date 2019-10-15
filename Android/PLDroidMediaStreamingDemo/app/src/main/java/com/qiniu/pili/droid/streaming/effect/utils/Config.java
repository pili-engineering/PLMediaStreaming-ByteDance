package com.qiniu.pili.droid.streaming.effect.utils;

import android.os.Environment;

public class Config {
    public static final String SDCARD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String DEFAULT_CACHE_DIR = SDCARD_DIR + "/PLDroidPlayer";
    public static String ROOM_NUMBER = "1234";
}
