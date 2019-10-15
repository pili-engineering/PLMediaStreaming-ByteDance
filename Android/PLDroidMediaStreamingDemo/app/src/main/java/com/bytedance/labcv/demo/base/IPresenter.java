package com.bytedance.labcv.demo.base;

public interface IPresenter {
    void attachView(IView view);
    void detachView();
}
