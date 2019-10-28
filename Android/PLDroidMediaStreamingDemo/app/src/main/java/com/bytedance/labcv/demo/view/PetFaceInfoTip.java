package com.bytedance.labcv.demo.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.TextView;

import com.bytedance.labcv.effectsdk.BefPetFaceInfo;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.qiniu.pili.droid.streaming.demo.R;

public class PetFaceInfoTip  extends ResultTip<BefPetFaceInfo.PetFace> {
    private static final String TAG = "PetFaceInfoTip";
    public final int[] PET_FACE_TYPES = {R.string.cat, R.string.dog,R.string.human,R.string.unknown};
    private TextView tvPetFaceType;

    private SparseArray<TextView> mActionMap = null;
    private TextView tvLeftEye;
    private TextView tvMouth;
    private TextView tvRightEye;

    private int height = getResources().getDimensionPixelSize(R.dimen.pet_face_info_height);
    private int mExpressionOnColor;
    private int mExpressionOffColor;

    public PetFaceInfoTip(@NonNull Context context) {
        super(context);
        addLayout(context, R.layout.view_pet_face_info);
        tvPetFaceType = findViewById(R.id.tv_pet_face_type);
        tvLeftEye = findViewById(R.id.tv_left_eye_pet_face_info);
        tvRightEye = findViewById(R.id.tv_right_eye_pet_face_info);
        tvMouth = findViewById(R.id.tv_mouth_pet_face_info);

        if (mActionMap == null) {
            mActionMap = new SparseArray<>();
            mActionMap.put(BytedEffectConstants.PetFaceAction.BEF_LEFT_EYE_PET_FACE, tvLeftEye);
            mActionMap.put(BytedEffectConstants.PetFaceAction.BEF_RIGHT_EYE_PET_FACE, tvRightEye);
            mActionMap.put(BytedEffectConstants.PetFaceAction.BEF_MOUTH_PET_FACE, tvMouth);

        }
        mExpressionOffColor = ActivityCompat.getColor(getContext(), R.color.colorGrey);
        mExpressionOnColor = ActivityCompat.getColor(getContext(), R.color.colorWhite);
        for (int i = 0; i < mActionMap.size(); i++) {
            mActionMap.valueAt(i).setTextColor(mExpressionOffColor);
        }
    }

    public PetFaceInfoTip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PetFaceInfoTip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    void updateInfo(BefPetFaceInfo.PetFace item, int preViewHeight, int previewWidth, int surfaceViewHeight, int surfaceViewWidth) {
        if (null == item) return;

        int type = item.getType();
        if (type > PET_FACE_TYPES.length){
            type = PET_FACE_TYPES.length;
        }
        tvPetFaceType.setText(PET_FACE_TYPES[type - 1]);

        int action = item.getAction();
        for (int i = 0; i < mActionMap.size(); i++) {
            if ((mActionMap.keyAt(i) & action) != 0) {
                mActionMap.get(mActionMap.keyAt(i)).setTextColor(mExpressionOnColor);
            } else {
                mActionMap.get(mActionMap.keyAt(i)).setTextColor(mExpressionOffColor);
            }
        }

        Rect rect = getRectInScreenCord(item.getRect().toRect(), preViewHeight, previewWidth, surfaceViewHeight, surfaceViewWidth) ;
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