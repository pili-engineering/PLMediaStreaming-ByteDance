package com.bytedance.labcv.demo.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.bytedance.labcv.demo.ResourceHelper;
import com.bytedance.labcv.demo.adapter.FaceClusterAdapter;
import com.bytedance.labcv.demo.task.facecluster.FaceClusterMgr;
import com.bytedance.labcv.demo.utils.CommonUtils;
import com.bytedance.labcv.demo.utils.ToasUtils;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.qiniu.pili.droid.streaming.demo.R;


import java.util.List;

import static android.app.Activity.RESULT_OK;

public class FaceClusterFragment extends BaseFeatureFragment implements FaceClusterMgr.ClusterCallback, FaceClusterAdapter.OnItemClickListener {
    private static final int REQUEST_CODE_CHOOSE = 10;
    private View mIvClear;
    private View mIvAdd;
    private RecyclerView mRvFaceList;
    private Button mBtnStart;
    private View mBtnRet;
    private ProgressBar mProgressBar;

    private FaceClusterMgr mFaceClusterMgr;

    private List<List<String>> mClusterResultList;
    private List<String> mChoosePicture;
    private FaceClusterAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getActivity()).inflate(R.layout.face_cluster_layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBtnStart = view.findViewById(R.id.btn_cluster_start);
        mRvFaceList = view.findViewById(R.id.rv_cluster_list);
        mIvClear = view.findViewById(R.id.ll_cluster_clear);
        mIvAdd = view.findViewById(R.id.ll_cluster_add);
        mBtnRet = view.findViewById(R.id.btn_cluster_ret);
        mProgressBar = view.findViewById(R.id.progress);

        mIvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtils.isFastClick()) return;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (intent.resolveActivity(getActivity().getPackageManager())!= null){
                    startChoosePic();
                } else {
                    ToasUtils.show(getString(R.string.ablum_not_supported));
                }

            }
        });

        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.d("cluster farg start click");
                if (CommonUtils.isFastClick()){
                    ToasUtils.show("too fast click");
                    return;
                }
                startCluster();
            }
        });

        mIvClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtils.isFastClick()){
                    ToasUtils.show("too fast click");
                    return;
                }

                cleanData();
            }
        });

        mBtnRet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtils.isFastClick()) return;

                mAdapter.resetCluster();
                v.setVisibility(View.GONE);
            }
        });

        LinearLayoutManager layoutManager = new WrapContentLinearLayoutManager(this.getActivity());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRvFaceList.setLayoutManager(layoutManager);
        mAdapter = new FaceClusterAdapter(getActivity());
        mRvFaceList.setAdapter(mAdapter);
        mFaceClusterMgr = new FaceClusterMgr(getActivity(), 10, ResourceHelper.getLicensePath(getContext()), this );
    }

    private void startCluster() {
        LogUtils.d("cluster fragment start cluster");
        mBtnStart.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setIndeterminate(true);
        mFaceClusterMgr.cluster(mChoosePicture);
    }

    private void cleanData(){
        mFaceClusterMgr.clean();
        mIvAdd.setVisibility(View.VISIBLE);
        mBtnStart.setVisibility(View.GONE);
        mRvFaceList.setVisibility(View.GONE);
        mBtnRet.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mChoosePicture = null;
        mClusterResultList = null;
        mAdapter.clear();
    }

    private void setData(){
        mAdapter.setChooseList(mChoosePicture);
        mIvAdd.setVisibility(View.GONE);
        mRvFaceList.setVisibility(View.VISIBLE);
        mBtnStart.setEnabled(true);
        mBtnStart.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    private void startChoosePic(){

//        Matisse.from(FaceClusterFragment.this)
//                .choose(MimeType.of(JPEG, PNG, BMP, WEBP))
//                .countable(true)
//                .maxSelectable(Integer.MAX_VALUE)
////                .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
////                .gridExpectedSize(60)
////                .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
//                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
//                .thumbnailScale(0.85f)
//                .imageEngine(new GlideEngine())
//                .forResult(REQUEST_CODE_CHOOSE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {

//            LogUtils.d( "Uris: " + Matisse.obtainResult(data));
//            LogUtils.d("Paths: " + Matisse.obtainPathResult(data));
//            LogUtils.e("Use the selected photos with original: "+String.valueOf(Matisse.obtainOriginalState(data)));

//            mChoosePicture = Matisse.obtainPathResult(data);
            setData();
        }
    }

    @Override
    public void onClusterCallback(List<List<String>> result, int clusterNums) {
        mClusterResultList = result;
        mAdapter.setClusterResultList(mClusterResultList, clusterNums);
        mAdapter.setOpenCluserListener(this);
        mBtnStart.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onClusterProcess(int process) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFaceClusterMgr.release();
    }

    @Override
    public void onOpenCluster() {
        mBtnRet.setVisibility(View.VISIBLE);
    }

    /**
     * fix: java.lang.IndexOutOfBoundsException: Inconsistency detected. Invalid view holder adapter positionViewHolder
     */
    public class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) {
            super(context);
        }

        public WrapContentLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public WrapContentLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }
}
