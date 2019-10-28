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

public class PetFaceDetectFragment  extends BaseFeatureFragment<IPresenter, PetFaceDetectFragment.IFaceCallback>
        implements View.OnClickListener, OnCloseListener {
    // 关键点跟踪
    private ButtonView bvFace;

    /**
     * 宠物脸相关设置回调接口，用于和FeatureBoardFragment通信
     */
    public interface IFaceCallback {
        // 宠物脸检测
        void petFaceOn(boolean flag);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getActivity()).inflate(R.layout.fragment_pet_face_detect, null);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bvFace = view.findViewById(R.id.bv_face);

        bvFace.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (CommonUtils.isFastClick()) {
            ToasUtils.show("too fast click");
            return;
        }
        switch (v.getId()) {
            case R.id.bv_face:
                boolean is106On = !bvFace.isOn();
                bvFace.change(is106On);
                getCallback().petFaceOn(is106On);
                break;
        }
    }

    @Override
    public void onClose() {
        getCallback().petFaceOn(false);

        bvFace.off();
    }
}

