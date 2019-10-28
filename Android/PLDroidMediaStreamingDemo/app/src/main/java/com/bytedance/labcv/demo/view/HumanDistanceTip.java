package com.bytedance.labcv.demo.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import com.bytedance.labcv.effectsdk.BefDistanceInfo;
import com.qiniu.pili.droid.streaming.demo.R;

import java.text.DecimalFormat;

public class HumanDistanceTip extends ResultTip<BefDistanceInfo.BefDistance> {
    private static final String TAG = "HandInfoTip";
    private TextView tvDist;

    private int height = getResources().getDimensionPixelSize(R.dimen.distance_info_height);
    private DecimalFormat df = new DecimalFormat("0.00");

    public HumanDistanceTip(@NonNull Context context) {
        super(context);
        addLayout(context, R.layout.view_distance_info);
        tvDist = findViewById(R.id.tv_dist);
    }

    public HumanDistanceTip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HumanDistanceTip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    void updateInfo(BefDistanceInfo.BefDistance item, int preViewHeight, int previewWidth, int surfaceViewHeight, int surfaceViewWidth) {
        if (null == item) return;
        if (null == tvDist) return;

        tvDist.setText(df.format(item.getDis()));

        Rect rect = getRectInScreenCord(item.getFaceRect().toRect(), preViewHeight, previewWidth, surfaceViewHeight, surfaceViewWidth) ;
        double widthRatio  = surfaceViewWidth * 1.0 /previewWidth;
        double heightRatio  = surfaceViewHeight * 1.0 /preViewHeight;
        double ratio = Math.max(widthRatio, heightRatio);
        int top = new Double(rect.top * ratio).intValue();
        int left = new Double(rect.left * ratio).intValue();
        int bottom = new Double(rect.bottom * ratio).intValue();
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) this.getLayoutParams();
        marginLayoutParams.leftMargin = left;
        marginLayoutParams.topMargin = top >height?(top - height):bottom;
        setLayoutParams(marginLayoutParams);

    }


}
