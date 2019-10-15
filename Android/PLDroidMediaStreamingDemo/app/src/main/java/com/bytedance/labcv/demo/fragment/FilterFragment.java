// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.bytedance.labcv.demo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.labcv.demo.adapter.FilterRVAdapter;
import com.bytedance.labcv.demo.contract.FilterContract;
import com.bytedance.labcv.demo.contract.presenter.FilterPresenter;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.effect.OnCloseListener;

import java.io.File;

/**
 * 滤镜
 */
public class FilterFragment extends BaseFeatureFragment<FilterContract.Presenter, FilterFragment.IFilterCallback>
        implements FilterRVAdapter.OnItemClickListener,
        OnCloseListener, FilterContract.View {
    private RecyclerView rv;

    public interface IFilterCallback {
        void onFilterSelected(File file);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rv = (RecyclerView) View.inflate(getContext(), R.layout.fragment_filter, null);
        return rv;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setPresenter(new FilterPresenter());

        FilterRVAdapter adapter = new FilterRVAdapter(mPresenter.getItems(), this);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }

    @Override
    public void onItemClick(File file) {
        getCallback().onFilterSelected(file);
    }

    @Override
    public void onClose() {
        ((FilterRVAdapter)rv.getAdapter()).setSelect(0);
    }
}
