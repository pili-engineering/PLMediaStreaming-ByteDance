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
 * 人体
 */
public class SkeletonDetectFragment
        extends BaseFeatureFragment<IPresenter, SkeletonDetectFragment.ISkeletonCallback>
        implements View.OnClickListener, OnCloseListener {
    private ButtonView bvSkeleton;

    public interface ISkeletonCallback {
        void skeletonDetectOn(boolean on);
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getActivity()).inflate(R.layout.fragment_skeleton, null);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bvSkeleton = view.findViewById(R.id.bv_skeleton);

        bvSkeleton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (CommonUtils.isFastClick()) {
            ToasUtils.show("too fast click");
            return;
        }
        switch (v.getId()) {
            case R.id.bv_skeleton:
                boolean isSkeletonOn = !bvSkeleton.isOn();
                getCallback().skeletonDetectOn(isSkeletonOn);
                bvSkeleton.change(isSkeletonOn);
                break;
        }
    }

    @Override
    public void onClose() {
        getCallback().skeletonDetectOn(false);

        bvSkeleton.off();
    }
}
