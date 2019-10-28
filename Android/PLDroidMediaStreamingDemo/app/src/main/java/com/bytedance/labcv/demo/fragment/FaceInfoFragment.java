package com.bytedance.labcv.demo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytedance.labcv.demo.MainActivity;
import com.bytedance.labcv.effectsdk.BefFaceInfo;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.effect.OnCloseListener;

import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAction.BEF_BROW_RAISE;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAction.BEF_EYE_BLINK;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAction.BEF_HEAD_NOD;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAction.BEF_HEAD_SHAKE;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAction.BEF_MOUTH_AH;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAction.BEF_MOUTH_POUT;

;

public class FaceInfoFragment extends AppCompatDialogFragment implements OnCloseListener {
    private TextView tvBlinkEye;
    private TextView tvShakeHead;
    private TextView tvNod;
    private TextView tvOpenMouth;
    private TextView tvPoutMouth;
    private TextView tvRaiseEyebrow;

    private TextView tvYaw;
    private TextView tvPitch;
    private TextView tvRoll;
    private TextView tvFaceCount;
    private TextView tvTotalFace;

    private LinearLayout llAttr;
    private TextView tvAge;
    private TextView tvGender;
    private TextView tvExpress;
    private TextView tvBeauty;
    private TextView tvHappiness;
    private TextView tvRace;

    private int mExpressionOnColor;
    private int mExpressionOffColor;

    private SparseArray<TextView> mActionMap = null;
    public final int[] mExpression = {R.string.anger,R.string.nausea,R.string.fear,R.string.happy,R.string.sad,R.string.surprise,R.string.poker_face};
    public final int[] mRace = {R.string.white,R.string.yellow,R.string.brown, R.string.black};

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return View.inflate(getContext(), R.layout.fragment_face_info, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvFaceCount = view.findViewById(R.id.tv_cur_face_face_info);
        tvTotalFace = view.findViewById(R.id.tv_total_verify_face_info);
        tvYaw = view.findViewById(R.id.tv_yaw_face_info);
        tvPitch = view.findViewById(R.id.tv_pitch_face_info);
        tvRoll = view.findViewById(R.id.tv_roll_face_info);

        tvBlinkEye = view.findViewById(R.id.tv_blink_eye_face_info);
        tvShakeHead = view.findViewById(R.id.tv_shake_head_face_info);
        tvNod = view.findViewById(R.id.tv_nod_face_info);
        tvOpenMouth = view.findViewById(R.id.tv_open_mouth_face_info);
        tvPoutMouth = view.findViewById(R.id.tv_pout_face_info);
        tvRaiseEyebrow = view.findViewById(R.id.tv_raise_eyebrow_face_info);

        llAttr = view.findViewById(R.id.ll_attr_face_info);
        tvAge = view.findViewById(R.id.tv_age_face_info);
        tvGender = view.findViewById(R.id.tv_gender_face_info);
        tvBeauty = view.findViewById(R.id.tv_beauty_face_info);
        tvRace = view.findViewById(R.id.tv_race_face_info);

        tvExpress = view.findViewById(R.id.tv_expression);
        tvHappiness = view.findViewById(R.id.tv_happiness_face_info);

        if (mActionMap == null) {
            mActionMap = new SparseArray<>();
            mActionMap.put(BEF_EYE_BLINK, tvBlinkEye);
            mActionMap.put(BEF_MOUTH_AH, tvOpenMouth);
            mActionMap.put(BEF_HEAD_SHAKE, tvShakeHead);
            mActionMap.put(BEF_HEAD_NOD, tvNod);
            mActionMap.put(BEF_BROW_RAISE, tvRaiseEyebrow);
            mActionMap.put(BEF_MOUTH_POUT, tvPoutMouth);
        }

        mExpressionOffColor = ActivityCompat.getColor(getContext(), R.color.colorGrey);
        mExpressionOnColor = ActivityCompat.getColor(getContext(), R.color.colorWhite);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void resetProperty(){
        tvFaceCount.setText("0");
        tvYaw.setText("0.0");
        tvPitch.setText("0.0");
        tvRoll.setText("0.0");

        for (int i = 0; i < mActionMap.size(); i++) {
            mActionMap.valueAt(i).setTextColor(mExpressionOffColor);
        }

        if (llAttr.getVisibility() == View.VISIBLE ) {
            tvAge.setText("");
            tvGender.setText("");
            tvBeauty.setText("");
            tvRace.setText("");
            tvExpress.setText("");
            tvHappiness.setText("");
        } else {
            llAttr.setVisibility(View.GONE);
        }
    }

    public void updateProperty(BefFaceInfo befFaceInfo, boolean isFaceAttrOn) {
        if (this.isVisible()) {
            // while  license error/ detect error will get a null faceinfo
            if ( befFaceInfo != null) {
                int faceCount = befFaceInfo.getFace106s().length;
                if (faceCount >= 1) {
                    tvTotalFace.setText(""+ (befFaceInfo.getFace106s()[0].getID() + 1));
                    tvFaceCount.setText(""+faceCount);
                    BefFaceInfo.Face106 face106 = befFaceInfo.getFace106s()[0];
                    tvYaw.setText(String.format("%.1f", face106.getYaw()));
                    tvPitch.setText(String.format("%.1f", face106.getPitch()));
                    tvRoll.setText(String.format("%.1f", face106.getRoll()));
                    int action = face106.getAction();
                    for (int i = 0; i < mActionMap.size(); i++) {
                        if ((mActionMap.keyAt(i) & action) != 0) {
                            mActionMap.get(mActionMap.keyAt(i)).setTextColor(mExpressionOnColor);
                        } else {
                            mActionMap.get(mActionMap.keyAt(i)).setTextColor(mExpressionOffColor);
                        }
                    }
                    int attrCount = befFaceInfo.getAttris().length;
                    if (isFaceAttrOn) {
                        llAttr.setVisibility(View.VISIBLE);
                        if (attrCount > 0) {
                            BefFaceInfo.FaceAttri attr = befFaceInfo.getAttris()[0];
                            llAttr.setVisibility(View.VISIBLE);
                            tvAge.setText(String.format("%.0f", attr.getAge()));
                            tvGender.setText((attr.getBoy_prob() > 0.5f ? getString(R.string.male):getString(R.string.female)));
                            tvBeauty.setText(String.format("%.0f", attr.getAttractive()));
                            int racialType = attr.getRacial_type();
                            if ( attr.getRacial_type() > mRace.length || attr.getRacial_type() < 0) {
                                racialType = 0;
                            }
                            tvRace.setText(getString(mRace[racialType]));
                            int exptype = attr.getExpression_type();
                            if (exptype > 6 || exptype < 0)
                                exptype = 6;
                            tvExpress.setText(getString(mExpression[exptype]));
                            tvHappiness.setText(String.format("%.0f", attr.getHappy_score()));
                        }
                    } else {
                        llAttr.setVisibility(View.GONE);
                    }
                } else {
                    resetProperty();
                }
            }
        }
    }

    @Override
    public void onClose() {
        resetProperty();
        llAttr.setVisibility(View.GONE);
    }
}
