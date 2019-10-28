package com.bytedance.labcv.demo.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

/**
 * 基类 飘在屏幕任意位置的预测结果 可跟随
 * @param <T>
 */
public abstract class ResultTip<T> extends FrameLayout {
    public ResultTip(@NonNull Context context) {
        super(context);
    }

    public ResultTip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ResultTip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addLayout(@NonNull Context context, int layoutId) {
        LayoutInflater.from(context).inflate(layoutId, this);


    }

    abstract void updateInfo(T info, int preViewHeight, int previewWidth, int surfaceViewHeight, int surfaceViewWidth);

    /**
     * 因相机输出比例和屏幕比例不一致会执行纹理裁剪，裁剪后框的坐标也要对应裁剪
     * @param rect
     * @param preViewHeight
     * @param previewWidth
     * @param surfaceViewHeight
     * @param surfaceViewWidth
     * @return
     */
    Rect getRectInScreenCord(Rect rect, int preViewHeight, int previewWidth, int surfaceViewHeight, int surfaceViewWidth) {
        float ratio1 =  previewWidth*1.0f / surfaceViewWidth;
        float ratio2 =  preViewHeight*1.0f / surfaceViewHeight;
        if (ratio1 < ratio2) {
            int offset = (preViewHeight - previewWidth* surfaceViewHeight /surfaceViewWidth)/2;
            return new Rect(rect.left, rect.top - offset,rect.right, rect.bottom - offset);
        } else {
            int offset = (previewWidth - preViewHeight* surfaceViewWidth /surfaceViewHeight)/2;
            return new Rect(rect.left - offset, rect.top,rect.right- offset,  rect.bottom);
        }



    }



}
