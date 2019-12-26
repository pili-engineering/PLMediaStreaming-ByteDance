package com.bytedance.labcv.demo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.labcv.demo.MainActivity;
import com.qiniu.pili.droid.streaming.demo.R;
import com.bytedance.labcv.demo.adapter.ButtonViewRVAdapter;
import com.bytedance.labcv.demo.contract.ItemGetContract;
import com.bytedance.labcv.demo.contract.presenter.ItemGetPresenter;
import com.bytedance.labcv.demo.model.ButtonItem;

import java.util.List;

import static com.bytedance.labcv.demo.contract.ItemGetContract.MASK;

public class BeautyFaceFragment extends BaseFeatureFragment<ItemGetContract.Presenter, BeautyFaceFragment.IBeautyCallBack>
        implements EffectFragment.IProgressCallback, MainActivity.OnCloseListener, ButtonViewRVAdapter.OnItemClickListener, ItemGetContract.View {
    private RecyclerView rv;
    private int mType;
    private MainActivity.ICheckAvailableCallback mCheckAvailableCallback;

    public interface IBeautyCallBack {
        void onBeautySelect(ButtonItem item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getActivity()).inflate(R.layout.fragment_beauty, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setPresenter(new ItemGetPresenter());

        rv = view.findViewById(R.id.rv_beauty);
        List<ButtonItem> items = mPresenter.getItems(mType);
        ButtonViewRVAdapter adapter = new ButtonViewRVAdapter(items, this);
        adapter.setCheckAvailableCallback(mCheckAvailableCallback);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }

    public BeautyFaceFragment setType(final int type) {
        mType = type;
        return this;
    }

    public BeautyFaceFragment setCheckAvailableCallback(MainActivity.ICheckAvailableCallback checkAvailableCallback) {
        mCheckAvailableCallback = checkAvailableCallback;
        return this;
    }

    @Override
    public void onItemClick(ButtonItem item) {
        getCallback().onBeautySelect(item);
    }

    @Override
    public void onProgress(float progress) {
        if (rv == null || rv.getAdapter() == null) return;
        ((ButtonViewRVAdapter)rv.getAdapter()).onProgress(progress);
    }

    @Override
    public void onProgress(float progress, int id) {
        if (rv == null || rv.getAdapter() == null) return;
        ((ButtonViewRVAdapter)rv.getAdapter()).onProgress(progress, id);
    }

    @Override
    public int getSelect() {
        return ((ButtonViewRVAdapter)rv.getAdapter()).getSelect();
    }

    @Override
    public void setSelect(int select) {
        ((ButtonViewRVAdapter)rv.getAdapter()).setSelect(select);
    }

    @Override
    public void setSelectItem(int id) {
        ((ButtonViewRVAdapter)rv.getAdapter()).setSelectItem(id);
    }

    @Override
    public void onClose() {
        ((ButtonViewRVAdapter)rv.getAdapter()).onClose();
    }
}
