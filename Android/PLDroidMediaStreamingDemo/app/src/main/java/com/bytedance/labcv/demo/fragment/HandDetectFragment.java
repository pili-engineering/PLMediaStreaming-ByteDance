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
 * 手势
 */
public class HandDetectFragment extends BaseFeatureFragment<IPresenter, HandDetectFragment.IHandCallBack>
        implements View.OnClickListener, OnCloseListener {
    private ButtonView bvHand;

    public interface IHandCallBack {
        void handDetectOn(boolean flag);

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getActivity()).inflate(R.layout.fragment_hand_detect, null);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bvHand = view.findViewById(R.id.bv_hand);

        bvHand.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (CommonUtils.isFastClick()) {
            ToasUtils.show("too fast click");
            return;
        }
        switch (v.getId()) {
            case R.id.bv_hand:
                boolean isOn = !bvHand.isOn();
                getCallback().handDetectOn(isOn);
                bvHand.change(isOn);
                break;
        }
    }

    @Override
    public void onClose() {
        getCallback().handDetectOn(false);

        bvHand.off();
    }
}
