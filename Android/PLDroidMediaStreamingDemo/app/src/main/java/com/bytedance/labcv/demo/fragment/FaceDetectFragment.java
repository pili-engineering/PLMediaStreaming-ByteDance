package com.bytedance.labcv.demo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.labcv.demo.MainActivity;
import com.bytedance.labcv.demo.base.IPresenter;
import com.bytedance.labcv.demo.utils.CommonUtils;
import com.bytedance.labcv.demo.utils.ToasUtils;
import com.bytedance.labcv.demo.view.ButtonView;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.effect.OnCloseListener;

/**
 * 人脸Fragment
 */
public class FaceDetectFragment extends BaseFeatureFragment<IPresenter, FaceDetectFragment.IFaceCallback>
        implements View.OnClickListener, OnCloseListener {
    // 106关键点跟踪
    private ButtonView bv106;
    // 280关键点检测
    private ButtonView bv280;
    // 人脸属性
    private ButtonView bvAttr;

    /**
     * 人脸相关设置回调接口，用于和FeatureBoardFragment通信
     */
    public interface IFaceCallback {
        // 人脸检测
        void face106On(boolean flag);
        // 人脸属性
        void faceAttrOn(boolean flag);
        // 人脸280
        void faceExtraOn(boolean flag);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getActivity()).inflate(R.layout.fragment_face_detect, null);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bv106 = view.findViewById(R.id.bv_face);
        bv280 = view.findViewById(R.id.bv_280_face);
        bvAttr = view.findViewById(R.id.bv_attr_face);

        bv106.setOnClickListener(this);
        bv280.setOnClickListener(this);
        bvAttr.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (CommonUtils.isFastClick()) {
            ToasUtils.show("too fast click");
            return;
        }
        switch (v.getId()) {
            case R.id.bv_face:
                if (bv106.isOn()) {
                    if (bvAttr.isOn()) {
                        getCallback().faceAttrOn(false);
                        bvAttr.off();
                    }
                    if (bv280.isOn()) {
                        getCallback().faceExtraOn(false);
                        bv280.off();
                    }
                }

                boolean is106On = !bv106.isOn();
                bv106.change(is106On);
                getCallback().face106On(is106On);
                break;
            case R.id.bv_280_face:
                if (!bv106.isOn()) {
                    ToasUtils.show(getString(R.string.open_face106_fist));
                    return;
                };

                boolean is280On = !bv280.isOn();
                getCallback().faceExtraOn(is280On);
                bv280.change(is280On);
                break;
            case R.id.bv_attr_face:
                if (!bv106.isOn()) {
                    ToasUtils.show(getString(R.string.open_face106_fist));
                    return;
                };

                boolean isAttrOn = !bvAttr.isOn();
                getCallback().faceAttrOn(isAttrOn);
                bvAttr.change(isAttrOn);
                break;
        }
    }

    @Override
    public void onClose() {
        getCallback().face106On(false);
        getCallback().faceAttrOn(false);
        getCallback().faceExtraOn(false);

        bv106.off();
        bv280.off();
        bvAttr.off();
    }
}
