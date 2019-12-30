package com.bytedance.labcv.demo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.labcv.demo.MainActivity;
import com.qiniu.pili.droid.streaming.demo.R;
import com.bytedance.labcv.demo.adapter.StickerRVAdapter;
import com.bytedance.labcv.demo.contract.StickerContract;
import com.bytedance.labcv.demo.contract.presenter.StickerPresenter;
import com.bytedance.labcv.demo.model.StickerItem;
import com.bytedance.labcv.demo.utils.ToasUtils;

import java.io.File;

public class StickerFragment extends BaseFeatureFragment<StickerContract.Presenter, StickerFragment.IStickerCallback>
        implements StickerRVAdapter.OnItemClickListener, MainActivity.OnCloseListener, StickerContract.View {
    private RecyclerView rv;
    private int mType;
    private MainActivity.ICheckAvailableCallback mCheckAvailableCallback;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rv = (RecyclerView) inflater.inflate(R.layout.fragment_sticker, container, false);
        return rv;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setPresenter(new StickerPresenter());

        StickerRVAdapter adapter = new StickerRVAdapter(mPresenter.getItems(mType), this);
        adapter.setCheckAvailableCallback(mCheckAvailableCallback);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 4));
        rv.setAdapter(adapter);
    }

    public StickerFragment setType(int type) {
        mType = type;
        return this;
    }

    public StickerFragment setCheckAvailableCallback(MainActivity.ICheckAvailableCallback callback) {
        mCheckAvailableCallback = callback;
        return this;
    }

    public void setSelectItem(String sticker) {
        ((StickerRVAdapter)rv.getAdapter()).setSelectItem(sticker);
    }

    public void recoverState(String sticker) {
        setSelectItem(sticker);
        getCallback().onStickerSelected(new File(sticker));
    }

    @Override
    public void onItemClick(StickerItem item) {
        if (item.hasTip()) {
            ToasUtils.show(item.getTip());
        }
        if (getCallback() == null) {
            return;
        }
        getCallback().onStickerSelected(item.getResource() == null ? null : new File(item.getResource()));
    }

    @Override
    public void onClose() {
        ((StickerRVAdapter)rv.getAdapter()).setSelect(0);
    }

    public interface IStickerCallback {
        void onStickerSelected(File file);
    }
}
