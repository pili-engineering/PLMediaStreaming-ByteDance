package com.bytedance.labcv.demo.contract;

import com.bytedance.labcv.demo.base.BasePresenter;
import com.bytedance.labcv.demo.base.IView;
import com.bytedance.labcv.demo.model.FilterItem;

import java.util.List;

/**
 * Created by QunZhang on 2019-07-21 12:22
 */
public interface FilterContract {
    interface View extends IView {

    }

    abstract class Presenter extends BasePresenter<View> {
        public abstract List<FilterItem> getItems();
    }
}
