package com.bytedance.labcv.demo.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytedance.labcv.demo.MainActivity;
import com.bytedance.labcv.demo.adapter.FragmentVPAdapter;
import com.bytedance.labcv.demo.adapter.OnPageChangeListenerAdapter;
import com.bytedance.labcv.demo.contract.EffectContract;
import com.bytedance.labcv.demo.contract.presenter.EffectPresenter;
import com.bytedance.labcv.demo.model.ButtonItem;
import com.bytedance.labcv.demo.model.ComposerNode;
import com.bytedance.labcv.demo.view.ProgressBar;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.effect.OnCloseListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bytedance.labcv.demo.contract.ItemGetContract.MASK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_BODY;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_BODY_LONG_LEG;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_BODY_THIN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_SHARPEN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_SMOOTH;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_WHITEN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_CHEEK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_CHIN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_DARK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_DECREE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_EYE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_EYE_ROTATE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_FACE_CUT;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_FACE_OVERALL;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_FACE_SMALL;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_FOREHEAD;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_JAW;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_MOUTH_SMILE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_MOUTH_ZOOM;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_NOSE_LEAN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_NOSE_LONG;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_CLOSE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_FILTER;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_OPTION;

public class EffectFragment extends BaseFeatureFragment<EffectContract.Presenter, EffectFragment.IEffectCallback>
        implements OnCloseListener, MakeupOptionFragment.IMakeupOptionCallback, EffectContract.View {
    public static final int POSITION_BEAUTY = 0;
    public static final int POSITION_RESHAPE = 1;
    public static final int POSITION_BODY = 2;
    public static final int POSITION_MAKEUP = 3;
    public static final int POSITION_FILTER = 4;

    public static final String TAG_MAKEUP_OPTION_FRAGMENT = "makeup_option";
    public static final int ANIMATION_DURATION = 400;

    public static final float NO_VALUE = -1F;
    public static final int NO_POSITION = -1;
    public static final Map<Integer, Float> DEFAULT_VALUE;
    static {
        @SuppressLint("UseSparseArrays") Map<Integer, Float> map = new HashMap<>();
        // 美颜
        map.put(TYPE_BEAUTY_FACE_SMOOTH, 0.4F);
        map.put(TYPE_BEAUTY_FACE_WHITEN, 0.4F);
        map.put(TYPE_BEAUTY_FACE_SHARPEN, 0.4F);
        // 美型
        map.put(TYPE_BEAUTY_RESHAPE_FACE_OVERALL, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_FACE_SMALL, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_FACE_CUT, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_EYE, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_EYE_ROTATE, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_CHEEK, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_JAW, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_NOSE_LEAN, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_NOSE_LONG, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_CHIN, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_FOREHEAD, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_MOUTH_ZOOM, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_MOUTH_SMILE, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_DECREE, 0.4F);
        map.put(TYPE_BEAUTY_RESHAPE_DARK, 0.4F);
        map.put(TYPE_BEAUTY_BODY_THIN, 0.4f);
        map.put(TYPE_BEAUTY_BODY_LONG_LEG, 0.4f);
        // 滤镜
        map.put(TYPE_FILTER, 0.5F);
        DEFAULT_VALUE = Collections.unmodifiableMap(map);
    }

    // view
    private ProgressBar pb;
    private ImageView ivNormal;
    private TabLayout tl;
    private TextView tvTitle;
    private ViewPager vp;

    // fragment 列表
    private List<Fragment> mFragmentList;
    // 当前选择的效果类型，如磨皮等
    private int mSelectType = -1;
    // 当前所处的 ViewPager 位置
    private int mLastPage = 0;
    // 当前显示的 Fragment
    private IProgressCallback mSelectFragment;
    // 美颜效果强度表
    private SparseArray<Float> mBeautyProgressMap = new SparseArray<>();
    // 美妆效果强度表
    private SparseArray<Float> mMakeupProgressMap = new SparseArray<>();
    // 每一个 Fragment 中选中的效果
    private SparseIntArray mTypeMap = new SparseIntArray();
    // 每一个美妆效果选中的类型，如口红的胡萝卜红
    private SparseIntArray mMakeupOptionSelectMap = new SparseIntArray();
    // 所有选中的效果集合
    private SparseArray<ComposerNode> mComposerNodeMap = new SparseArray<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_effect, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setPresenter(new EffectPresenter());

        pb = view.findViewById(R.id.pb_effect);
        ivNormal = view.findViewById(R.id.iv_normal_effect);
        tl = view.findViewById(R.id.tl_identify);
        tvTitle = view.findViewById(R.id.tv_title_identify);
        vp = view.findViewById(R.id.vp_identify);

        pb.setOnProgressChangedListener(new ProgressBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(ProgressBar progressBar, float progress, boolean isFormUser) {
                if (isFormUser) {
                    dispatchProgress(progress);
                }
            }
        });

        ivNormal.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        onNormalDown();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        onNormalUp();
                        break;
                }
                return true;
            }
        });
        initVP();
    }

    private void initVP() {
        mFragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        // 美颜
        mFragmentList.add(new BeautyFaceFragment().setType(TYPE_BEAUTY_FACE).setCallback(new BeautyFaceFragment.IBeautyCallBack() {
            @Override
            public void onBeautySelect(ButtonItem item) {
                int type = item.getNode().getId();
                mSelectType = type;
                if (type == TYPE_CLOSE) {
                    closeBeautyFace();
                    return;
                }

                if (mComposerNodeMap.get(type) == null) {
                    mComposerNodeMap.put(type, item.getNode());
                    updateComposerNodes();
                }
                float progress = mBeautyProgressMap.get(type, NO_VALUE);
                if (progress == NO_VALUE) {
                    dispatchProgress(DEFAULT_VALUE.get(mSelectType));
                } else {
                    dispatchProgress(progress);
                }
            }
        }));
        titleList.add(getString(R.string.tab_face_beautification));

        // 美形
        mFragmentList.add(new BeautyFaceFragment().setType(TYPE_BEAUTY_RESHAPE).setCallback(new BeautyFaceFragment.IBeautyCallBack() {
            @Override
            public void onBeautySelect(ButtonItem item) {
                int type = item.getNode().getId();
                mSelectType = type;
                if (type == TYPE_CLOSE) {
                    closeBeautyReshape();
                    return;
                }

                if (mComposerNodeMap.get(type) == null) {
                    mComposerNodeMap.put(type, item.getNode());
                    updateComposerNodes();
                }
                float progress = mMakeupProgressMap.get(type, NO_VALUE);
                if (progress == NO_VALUE) {
                    dispatchProgress(DEFAULT_VALUE.get(mSelectType));
                } else {
                    dispatchProgress(progress);
                }
            }
        }));
        titleList.add(getString(R.string.tab_face_beauty_reshape));

        // 美体
        mFragmentList.add(new BeautyFaceFragment().setType(TYPE_BEAUTY_BODY).setCallback(new BeautyFaceFragment.IBeautyCallBack() {
            @Override
            public void onBeautySelect(ButtonItem item) {
                int type = item.getNode().getId();
                mSelectType = type;
                if (type == TYPE_CLOSE) {
                    closeBeautyBody();
                    return;
                }

                if (mComposerNodeMap.get(type) == null) {
                    mComposerNodeMap.put(type, item.getNode());
                    updateComposerNodes();
                }
                dispatchProgress(DEFAULT_VALUE.get(mSelectType));



            }
        }));
        titleList.add(getString(R.string.tab_face_beauty_body));

        // 美妆
        mFragmentList.add(new BeautyFaceFragment().setType(TYPE_MAKEUP).setCallback(new BeautyFaceFragment.IBeautyCallBack() {
            @Override
            public void onBeautySelect (ButtonItem item) {
                mSelectType = item.getNode().getId();
                if (mSelectType == TYPE_CLOSE) {
                    closeMakeup();
                    return;
                }

                tvTitle.setText(item.getTitle());
                showOrHideMakeupOptionFragment(true);
            }
        }));
        titleList.add(getString(R.string.tab_face_makeup));
        // 滤镜
        mFragmentList.add(new FilterFragment().setCallback(new FilterFragment.IFilterCallback() {
            @Override
            public void onFilterSelected(File file) {
                mSelectType = TYPE_FILTER;
                getCallback().onFilterSelected(file);
                // 选中滤镜之后初始化强度
                dispatchProgress(DEFAULT_VALUE.get(mSelectType));
            }
        }));
        titleList.add(getString(R.string.tab_filter));

        mSelectFragment = (IProgressCallback) mFragmentList.get(0);
        FragmentVPAdapter adapter = new FragmentVPAdapter(getChildFragmentManager(),
                mFragmentList, titleList);
        vp.setAdapter(adapter);
        vp.setOffscreenPageLimit(mFragmentList.size());
        vp.addOnPageChangeListener(new OnPageChangeListenerAdapter() {
            @Override
            public void onPageSelected(int position) {
                mSelectFragment = (IProgressCallback) mFragmentList.get(position);

                // 为了实现用户切换不同的 Fragment 时 ProgressBar 也能跟着切换，需要使用 mTypeMap 保存
                // 用户在每一个 Fragment 中选中的开关，然后再根据开关从 mProgressMap 找到这个开关对应的进度，
                // 再将其设置到 ProgressBar 中
                mTypeMap.put(mLastPage, mSelectType);
                mSelectType = mTypeMap.get(position, NO_POSITION);
                pb.setProgress(getProgressWithType(mSelectType));
                mLastPage = position;

                showOrHideProgressBar(position != POSITION_MAKEUP && position != POSITION_BODY);
            }
        });
        tl.setupWithViewPager(vp);
    }

    /**
     * 将进度分发出去，有两个出口
     * 1、分到对应的 Fragment 中供其更改 UI
     * 2、传递给 Callback 供 EffectRenderHelper 渲染
     * @param progress 进度，0～100
     */
    private void dispatchProgress(float progress) {
        if (mSelectType < 0) return;

        mSelectFragment.onProgress(progress);

        if (pb.getProgress() != progress) {
            pb.setProgress(progress);
        }

        if (mSelectType == TYPE_FILTER) {
            getCallback().onFilterValueChanged(progress);
        } else {
            if ((mSelectType & MASK) == TYPE_BEAUTY_FACE) {
                mBeautyProgressMap.put(mSelectType, progress);
            } else if ((mSelectType & MASK) == TYPE_BEAUTY_RESHAPE) {
                mMakeupProgressMap.put(mSelectType, progress);
            }

            // 特殊情况，锐化的强度值为 0～0.9
            if (mSelectType == TYPE_BEAUTY_FACE_SHARPEN) {
                progress = progress * 0.9F;
            }
            // 从 mComposerNodeMap 中取 node
            ComposerNode node = mComposerNodeMap.get(mSelectType);
            if (node == null) {
                LogUtils.e("composer node must be added in mComposerNodeMap before, " +
                        "node not found: " + mSelectType + ", map: " + mComposerNodeMap.toString());
                return;
            }
            node.setValue(progress);
            getCallback().updateComposeNodeIntensity(node);
        }
    }

    @Override
    public void onClose() {
        // 关闭美颜、美妆、滤镜效果
        getCallback().updateComposeNodes(new String[0]);
        getCallback().onFilterSelected(null);

        // 清空缓存
        mBeautyProgressMap.clear();
        mMakeupProgressMap.clear();
        mMakeupOptionSelectMap.clear();
        mComposerNodeMap.clear();

        // 重置 view
        vp.setCurrentItem(0);
        pb.setProgress(0);
        mTypeMap.clear();

        // 重置 MakeupOptionFragment
        MakeupOptionFragment fragment = (MakeupOptionFragment) getChildFragmentManager()
                .findFragmentByTag(TAG_MAKEUP_OPTION_FRAGMENT);
        if (fragment != null) {
            showOrHideMakeupOptionFragment(false);
        }

        // 调用子 View onClose
        for (Fragment f : mFragmentList) {
            if (f instanceof OnCloseListener) {
                ((OnCloseListener) f).onClose();
            }
        }
    }

    @Override
    public void onOptionSelect(ComposerNode node, int select) {
        // 记录当前选择并更新 UI
        mMakeupOptionSelectMap.put(mSelectType, select);
        BeautyFaceFragment fragment = (BeautyFaceFragment) mFragmentList.get(POSITION_MAKEUP);
        fragment.onProgress(select == 0 ? 0 : 1);

        // 生成对应 flag
        mComposerNodeMap.put(node.getId(), node);

        updateComposerNodes();
    }

    @Override
    public void onCloseClick() {
        showOrHideMakeupOptionFragment(false);
    }

    /**
     * 更新 ComposerNode，先调用 Presenter 将 composerNodeMap 转化为 String[]
     */
    private void updateComposerNodes() {
        getCallback().updateComposeNodes(mPresenter.generateComposerNodes(mComposerNodeMap));
    }

    private void closeBeautyFace() {
        mPresenter.removeNodesOfType(mComposerNodeMap, TYPE_BEAUTY_FACE);
        updateComposerNodes();

        // 清除缓存
        mBeautyProgressMap.clear();

        // 调用子 Fragment 关闭 UI
        OnCloseListener listener = (OnCloseListener) mFragmentList.get(POSITION_BEAUTY);
        listener.onClose();
    }

    private void closeBeautyBody() {
        mPresenter.removeNodesOfType(mComposerNodeMap, TYPE_BEAUTY_BODY);
        updateComposerNodes();
        // 调用子 Fragment 关闭 UI
        OnCloseListener listener = (OnCloseListener) mFragmentList.get(POSITION_BODY);
        listener.onClose();
    }

    private void closeBeautyReshape() {
        mPresenter.removeNodesOfType(mComposerNodeMap, TYPE_BEAUTY_RESHAPE);
        updateComposerNodes();

        int index;
        mMakeupProgressMap.clear();

        OnCloseListener listener = (OnCloseListener) mFragmentList.get(POSITION_RESHAPE);
        listener.onClose();
    }

    private void closeMakeup() {
        mPresenter.removeNodesOfType(mComposerNodeMap, TYPE_MAKEUP_OPTION);
        updateComposerNodes();

        mMakeupOptionSelectMap.clear();

        OnCloseListener listener = (OnCloseListener) mFragmentList.get(POSITION_MAKEUP);
        listener.onClose();
    }

    private void showOrHideProgressBar(boolean isShow) {
        pb.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * 显示 or 隐藏 MakeupOptionFragment，在没有实例的情况下会先初始化一个实例
     * 显示一个 MakeupOptionFragment 的时候还会设置其默认选择位置，这个位置保存在
     * {@link this#mMakeupOptionSelectMap} 中
     * @param isShow 是否显示
     */
    private void showOrHideMakeupOptionFragment(boolean isShow) {
        FragmentManager manager = getChildFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(R.anim.board_enter, R.anim.board_exit);
        Fragment makeupOptionFragment = manager.findFragmentByTag(TAG_MAKEUP_OPTION_FRAGMENT);

        if (isShow) {
            tl.setVisibility(View.GONE);
            vp.setVisibility(View.GONE);
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
            if (makeupOptionFragment == null) {
                makeupOptionFragment = generateMakeupOptionFragment();
                ((MakeupOptionFragment)makeupOptionFragment).setMakeupType(mSelectType, mMakeupOptionSelectMap.get(mSelectType, 0));

                transaction.add(R.id.fl_identify, makeupOptionFragment, TAG_MAKEUP_OPTION_FRAGMENT).commit();
            } else {
                ((MakeupOptionFragment)makeupOptionFragment).setMakeupType(mSelectType, mMakeupOptionSelectMap.get(mSelectType, 0));
                transaction.show(makeupOptionFragment).commit();
            }
        } else {
            transaction.hide(makeupOptionFragment).commit();
            tvTitle.animate().alpha(0).setDuration(ANIMATION_DURATION).start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    tvTitle.setVisibility(View.GONE);
                    tl.setVisibility(View.VISIBLE);
                    vp.setVisibility(View.VISIBLE);
                }
            }, ANIMATION_DURATION);
        }
    }

    private Fragment generateMakeupOptionFragment() {
        return new MakeupOptionFragment().setCallback(this);
    }

    /**
     * 对比按钮按下，关闭无美颜美妆
     */
    private void onNormalDown() {
        getCallback().setEffectOn(false);
    }

    /**
     * 对比按钮松开，恢复美颜美妆
     */
    private void onNormalUp() {
        getCallback().setEffectOn(true);
    }

    /**
     * 从缓存表找到对应类型的缓存值
     * @param type 特效类型
     * @return 缓存值，没有则返回 0
     */
    private float getProgressWithType(int type) {
        float value;
        if ((value = mBeautyProgressMap.get(type, NO_VALUE)) > 0) {
            return value;
        }
        if ((value = mMakeupProgressMap.get(type, NO_VALUE)) > 0) {
            return value;
        }
        return 0F;
    }

    /**
     * 用户手动调节 ProgressBar 之后，由此回调至各功能 Fragment 调整 UI
     */
    public interface IProgressCallback {
        /**
         * 用于进度更改时的回调，将进度分发到子 Fragment 中
         * @param progress 进度
         */
        void onProgress(float progress);
    }

    public interface IEffectCallback {
        /**
         * 更新美妆美颜设置
         * @param nodes 字符串数组，存储所有设置的美颜内容，当 node 长度为 0 时意为关闭美妆
         */
        void updateComposeNodes(String[] nodes);

        /**
         * 更新某一个效果的强度
         * @param node 效果对应 ComposerNode
         */
        void updateComposeNodeIntensity(ComposerNode node);

        // 滤镜
        void onFilterSelected(File file);
        void onFilterValueChanged(float cur);

        /**
         * 设置是否处理特效
         * @param isOn if false，则在处理纹理的时候不使用 RenderManager 处理原始纹理，则不会有效果
         */
        void setEffectOn(boolean isOn);
    }
}
