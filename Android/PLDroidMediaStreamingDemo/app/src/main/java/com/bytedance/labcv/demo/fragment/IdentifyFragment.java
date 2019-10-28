package com.bytedance.labcv.demo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.labcv.demo.MainActivity;
import com.bytedance.labcv.demo.adapter.FragmentVPAdapter;
import com.bytedance.labcv.demo.base.IPresenter;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.effect.OnCloseListener;

import java.util.ArrayList;
import java.util.List;

public class IdentifyFragment extends BaseFeatureFragment<IPresenter, IdentifyFragment.IIdentifyCallback>
        implements OnCloseListener {
    private TabLayout tl;
    private ViewPager vp;

    private List<Fragment> mFragmentList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_identify, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tl = view.findViewById(R.id.tl_identify);
        vp = view.findViewById(R.id.vp_identify);

        initVP();
    }

    private void initVP() {
        mFragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        // 添加人脸 Fragment
        mFragmentList.add(new FaceDetectFragment().setCallback(getCallback()));
        titleList.add(getString(R.string.tab_face));
        // 添加人脸 Fragment
        mFragmentList.add(new PetFaceDetectFragment().setCallback(getCallback()));
        titleList.add(getString(R.string.tab_pet_face));
        // 添加手势 Fragment
        mFragmentList.add(new HandDetectFragment().setCallback(getCallback()));
        titleList.add(getString(R.string.tab_hand));
        // 添加人体 Fragment
        mFragmentList.add(new SkeletonDetectFragment().setCallback(getCallback()));
        titleList.add(getString(R.string.tab_body));
        // 添加分割 Fragment
        mFragmentList.add(new SegmentationFragment().setCallback(getCallback()));
        titleList.add(getString(R.string.tab_segmentation));
        // 添加人像对比 Fragment
        mFragmentList.add(new FaceVerifyFragment().setCallback(getCallback()));
        titleList.add(getString(R.string.tab_face_verify));
        mFragmentList.add(new FaceClusterFragment().setCallback(getCallback()));
        titleList.add(getString(R.string.tab_face_cluster));

        // 添加距离检测 Fragment
//        mFragmentList.add(new HumanDistanceFragment().setCallback(getCallback()));
//        titleList.add(getString(R.string.setting_human_dist));

        FragmentVPAdapter adapter = new FragmentVPAdapter(
                getChildFragmentManager(), mFragmentList, titleList);
        vp.setAdapter(adapter);
        vp.setOffscreenPageLimit(mFragmentList.size());
        tl.setupWithViewPager(vp);
    }

    @Override
    public void onClose() {
        for (Fragment f : mFragmentList) {
            if (f instanceof OnCloseListener) {
                ((OnCloseListener) f).onClose();
            }
        }
    }

    public interface IIdentifyCallback extends
            FaceDetectFragment.IFaceCallback,
            PetFaceDetectFragment.IFaceCallback,
            HandDetectFragment.IHandCallBack,
            SkeletonDetectFragment.ISkeletonCallback,
            SegmentationFragment.IPortraitMattingCallback,
            FaceVerifyFragment.IFaceVerifyCallback,
            HumanDistanceFragment.IDistCallback
    { }
}
