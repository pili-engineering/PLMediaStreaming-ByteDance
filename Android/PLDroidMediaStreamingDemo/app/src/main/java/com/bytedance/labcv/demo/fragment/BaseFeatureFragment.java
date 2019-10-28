package com.bytedance.labcv.demo.fragment;

import com.bytedance.labcv.demo.base.BaseFragment;
import com.bytedance.labcv.demo.base.IPresenter;

/**
 * 每个功能fragemnt的基类
 * @param <T>
 */
public abstract class BaseFeatureFragment<T extends IPresenter, Callback> extends BaseFragment<T> {
    private Callback mCallback;

    public BaseFeatureFragment setCallback(Callback t){
        this.mCallback =t;
        return this;
    }

    public Callback getCallback() {
        return mCallback;
    }
}
