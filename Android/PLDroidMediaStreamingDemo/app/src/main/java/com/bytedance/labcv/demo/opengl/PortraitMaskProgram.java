package com.bytedance.labcv.demo.opengl;

import android.content.Context;

/**
 * 人体分割的绘制句柄
 */
public class PortraitMaskProgram extends MaskProgram {
    public PortraitMaskProgram(Context context, int width, int height) {
        super(context, width, height, FRAGMENT_PORTRAIT);
    }
}
