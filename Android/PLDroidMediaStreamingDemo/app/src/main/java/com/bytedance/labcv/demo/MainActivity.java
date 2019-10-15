// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.bytedance.labcv.demo;

import android.app.ProgressDialog;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytedance.labcv.demo.core.CameraRenderView;
import com.bytedance.labcv.demo.core.EffectRenderHelper;
import com.bytedance.labcv.demo.fragment.EffectFragment;
import com.bytedance.labcv.demo.fragment.StickerFragment;
import com.bytedance.labcv.demo.model.CaptureResult;
import com.bytedance.labcv.demo.model.ComposerNode;
import com.bytedance.labcv.demo.utils.BitmapUtils;
import com.bytedance.labcv.demo.utils.CommonUtils;
import com.bytedance.labcv.demo.utils.Config;
import com.bytedance.labcv.demo.utils.ToasUtils;
import com.bytedance.labcv.demo.view.TipManager;
import com.bytedance.labcv.demo.view.VideoButton;
import com.bytedance.labcv.demo.view.ViewfinderView;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.effect.OnCloseListener;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;

public class MainActivity extends FragmentActivity implements View.OnClickListener{
    // 标志面板类型，分别是 识别、特效、贴纸
    // Logo panel type, respectively is identification, special effects, stickers
    public static final String TAG_EFFECT = "effect";
    public static final String TAG_STICKER = "sticker";

    public static final int ANIMATOR_DURATION = 400;

    private EffectFragment mEffectFragment;
    private StickerFragment mStickerFragment;
    // 正在处于功能可用状态的面板
    // current panel
    private OnCloseListener mWorkingFragment;

    private int[] mPreviewsizes = new int[] {720, 1280};

    private TextView mFpsTextView;

    private CameraRenderView mSurfaceView;
    private EffectRenderHelper effectRenderHelper;

    private TipManager mTipManager = new TipManager();
    private FrameLayout mSurfaceContainer;

    private FrameLayout mTipContainer;

    private View rootView;


    private ViewfinderView mFinderView;

    private Context mContext;


    private ProgressDialog progressDialog;



    private LinearLayout llFeature;
    private LinearLayout llEffect;
    private LinearLayout llSticker;

    private boolean isShowQr = false;
    private boolean isStartedRecord = false;
    private String mVideoPath;

    //  below UI elements are for debug

    public StringBuilder info;
    public StringBuilder cameraInfo;
    public TextView tvInfo;

    public TextView tvcameraInfo;

    public ImageView mImageView;
//    private Button btnTakePic;
    private VideoButton vbTakePic;


    private static final int UPDATE_INFO = 1;
    // 文件下载成功
    private static final int DOWNLOAD_SUCCESS = 2;
    // 文件下载失败
    private static final int DOWNLOAD_FAIL = 3;
    // 文件解压失败
    private static final int UNZIP_FAIL = 4;
    // 文件校验失败 主要检测是否有license
    // File validation failure mainly detects whether there is a license
    private static final int FILE_CHECK_FAIL = 5;
    // 授权失败
    private static final int LICENSE_CHECK_FAIL = 6;
    // 贴纸加载成功
    private static final int STICKER_LOAD_SUCCESS = 7;
    // 贴纸加载失败
    private static final int STICKER_LOAD_FAIL = 8;
    // 拍照失败
    private static final int CAPTURE_FAIL = 9;
    // 拍照成功
    private static final int CAPTURE_SUCCESS = 10;



    private static final int UPDATE_INFO_INTERVAL = 1000;

    public InnerHandler getHandler() {
        return mHandler;
    }

    private InnerHandler mHandler = new InnerHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        OrientationSensor.start(this);

        setContentView(R.layout.activity_main);
        checkPermissions();
        initViews();

    }

    public void showProgressDialog(String title) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(title);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setProgress(0);
        progressDialog.show();


    }

    public void hideProressDialog() {
        if (null != progressDialog) {
            progressDialog.dismiss();
        }

    }

    public void updateProgressDialog(int progress) {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setProgress(progress);
        }
    }

    private void initViews() {
        llFeature = findViewById(R.id.ll_feature);
        llEffect = findViewById(R.id.ll_effect);
        llSticker = findViewById(R.id.ll_sticker);



        mFinderView = findViewById(R.id.qrcode_finder);
        mImageView = findViewById(R.id.img);
        tvInfo = findViewById(R.id.tv_info);
        tvcameraInfo = findViewById(R.id.camera_info);
        mSurfaceView = findViewById(R.id.gl_surface);
        effectRenderHelper = mSurfaceView.getEffectRenderHelper();
        mSurfaceContainer = findViewById(R.id.surface_container);

        mTipContainer = findViewById(R.id.tip_container);
        mTipManager.init(MainActivity.this, mTipContainer);

        findViewById(R.id.iv_change_camera).setOnClickListener(this);
        findViewById(R.id.iv_qr_code).setOnClickListener(this);
        vbTakePic = findViewById(R.id.btn_take_pic);
        vbTakePic.setOnClickListener(this);
        llEffect.setOnClickListener(this);
        llSticker.setOnClickListener(this);


        mFpsTextView = findViewById(R.id.info_fps);
        rootView = findViewById(R.id.rl_root);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                closeFeature();
                return false;
            }
        });


    }

    private void switchCamera() {
        mSurfaceView.switchCamera();
    }




    /**
     * 根据 TAG 创建对应的 Fragment
     * Create the corresponding Fragment based on TAG
     * @param tag  tag
     * @return  Fragment
     */
    private Fragment generateFragment(String tag) {
        switch (tag) {
            case TAG_EFFECT:
                final EffectFragment effectFragment = new EffectFragment();
                effectFragment.setCallback(new EffectFragment.IEffectCallback() {

                    @Override
                    public void updateComposeNodes(final String[] nodes) {
                        LogUtils.e("update composer nodes: " + Arrays.toString(nodes));
                        if (nodes.length > 0) {
                            onFragmentWorking(mEffectFragment);
                        }
                        if (mSurfaceView != null) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.setComposeNodes(nodes);
                                }
                            });
                        }
                    }


                    @Override
                    public void updateComposeNodeIntensity(final ComposerNode node) {
                        LogUtils.e("update composer node intensity: node: " + node.getNode() + ", key: " + node.getKey() + ", value: " + node.getValue());
                        if (mSurfaceView != null) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.updateComposeNode(node);
                                }
                            });
                        }
                    }

                    @Override
                    public void onFilterSelected(final File file) {
                        if (null != mSurfaceView) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.setFilter(file != null ? file.getAbsolutePath() : "");

                                }
                            });
                        }
                        if (file != null) {
                            onFragmentWorking(mEffectFragment);
                        }
                    }


                    @Override
                    public void onFilterValueChanged(final float cur) {
                        if (null != mSurfaceView) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.updateIntensity(BytedEffectConstants.IntensityType.Filter, cur);
                                }
                            });
                        }
                    }

                    @Override
                    public void setEffectOn(final boolean isOn) {
                        if (mSurfaceView != null) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.setEffectOn(isOn);
                                }
                            });
                        }
                    }
                });
                mEffectFragment = effectFragment;
                return effectFragment;
            case TAG_STICKER:
                StickerFragment stickerFragment = new StickerFragment();
                stickerFragment.setCallback(new StickerFragment.IStickerCallback() {
                    @Override
                    public void onStickerSelected(final File file) {
                        if (file != null) {
                            onFragmentWorking(mStickerFragment);
                        }
                        if (null != mSurfaceView) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.setSticker(file != null ? file.getAbsolutePath() : "");
                                }
                            });
                        }
                    }
                });
                mStickerFragment = stickerFragment;
                return stickerFragment;
            default:
                return null;
        }
    }

    /**
     * 展示某一个 feature 面板
     * Show a feature panel
     * @param tag tag use to mark Fragment 用于标志 Fragment 的 tag
     */
    private void showFeature(String tag) {
        if (mSurfaceView == null) return;
        if (effectRenderHelper == null) return;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(R.anim.board_enter, R.anim.board_exit);
        Fragment fragment = fm.findFragmentByTag(tag);

        if (fragment == null) {
            fragment = generateFragment(tag);
            ft.add(R.id.board_container, fragment, tag).commit();
        } else {
            ft.show(fragment).commit();
        }
        showOrHideBoard(false);
    }

    /**
     * 关闭所有的 feature 面板
     * close all feature panel
     * @return whether close panel successfully 是否成功关闭某个面板，即是否有面板正在开启中
     */
    private boolean closeFeature() {
        boolean hasFeature = false;

        Fragment showedFragment = null;
        if (mEffectFragment != null && !mEffectFragment.isHidden()) {
            showedFragment = mEffectFragment;
            hasFeature = true;
        } else if (mStickerFragment != null && !mStickerFragment.isHidden()) {
            showedFragment = mStickerFragment;
            hasFeature = true;
        }

        if (hasFeature) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.board_enter, R.anim.board_exit)
                    .hide(showedFragment)
                    .commit();
        }

        showOrHideBoard(true);
        return hasFeature;
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
                    llFeature.setVisibility(View.VISIBLE);
                }
            }, ANIMATOR_DURATION);
        } else {
            llFeature.setVisibility(View.GONE);
        }
    }




    @Override
    protected void onPause() {
        super.onPause();
        // release device
        mHandler.removeCallbacksAndMessages(null);
        mSurfaceView.onPause();
    }


    @Override
    protected void onDestroy() {
        OrientationSensor.stop();
        mSurfaceContainer.removeAllViews();
        super.onDestroy();
        mSurfaceView = null;
        mEffectFragment = null;
        mStickerFragment = null;

    }

    @Override
    protected void onResume() {
        super.onResume();
        // setup device
        mSurfaceView.onResume();
        mHandler.sendEmptyMessageDelayed(UPDATE_INFO, UPDATE_INFO_INTERVAL);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Config.PERMISSION_CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Config.PERMISSION_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Config.PERMISSION_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // start Permissions activity
                Intent intent = new Intent(this, PermissionsActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (CommonUtils.isFastClick()) {
            ToasUtils.show("too fast click");
            return;
        }
        switch (v.getId()) {
            case R.id.iv_change_camera:
                switchCamera();
                break;
            case R.id.btn_take_pic:
                takePic();

                break;
            case R.id.ll_effect:
                showFeature(TAG_EFFECT);
                break;
            case R.id.ll_sticker:
                showFeature(TAG_STICKER);
                break;
        }
    }



    @Override
    public void onBackPressed() {
        if (closeFeature()) {
            return;
        }
        super.onBackPressed();
    }

    private void takePic() {
        if (null != mSurfaceView){
            mSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (null == effectRenderHelper) return;
                    if (mHandler == null) return;
//                    CaptureResult captureResult= effectRenderHelper.capture();
//
//                    if (null == captureResult || captureResult.getWidth() == 0 || captureResult.getHeight() == 0|| null == captureResult.getByteBuffer()){
//                        mHandler.sendEmptyMessage(CAPTURE_FAIL);
//
//                    }else {
//                        Message msg = mHandler.obtainMessage(CAPTURE_SUCCESS, captureResult);
//                        mHandler.sendMessage(msg);
//                    }
                }
            });
        }
    }







    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        finish();
    }

    private static class InnerHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public InnerHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case UPDATE_INFO:
                        activity.mFpsTextView.setText("" + activity.mSurfaceView.getFrameRate());
                        sendEmptyMessageDelayed(UPDATE_INFO, UPDATE_INFO_INTERVAL);
                        break;
                    case DOWNLOAD_SUCCESS:
                        activity.hideProressDialog();
                        break;

                    case DOWNLOAD_FAIL:
                        activity.hideProressDialog();
                        ToasUtils.show(activity.getString(R.string.download_fail));
                        break;

                    case UNZIP_FAIL:
                        activity.hideProressDialog();
                        ToasUtils.show(activity.getString(R.string.unip_fail));
                        break;

                    case FILE_CHECK_FAIL:
                        activity.hideProressDialog();
                        ToasUtils.show(activity.getString(R.string.file_check_fail));
                        break;
                    case LICENSE_CHECK_FAIL:
                        activity.hideProressDialog();
                        ToasUtils.show(activity.getString(R.string.license_check_fail));
                        break;
                    case STICKER_LOAD_FAIL:
                        activity.hideProressDialog();
                        ToasUtils.show(activity.getString(R.string.sticker_load_fail));
                        break;
                    case STICKER_LOAD_SUCCESS:
                        activity.hideProressDialog();
                        ToasUtils.show(activity.getString(R.string.sticker_load_success));
                        break;

                    case CAPTURE_FAIL:
                        ToasUtils.show(activity.getString(R.string.sticker_load_fail));
                        break;
                    case CAPTURE_SUCCESS:
                        CaptureResult captureResult = (CaptureResult) msg.obj;
                        SavePicTask task  = new SavePicTask(mActivity.get());
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
        if (fragment instanceof OnCloseListener) {
            if (fragment != mWorkingFragment) {
                if (mWorkingFragment != null) {
                    mWorkingFragment.onClose();
                }
                mWorkingFragment = (OnCloseListener) fragment;
            }
        } else {
            throw new IllegalArgumentException("fragment " + fragment + " must implement " + OnCloseListener.class);
        }
    }

}
