package com.bytedance.labcv.demo.utils;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bytedance.labcv.demo.ResourceHelper;
import com.bytedance.labcv.effectsdk.library.FileUtils;

import java.io.File;

public class DownloadStickerManager {

    public static boolean unzip(String path, String dst){
        return FileUtils.unzipFile(path, new File(dst));

    }

    public static String getStickerPath(@NonNull final Context mContext,  String dir){
        return ResourceHelper.getDownloadedStickerDir(mContext) + File.separator + dir;

    }

    public static String getLicensePath(String path){
        File file = new File(path);
        File[]files = file.listFiles();
        for (File item : files){
            if (item.isFile() && item.getAbsolutePath().endsWith("licbag")){
                return item.getAbsolutePath();
            }
        }
        return null;
    }

}
