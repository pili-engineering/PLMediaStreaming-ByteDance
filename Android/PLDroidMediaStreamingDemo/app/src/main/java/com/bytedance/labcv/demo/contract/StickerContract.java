package com.bytedance.labcv.demo.contract;

import com.bytedance.labcv.demo.base.BasePresenter;
import com.bytedance.labcv.demo.base.IView;
import com.bytedance.labcv.demo.model.StickerItem;

import java.util.List;

/**
 * Created by QunZhang on 2019-07-21 12:24
 */
public interface StickerContract {
    interface View extends IView {

    }

    abstract class Presenter extends BasePresenter<View> {
        public abstract List<StickerItem> getItems();
    }
}
