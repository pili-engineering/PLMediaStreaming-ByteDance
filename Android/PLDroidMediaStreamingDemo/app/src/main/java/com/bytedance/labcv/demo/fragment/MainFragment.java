package com.bytedance.labcv.demo.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytedance.labcv.demo.MainActivity;
import com.bytedance.labcv.demo.PermissionsActivity;
import com.qiniu.pili.droid.streaming.demo.R;
import com.bytedance.labcv.demo.core.CameraRenderView;
import com.bytedance.labcv.demo.core.EffectRenderHelper;
import com.bytedance.labcv.demo.model.CaptureResult;
import com.bytedance.labcv.demo.model.ComposerNode;
import com.bytedance.labcv.demo.utils.BitmapUtils;
import com.bytedance.labcv.demo.utils.CommonUtils;
import com.bytedance.labcv.demo.utils.Config;
import com.bytedance.labcv.demo.utils.ToasUtils;
import com.bytedance.labcv.demo.utils.UserData;
import com.bytedance.labcv.demo.view.VideoButton;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import static com.bytedance.labcv.demo.contract.StickerContract.TYPE_ANIMOJI;
import static com.bytedance.labcv.demo.contract.StickerContract.TYPE_STICKER;

/**
 * Created by QunZhang on 2019-12-20 11:14
 */
public class MainFragment extends Fragment implements EffectRenderHelper.OnEffectListener, View.OnClickListener {
    // 标志面板类型，分别是 识别、特效、贴纸
    // Logo panel type, respectively is identification, special effects, stickers
    public static final String TAG_EFFECT = "effect";
    public static final String TAG_STICKER = "sticker";
    public static final String TAG_ANIMOJI = "animoji";

    public static final int ANIMATOR_DURATION = 400;

    private EffectFragment mEffectFragment;
    private StickerFragment mStickerFragment;
    private StickerFragment mAnimojiFragment;
    // 正在处于功能可用状态的面板
    // current panel
    private MainActivity.OnCloseListener mWorkingFragment;

    private TextView mFpsTextView;

//    private CameraRenderView mSurfaceView;
    private EffectRenderHelper effectRenderHelper;

//    private FrameLayout mSurfaceContainer;

    private View rootView;

    private LinearLayout llFeature;
    private LinearLayout llEffect;
    private LinearLayout llSticker;
    private LinearLayout llAnimoji;

    //  below UI elements are for debug
    public StringBuilder cameraInfo;
    public TextView tvInfo;

    public TextView tvcameraInfo;

    public ImageView mImageView;
    private VideoButton vbTakePic;

    private SwitchCompat scExclusive;

    private boolean mFirstEnter = true;
    private String mSavedStickerPath;
    private String mSavedAnimojiPath;
    private MainActivity.ICheckAvailableCallback mCheckAvailableCallback = new MainActivity.ICheckAvailableCallback() {
        @Override
        public boolean checkAvailable(int id) {
            if (mSavedAnimojiPath != null && !mSavedAnimojiPath.equals("")) {
                ToasUtils.show(getString(R.string.tip_close_animoji_first));
                return false;
            }
            if (isExclusive() && id != TYPE_STICKER && mSavedStickerPath != null && !mSavedStickerPath.equals("")) {
                ToasUtils.show(getString(R.string.tip_close_sticker_first));
                return false;
            }
            return true;
        }
    };


    private static final int UPDATE_INFO = 1;
    // 拍照失败
    private static final int CAPTURE_FAIL = 9;
    // 拍照成功
    private static final int CAPTURE_SUCCESS = 10;



    private static final int UPDATE_INFO_INTERVAL = 1000;


    private InnerHandler mHandler = new InnerHandler(this);

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        OrientationSensor.start(getContext());
        initViews();
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        OrientationSensor.start(this);
//
//        setContentView(R.layout.activity_main);
//        checkPermissions();
//        initViews();
//    }

    private void initViews() {
        llFeature = getView().findViewById(R.id.ll_feature);
        llEffect = getView().findViewById(R.id.ll_effect);
        llSticker = getView().findViewById(R.id.ll_sticker);
        llAnimoji = getView().findViewById(R.id.ll_animoji);
        mImageView = getView().findViewById(R.id.img);
        tvInfo = getView().findViewById(R.id.tv_info);
        tvcameraInfo = getView().findViewById(R.id.camera_info);
//        mSurfaceView = getView().findViewById(R.id.gl_surface);
//        effectRenderHelper = mSurfaceView.getEffectRenderHelper();
        effectRenderHelper = new EffectRenderHelper(getContext());
        effectRenderHelper.setOnEffectListener(this);
//        mSurfaceContainer = getView().getView().findViewById(R.id.surface_container);

        getView().findViewById(R.id.iv_change_camera).setOnClickListener(this);
        vbTakePic = getView().findViewById(R.id.btn_take_pic);
        vbTakePic.setOnClickListener(this);
        llEffect.setOnClickListener(this);
        llSticker.setOnClickListener(this);
        llAnimoji.setOnClickListener(this);
        scExclusive = getView().findViewById(R.id.switch_exclusive);
        scExclusive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == UserData.getExclusive(getContext(), false)) {
                    return;
                }
                UserData.setExclusive(getContext(), isChecked);
                ToasUtils.show(getString(R.string.exclusive_tip));
            }
        });

        mFpsTextView = getView().findViewById(R.id.info_fps);
        rootView = getView().findViewById(R.id.rl_root);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                closeFeature(true);
                return false;
            }
        });
    }

//    private void switchCamera() {
//        mSurfaceView.switchCamera();
//    }




    /**
     * 根据 TAG 创建对应的 Fragment
     * Create the corresponding Fragment based on TAG
     * @param tag  tag
     * @return  Fragment
     */
    private Fragment generateFragment(String tag) {
        switch (tag) {
            case TAG_EFFECT:
                if (mEffectFragment != null) return mEffectFragment;

                final EffectFragment effectFragment = new EffectFragment();
                effectFragment.setCheckAvailableCallback(mCheckAvailableCallback)
                        .setCallback(new EffectFragment.IEffectCallback() {

                            @Override
                            public void updateComposeNodes(final String[] nodes) {
                                LogUtils.e("update composer nodes: " + Arrays.toString(nodes));
//                                if (nodes.length > 0) {
//                                    onFragmentWorking(mEffectFragment);
//                                }
                                effectRenderHelper.setComposeNodes(nodes);
                            }


                            @Override
                            public void updateComposeNodeIntensity(final ComposerNode node) {
                                LogUtils.e("update composer node intensity: node: " + node.getNode() + ", key: " + node.getKey() + ", value: " + node.getValue());

                                effectRenderHelper.updateComposeNode(node, true);
                            }

                            @Override
                            public void onFilterSelected(final File file) {

                                effectRenderHelper.setFilter(file != null ? file.getAbsolutePath() : "");
//                                if (file != null) {
//                                    onFragmentWorking(mEffectFragment);
//                                }
                            }


                            @Override
                            public void onFilterValueChanged(final float cur) {

                                effectRenderHelper.updateIntensity(BytedEffectConstants.IntensityType.Filter, cur);
                            }

                            @Override
                            public void setEffectOn(final boolean isOn) {

                                effectRenderHelper.setEffectOn(isOn);
                            }

                            @Override
                            public void onDefaultClick() {
                                onFragmentWorking(mEffectFragment);
                            }
                        });
                mEffectFragment = effectFragment;
                return effectFragment;
            case TAG_STICKER:
                if (mStickerFragment != null) return mStickerFragment;

                StickerFragment stickerFragment = new StickerFragment()
                        .setCheckAvailableCallback(mCheckAvailableCallback)
                        .setType(TYPE_STICKER);
                stickerFragment.setCallback(new StickerFragment.IStickerCallback() {
                    @Override
                    public void onStickerSelected(final File file) {
                        mSavedStickerPath = file == null ? null : file.getAbsolutePath();
                        if (file != null) {
                            onFragmentWorking(mStickerFragment);
                        }
                        effectRenderHelper.setSticker(file != null ? file.getAbsolutePath() : "");
                        if (isExclusive() && file == null) {
                            mEffectFragment.recoverState();
                        }
                    }
                });
                mStickerFragment = stickerFragment;
                return stickerFragment;
            case TAG_ANIMOJI:
                if (mAnimojiFragment != null) return mAnimojiFragment;

                StickerFragment animojiFragment = new StickerFragment().setType(TYPE_ANIMOJI);
                animojiFragment.setCallback(new StickerFragment.IStickerCallback() {
                    @Override
                    public void onStickerSelected(final File file) {
                        mSavedAnimojiPath = file == null ? null : file.getAbsolutePath();
                        if (file != null) {
                            onFragmentWorking(mAnimojiFragment);
                        }
                        effectRenderHelper.setSticker(file != null ? file.getAbsolutePath() : "");
                        if (file == null) {
                            if (mStickerFragment != null && mSavedStickerPath != null && !mSavedStickerPath.equals("")) {
                                mStickerFragment.recoverState(mSavedStickerPath);
                            }
                            if ((!isExclusive() || mSavedStickerPath == null || mSavedStickerPath.equals(""))
                                    && mEffectFragment != null) {
                                mEffectFragment.recoverState();
                            }
                        }
//                        if (file == null && mEffectFragment != null) {
//                            mEffectFragment.recoverState();
//                        }
//                        if (file == null && mSavedStickerPath != null &&
//                                !mSavedStickerPath.equals("") && mStickerFragment != null) {
//                            mStickerFragment.recoverState(mSavedStickerPath);
//                        }
                    }
                });
                mAnimojiFragment = animojiFragment;
                return animojiFragment;
            default:
                return null;
        }
    }

    /**
     * 展示某一个 feature 面板
     * Show a feature panel
     * @param tag tag use to mark Fragment 用于标志 Fragment 的 tag
     */
    private void showFeature(String tag, boolean hideBoard) {
//        if (mSurfaceView == null) return;
        if (effectRenderHelper == null) return;

        if (showingFragment() != null) {
            getChildFragmentManager().beginTransaction().hide(showingFragment()).commit();
        }

        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (hideBoard) {
            ft.setCustomAnimations(R.anim.board_enter, R.anim.board_exit);
        }
        Fragment fragment = fm.findFragmentByTag(tag);

        if (fragment == null) {
            fragment = generateFragment(tag);
            ft.add(R.id.board_container, fragment, tag).commit();
        } else {
            ft.show(fragment).commit();
        }
        if (hideBoard) {
            showOrHideBoard(false);
        }
    }

    /**
     * 关闭所有的 feature 面板
     * close all feature panel
     * @return whether close panel successfully 是否成功关闭某个面板，即是否有面板正在开启中
     */
    private boolean closeFeature(boolean animation) {
        Fragment showingFragment = showingFragment();
        if (showingFragment != null) {
            FragmentTransaction ft =getChildFragmentManager().beginTransaction();
            if (animation) {
                ft.setCustomAnimations(R.anim.board_enter, R.anim.board_exit);
            }
            ft.hide(showingFragment).commit();
        }

        showOrHideBoard(true);
        return showingFragment != null;
    }

    private Fragment showingFragment() {
        if (mEffectFragment != null && !mEffectFragment.isHidden()) {
            return mEffectFragment;
        } else if (mStickerFragment != null && !mStickerFragment.isHidden()) {
            return mStickerFragment;
        } else if (mAnimojiFragment != null && !mAnimojiFragment.isHidden()) {
            return mAnimojiFragment;
        }
        return null;
    }

    /**
     * 展示或关闭菜单面板
     * show board
     * @param show 展示
     */
    private void showOrHideBoard(boolean show) {
        if (show) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (showingFragment() == null) {
                        vbTakePic.setVisibility(View.VISIBLE);
                        llFeature.setVisibility(View.VISIBLE);
                    }
                }
            }, ANIMATOR_DURATION);
        } else {
            vbTakePic.setVisibility(View.GONE);
            llFeature.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // release device
        mHandler.removeCallbacksAndMessages(null);
//        mSurfaceView.onPause();
    }


    @Override
    public void onDestroy() {
        OrientationSensor.stop();
//        mSurfaceContainer.removeAllViews();
        super.onDestroy();
//        mSurfaceView = null;
        mEffectFragment = null;
        mStickerFragment = null;

    }

    @Override
    public void onResume() {
        super.onResume();
        // setup device
//        mSurfaceView.onResume();
        mHandler.sendEmptyMessageDelayed(UPDATE_INFO, UPDATE_INFO_INTERVAL);
    }

//    private void checkPermissions() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(Config.PERMISSION_CAMERA) != PackageManager.PERMISSION_GRANTED ||
//                    checkSelfPermission(Config.PERMISSION_STORAGE) != PackageManager.PERMISSION_GRANTED ||
//                    checkSelfPermission(Config.PERMISSION_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//                // start Permissions activity
//                Intent intent = new Intent(this, PermissionsActivity.class);
//                startActivity(intent);
//                finish();
//                overridePendingTransition(0, 0);
//            }
//        }
//    }

    @Override
    public void onClick(View v) {
        if (CommonUtils.isFastClick()) {
            ToasUtils.show("too fast click");
            return;
        }
        switch (v.getId()) {
            case R.id.iv_change_camera:
//                switchCamera();
                break;
            case R.id.btn_take_pic:
                takePic();
                break;
            case R.id.ll_effect:
                showFeature(TAG_EFFECT, true);
                break;
            case R.id.ll_sticker:
                showFeature(TAG_STICKER, true);
                break;
            case R.id.ll_animoji:
                showFeature(TAG_ANIMOJI, true);
                break;
        }
    }


//
//    @Override
//    public void onBackPressed() {
//        if (closeFeature(true)) {
//            return;
//        }
//        super.onBackPressed();
//    }

    private void takePic() {
        if (null == effectRenderHelper) return;
        if (mHandler == null) return;
        CaptureResult captureResult= effectRenderHelper.capture();

        if (null == captureResult || captureResult.getWidth() == 0 || captureResult.getHeight() == 0|| null == captureResult.getByteBuffer()){
            mHandler.sendEmptyMessage(CAPTURE_FAIL);

        }else {
            Message msg = mHandler.obtainMessage(CAPTURE_SUCCESS, captureResult);
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getActivity().finish();
    }

    private static class InnerHandler extends Handler {
        private final WeakReference<MainFragment> mActivity;

        public InnerHandler(MainFragment activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainFragment activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case UPDATE_INFO:
//                        activity.mFpsTextView.setText("" + activity.mSurfaceView.getFrameRate());
                        sendEmptyMessageDelayed(UPDATE_INFO, UPDATE_INFO_INTERVAL);
                        break;
                    case CAPTURE_SUCCESS:
                        CaptureResult captureResult = (CaptureResult) msg.obj;
                        SavePicTask task  = new SavePicTask(mActivity.get().getContext());
                        task.execute(captureResult);

                        break;


                }
            }
        }

    }

    static class SavePicTask extends AsyncTask<CaptureResult, Void,String> {
        private WeakReference<Context> mContext;

        public SavePicTask(Context context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(CaptureResult... captureResults) {
            if (captureResults.length == 0) return "captureResult arrayLength is 0";
            Bitmap bitmap = Bitmap.createBitmap(captureResults[0].getWidth(), captureResults[0].getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(captureResults[0].getByteBuffer().position(0));
            File file = BitmapUtils.saveToLocal(bitmap);
            if (file.exists()){
                return file.getAbsolutePath();
            }else{
                return "";
            }
        }

        @Override
        protected void onPostExecute(String path) {
            super.onPostExecute(path);
            if (TextUtils.isEmpty(path)){
                ToasUtils.show("图片保存失败");
                return;
            }
            if (mContext.get() == null) {
                try {
                    new File(path).delete();
                } catch (Exception ignored) {
                }
                ToasUtils.show("图片保存失败");
            }
            try{
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, path);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
                mContext.get().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }catch (Exception e){
                e.printStackTrace();
            }
            ToasUtils.show("保存成功，路径："+path);
        }
    }

    /**
     * 当用户选择贴纸时，利用回调接口，关闭对应的开关
     * When the user selects the sticker
     * Use the callback interface to turn off the corresponding switch
     */
    private void onFragmentWorking(Fragment fragment) {
        if (fragment == mEffectFragment) {
            if (mStickerFragment != null) {
                if (mSavedStickerPath != null) {
                    mStickerFragment.onClose();
                    mSavedStickerPath = null;
                    effectRenderHelper.setSticker(null);
                }
            }
            if (mAnimojiFragment != null) {
                if (mSavedAnimojiPath != null) {
                    mAnimojiFragment.onClose();
                    mSavedAnimojiPath = null;
                    effectRenderHelper.setSticker(null);
                }
            }
        } else if (fragment == mStickerFragment) {
            if (isExclusive() && mEffectFragment != null) {
                mEffectFragment.onClose();
            }
        } else if (fragment == mAnimojiFragment) {
            if (mEffectFragment != null) {
                mEffectFragment.onClose();
            }
            if (mStickerFragment != null) {
                mStickerFragment.onClose();
            }
        }
//        if (fragment == mAnimojiFragment) {
//            if (mEffectFragment != null) {
//                mEffectFragment.onClose();
//            }
//            if (mStickerFragment != null) {
//                mStickerFragment.onClose();
//            }
//            return;
//        }
//        if (!isExclusive()) {
//            return;
//        }
//        if (fragment instanceof OnCloseListener) {
//            if (fragment != mWorkingFragment) {
//                // 开启贴纸会关闭美颜，反之不生效
//                if (mWorkingFragment != null) {
//                    mWorkingFragment.onClose();
//                }
//                mWorkingFragment = (OnCloseListener) fragment;
//            }
//        } else {
//            throw new IllegalArgumentException("fragment " + fragment + " must implement " + OnCloseListener.class);
//        }
    }

    @Override
    public void onEffectInitialized() {
        if (!mFirstEnter) {
            return;
        }
        mFirstEnter = false;
        final boolean exclusive = UserData.getExclusive(getContext(), false);
        effectRenderHelper.setComposerMode(exclusive ? 0 : 1);
        final String[] features = new String[30];
        effectRenderHelper.getAvailableFeatures(features);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scExclusive.setChecked(exclusive);
                showFeature(TAG_EFFECT, false);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (mEffectFragment != null) {
                            mEffectFragment.onDefaultClick();
                        }
                        closeFeature(false);
                    }
                });
                for (String feature : features) {
                    if (feature != null && feature.equals("3DStickerV3")) {
                        llAnimoji.setVisibility(View.VISIBLE);
                        break;
                    }
                }
                llFeature.setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean isExclusive() {
        return effectRenderHelper.getComposerMode() == 0;
    }

    public void initEffectRenderHelper(int width, int height) {
        effectRenderHelper.initEffectSDK(width, height);
        effectRenderHelper.onSurfaceChanged(width, height);
        effectRenderHelper.recoverStatus();
    }

    public int processTexture(int texture, int width, int height) {
        return effectRenderHelper.processTexure(texture, BytedEffectConstants.TextureFormat.Texure2D,
                width, height, 0, true, BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0,
                System.currentTimeMillis());
    }

    /**
     * 定义一个回调接口，用于当用户选择其中一个面板时，
     * 关闭其他面板的回调，此接口由各 Fragment 实现，
     * 在 onClose() 方法中要完成各 Fragment 中 UI 的初始化，
     * 即关闭用户已经开启的开关
     *
     * Define a callback interface for when a user selects one of the panels，
     * close the callback of the other panel, which is implemented by each Fragment
     * In the onClose() method, initialize the UI of each Fragment:
     * turn off the switch that the user has already turned on
     */
    public interface OnCloseListener {
        void onClose();
    }

    public interface ICheckAvailableCallback {
        boolean checkAvailable(int id);
    }
}
