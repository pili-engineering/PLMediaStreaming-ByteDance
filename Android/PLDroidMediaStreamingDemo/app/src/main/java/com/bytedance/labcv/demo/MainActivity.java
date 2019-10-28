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
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytedance.labcv.demo.fragment.EffectFragment;
import com.bytedance.labcv.demo.fragment.FaceInfoFragment;
import com.bytedance.labcv.demo.fragment.IdentifyFragment;
import com.bytedance.labcv.demo.fragment.StickerFragment;
import com.bytedance.labcv.demo.model.CaptureResult;
import com.bytedance.labcv.demo.model.ComposerNode;
import com.bytedance.labcv.demo.model.FaceVerifyResult;
import com.bytedance.labcv.demo.record.IVideoRecord;
import com.bytedance.labcv.demo.record.VideoRecord;
import com.bytedance.labcv.demo.task.decode.RepeatedScannerHandler;
import com.bytedance.labcv.demo.task.faceverify.RepeatedVerifyHandler;
import com.bytedance.labcv.demo.utils.AppUtils;
import com.bytedance.labcv.demo.utils.BitmapUtils;
import com.bytedance.labcv.demo.utils.CommonUtils;
import com.bytedance.labcv.demo.utils.Config;
import com.bytedance.labcv.demo.utils.DownloadStickerManager;
import com.bytedance.labcv.demo.utils.DownloadUtil;
import com.bytedance.labcv.demo.utils.NetworkUtil;
import com.bytedance.labcv.demo.utils.StickerCodeParser;
import com.bytedance.labcv.demo.utils.ToasUtils;
import com.bytedance.labcv.demo.view.TipManager;
import com.bytedance.labcv.demo.view.VideoButton;
import com.bytedance.labcv.demo.view.ViewfinderView;
import com.bytedance.labcv.effectsdk.BefDistanceInfo;
import com.bytedance.labcv.effectsdk.BefFaceInfo;
import com.bytedance.labcv.effectsdk.BefHandInfo;
import com.bytedance.labcv.effectsdk.BefPetFaceInfo;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.library.FileUtils;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;
import com.google.zxing.Result;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.effect.OnCloseListener;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.Arrays;

import static com.bytedance.labcv.effectsdk.FaceVerify.SAME_FACE_SCORE;


public class MainActivity extends FragmentActivity implements View.OnClickListener,
        DownloadUtil.DownloadListener, RepeatedScannerHandler.RepeatedScannerCallback
, RepeatedVerifyHandler.RepeatedVerifyCallback, View.OnLongClickListener {
    // 标志面板类型，分别是 识别、特效、贴纸
    public static final String TAG_IDENTIFY = "identify";
    public static final String TAG_EFFECT = "effect";
    public static final String TAG_STICKER = "sticker";

    public static final int ANIMATOR_DURATION = 400;

    private IdentifyFragment mIdentifyFragment;
    private EffectFragment mEffectFragment;
    private StickerFragment mStickerFragment;
    // 正在处于功能可用状态的面板
    private OnCloseListener mWorkingFragment;

    private int[] mPreviewsizes = new int[] {720, 1280};

    private FaceInfoFragment faceInfoFragment;
    private TextView mFpsTextView;

    private CameraSurfaceView mSurfaceView;
    private EffectRenderHelper effectRenderHelper;

    private TipManager mTipManager = new TipManager();
    private FrameLayout mSurfaceContainer;

    private FrameLayout mTipContainer;

    private View rootView;

    // 展示人脸检测结果的控件
    private LinearLayout llFaceVerify;
    private TextView tvSimilarityFaceVerify;
    private TextView tvCostFaceVerify;
    private TextView tvResultFaceVerify;;

    private ViewfinderView mFinderView;

    private Context mContext;

    private RepeatedScannerHandler mQrScannerHandler;
    private RepeatedVerifyHandler mFaceVerifyHandler;


    private ProgressDialog progressDialog;

    private DecimalFormat df = new DecimalFormat("0.00");


    private LinearLayout llFeature;
    private LinearLayout llIdentify;
    private LinearLayout llEffect;
    private LinearLayout llSticker;
    private ImageView ivFaceVerifyShow;

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

    private IVideoRecord mVideoRecord;

    private static final int UPDATE_INFO = 1;
    // 文件下载成功
    private static final int DOWNLOAD_SUCCESS = 2;
    // 文件下载失败
    private static final int DOWNLOAD_FAIL = 3;
    // 文件解压失败
    private static final int UNZIP_FAIL = 4;
    // 文件校验失败 主要检测是否有license
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
        CameraDevice.get().setMainActivity(this);

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
//        llIdentify = findViewById(R.id.ll_identify);
        llEffect = findViewById(R.id.ll_effect);
        llSticker = findViewById(R.id.ll_sticker);
        ivFaceVerifyShow = findViewById(R.id.iv_face_verify_show);

        llFaceVerify = findViewById(R.id.ll_face_verify);
        tvSimilarityFaceVerify = findViewById(R.id.tv_similarity_face_verify);
        tvCostFaceVerify = findViewById(R.id.tv_cost_face_verify);
        tvResultFaceVerify = findViewById(R.id.tv_result_face_verify);

        mFinderView = findViewById(R.id.qrcode_finder);
        mImageView = findViewById(R.id.img);
        tvInfo = findViewById(R.id.tv_info);
        tvcameraInfo = findViewById(R.id.camera_info);
        mSurfaceView = findViewById(R.id.gl_surface);
        effectRenderHelper = mSurfaceView.getEffectRenderHelper();
        effectRenderHelper.setMainActivity(MainActivity.this);
        mSurfaceContainer = findViewById(R.id.surface_container);

        mTipContainer = findViewById(R.id.tip_container);
        mTipManager.init(MainActivity.this, mTipContainer);

        findViewById(R.id.iv_change_camera).setOnClickListener(this);
        findViewById(R.id.iv_qr_code).setOnClickListener(this);
        vbTakePic = findViewById(R.id.btn_take_pic);
        vbTakePic.setOnClickListener(this);
        vbTakePic.setOnLongClickListener(this);
        llIdentify.setOnClickListener(this);
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

        initEffectHelper();

        // 初始化扫码管理器
        mQrScannerHandler = new RepeatedScannerHandler( this);
        mFaceVerifyHandler = new RepeatedVerifyHandler(this,10, ResourceHelper.getLicensePath(mContext), this );
        mVideoRecord = new VideoRecord();

        if (!AppUtils.isDebug()) {
            vbTakePic.setVisibility(View.GONE);
        }
    }

    private void switchCamera() {
        mSurfaceView.onPause();
        if (AppUtils.isDebug()) {
            effectRenderHelper.mCameraBitmap = null;
        }
        CameraDevice.get().switchCamera();
        changePreviewSize(mPreviewsizes[1] >= 1280, mPreviewsizes[0], mPreviewsizes[1]);
    }

    private void changePreviewSize(final boolean full, final int width, final int height) {
        mSurfaceView.onPause();
        mSurfaceContainer.removeAllViews();

        DisplayMetrics dm = getResources().getDisplayMetrics();

        final int viewW = dm.widthPixels;
        final int viewH = (full ? dm.heightPixels : (int) (dm.widthPixels * 4.0f / 3));

        mSurfaceContainer.addView(mSurfaceView, new FrameLayout.LayoutParams(viewW, viewH));

        float ratio = 1F * viewH / viewW;
        CameraDevice.get().setPreviewSize(width, height);
        mSurfaceView.onResume();
    }


    /**
     * 根据 TAG 创建对应的 Fragment
     * @param tag Fragment 对应的 tag
     * @return 创建的 Fragment
     */
    private Fragment generateFragment(String tag) {
        switch (tag) {
            case TAG_IDENTIFY:
                IdentifyFragment identifyFragment = new IdentifyFragment();
                identifyFragment.setCallback(new IdentifyFragment.IIdentifyCallback() {
                    @Override
                    public void petFaceOn(boolean flag) {
                        effectRenderHelper.setPetDetectOn(flag);
                        if (null != mTipManager) {
                            if (flag) {
                                mTipManager.addPetFaceTip();

                            } else {
                                mTipManager.removePetFaceTip();
                            }
                        }
                        if (flag) {
                            onFragmentWorking(mIdentifyFragment);
                        } else {
                            if (faceInfoFragment != null) {
                                faceInfoFragment.onClose();
                            }
                        }
                    }

                    @Override
                    public void face106On(boolean flag) {
                        hideAndShowFaceAction(!flag);
                        effectRenderHelper.setFaceDetectOn(flag);
                        if (flag) {
                            onFragmentWorking(mIdentifyFragment);
                        } else {
                            if (faceInfoFragment != null) {
                                faceInfoFragment.onClose();
                            }
                        }
                    }

                    @Override
                    public void faceAttrOn(boolean flag) {
                        effectRenderHelper.setFaceAttriOn(flag);
                    }

                    @Override
                    public void faceExtraOn(boolean flag) {
                        effectRenderHelper.setFaceExtraOn(flag);
                    }


                    @Override
                    public void handDetectOn(boolean flag) {
                        effectRenderHelper.setHandDetectOn(flag);
                        if (null != mTipManager) {
                            if (flag) {
                                mTipManager.addHandTip();

                            } else {
                                mTipManager.removeHandTip();
                            }
                        }
                        if (flag) {
                            onFragmentWorking(mIdentifyFragment);
                        }
                    }

                    @Override
                    public void skeletonDetectOn(boolean flag) {
                        effectRenderHelper.setSkeletonOn(flag);
                        if (flag) {
                            onFragmentWorking(mIdentifyFragment);
                        }
                    }

                    @Override
                    public void portraitMattingOn(final boolean flag) {
                        if (null != mSurfaceView) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.setPortraitMattingOn(flag);

                                }
                            });
                        }
                        if (flag) {
                            onFragmentWorking(mIdentifyFragment);
                        }
                    }

                    @Override
                    public void hairParserOn(final boolean flag) {
                        if (null != mSurfaceView) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.setParsingHair(flag);
                                }
                            });
                        }
                        if (flag) {
                            onFragmentWorking(mIdentifyFragment);
                        }
                    }

                    @Override
                    public void onPicChoose(Bitmap bitmap) {
                        if (null != mSurfaceView) {
                            mFaceVerifyHandler.setOriginalBitmap(bitmap);
                            ivFaceVerifyShow.setImageBitmap(bitmap);
                        }
                    }

                    @Override
                    public void faceVerifyOn(final boolean flag) {
                        if (null != mSurfaceView) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.setFaceVerify(flag);
                                }
                            });
                        }
                        if (flag) {
                            mFaceVerifyHandler.resume();
                        } else {
                            mFaceVerifyHandler.pause();
                        }
                        ivFaceVerifyShow.setVisibility(flag ? View.VISIBLE : View.GONE);
                        llFaceVerify.setVisibility(flag ? View.VISIBLE : View.GONE);
                        if (flag) {
                            onFragmentWorking(mIdentifyFragment);
                        }
                    }

                    @Override
                    public void distanceOn(final boolean flag) {
                        if (mSurfaceView != null) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.setHumanDistOn(flag);
                                }
                            });
                        }
                        if (flag) {
                            onFragmentWorking(mIdentifyFragment);
                        }
                    }
                });
                mIdentifyFragment = identifyFragment;
                return identifyFragment;
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

//                    @Override
//                    public void updateComposeNodeIntensity(final String node, final String key, final float value) {
//                        if (mSurfaceView != null) {
//                            mSurfaceView.queueEvent(new Runnable() {
//                                @Override
//                                public void run() {
//                                    effectRenderHelper.updateComposeNode(mContext, node, key, value);
//                                }
//                            });
//                        }
//                    }

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


                    //                    @Override
//                    public void faceMakeUpOn(final boolean flag) {
//                        if (null != mSurfaceView) {
//                            mSurfaceView.queueEvent(new Runnable() {
//                                @Override
//                                public void run() {
//                                    onMakeUpClicked(flag);
//                                }
//                            });
//                        }
//                        if (flag) {
//                            onFragmentWorking(mEffectFragment);
//                        }
//                    }

//                    @Override
//                    public void updateBeautyIntensity(final BytedEffectConstants.IntensityType intensitytype, final float intensity) {
//                        if (null != mSurfaceView) {
//                            mSurfaceView.queueEvent(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if(!effectRenderHelper.updateIntensity(intensitytype, intensity)){
//                                        LogUtils.e("updateIntensity return error!!");
//                                    }
//                                }
//                            });
//                        }
//                    }
//
//                    @Override
//                    public void updateReshapeIntensity(final float cheekintensity, final float eyeintensity) {
//                        if (null != mSurfaceView) {
//                            mSurfaceView.queueEvent(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (!effectRenderHelper.updateReshapeIntensity(cheekintensity, eyeintensity)) {
//                                        LogUtils.e("updateReshapeIntensity return error!!");
//                                    }
//                                }
//                            });
//                        }
//                    }
//
//                    @Override
//                    public void faceBeautyOn(final boolean flag) {
//                        if (null != mSurfaceView) {
//                            mSurfaceView.queueEvent(new Runnable() {
//                                @Override
//                                public void run() {
//                                    onBeautyClicked(flag);
//
//                                }
//                            });
//                        }
//                        if (flag) {
//                            onFragmentWorking(mEffectFragment);
//                        }
//                    }

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
     * @param tag 用于标志 Fragment 的 tag {@value TAG_IDENTIFY}
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
     * @return 是否成功关闭某个面板，即是否有面板正在开启中
     */
    private boolean closeFeature() {
        boolean hasFeature = false;

        Fragment showedFragment = null;
        if (mIdentifyFragment != null && !mIdentifyFragment.isHidden()) {
            showedFragment = mIdentifyFragment;
            hasFeature = true;
        } else if (mEffectFragment != null && !mEffectFragment.isHidden()) {
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

    /**
     * 控制显示或隐藏二维码扫描器
     */
    public void hideAndShowQr(final boolean flag) {
        isShowQr = flag;
        if (null != mFinderView) {
            mFinderView.setVisibility(flag ? View.VISIBLE : View.GONE);
        }
        if (null != mSurfaceView) {
            mSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    effectRenderHelper.setQrDecoding(flag);
                    if (flag) {
                        mQrScannerHandler.resume();
                    } else {
                        mQrScannerHandler.pause();
                    }

                }
            });
        }
    }

    /**
     * 控制显示或者隐藏人脸检测结果
     *
     * @param hide
     */
    private void hideAndShowFaceAction(boolean hide) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        faceInfoFragment = (FaceInfoFragment) fm.findFragmentByTag("action");

        if (hide) {
            if (null != faceInfoFragment) {
                ft.hide(faceInfoFragment).commit();
            }
        } else {
            if (null == faceInfoFragment) {
                faceInfoFragment = new FaceInfoFragment();
                ft.replace(R.id.faceinfotrans, faceInfoFragment, "action").commit();

            } else {
                ft.show(faceInfoFragment).commit();
            }
        }
    }

    /**
     * 注册检测结果回调 将结果信息返回到MainActivity中展示
     */
    private void initEffectHelper() {
        if (null == effectRenderHelper) return;
        effectRenderHelper.addResultCallback(new EffectRenderHelper.ResultCallback<BefFaceInfo>() {
            @Override
            public void doResult(final BefFaceInfo befFaceInfo, int framecount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        faceInfoFragment.updateProperty(befFaceInfo, effectRenderHelper.isFaceAttrOn());


                    }
                });

            }
        });

        effectRenderHelper.addResultCallback(new EffectRenderHelper.ResultCallback<BefHandInfo>() {
            @Override
            public void doResult(final BefHandInfo handInfo, int framecount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null == mSurfaceView || null == mTipManager || null == mContext) return;
                        float radio = effectRenderHelper.getRadio();
                        int glpreviewWidth = Float.valueOf(CameraDevice.get().getPreviewWidth() * radio).intValue();
                        int glpreviewHeight = Float.valueOf(CameraDevice.get().getPreviewHeight() * radio).intValue();
                        int sufaceViewHeight = mSurfaceView.getHeight();
                        int sufaceViewWidth = mSurfaceView.getWidth();
                        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            mTipManager.updateHandInfo(handInfo, glpreviewHeight, glpreviewWidth, sufaceViewHeight, sufaceViewWidth);

                        } else
                            {
                            mTipManager.updateHandInfo(handInfo, glpreviewWidth, glpreviewHeight, sufaceViewHeight, sufaceViewWidth);

                        }
                    }
                });

            }
        });

        effectRenderHelper.addResultCallback(new EffectRenderHelper.ResultCallback<BefPetFaceInfo>() {
            @Override
            public void doResult(final BefPetFaceInfo petFaceInfo, int framecount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null == mSurfaceView || null == mTipManager || null == mContext) return;
                        float radio = effectRenderHelper.getRadio();
                        int glpreviewWidth = Float.valueOf(CameraDevice.get().getPreviewWidth() * radio).intValue();
                        int glpreviewHeight = Float.valueOf(CameraDevice.get().getPreviewHeight() * radio).intValue();
                        int sufaceViewHeight = mSurfaceView.getHeight();
                        int sufaceViewWidth = mSurfaceView.getWidth();
                        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            mTipManager.updatePetFaceInfo(petFaceInfo, glpreviewHeight, glpreviewWidth, sufaceViewHeight, sufaceViewWidth);

                        } else {
                            mTipManager.updatePetFaceInfo(petFaceInfo, glpreviewWidth, glpreviewHeight, sufaceViewHeight, sufaceViewWidth);

                        }
                    }
                });

            }
        });

        effectRenderHelper.addResultCallback(new EffectRenderHelper.ResultCallback<BefDistanceInfo>() {
            @Override
            public void doResult(final BefDistanceInfo distanceResult, int framecount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null == mSurfaceView || null == mTipManager || null == mContext) return;
                        float radio = effectRenderHelper.getRadio();
                        int glpreviewWidth = Float.valueOf(CameraDevice.get().getPreviewWidth() * radio).intValue();
                        int glpreviewHeight = Float.valueOf(CameraDevice.get().getPreviewHeight() * radio).intValue();
                        int sufaceViewHeight = mSurfaceView.getHeight();
                        int sufaceViewWidth = mSurfaceView.getWidth();
                        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

                                mTipManager.updateDistanceInfo(distanceResult, glpreviewHeight, glpreviewWidth, sufaceViewHeight, sufaceViewWidth);

                        } else {
                            mTipManager.updateDistanceInfo(distanceResult, glpreviewWidth, glpreviewHeight, sufaceViewHeight, sufaceViewWidth);

                        }
                    }
                });

            }
        });

        effectRenderHelper.addResultCallback(new EffectRenderHelper.ResultCallback<FaceVerifyResult>() {
            @Override
            void doResult(final FaceVerifyResult faceVerifyResult, int framecount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null == mSurfaceView || null == mTipManager
                                || null == mContext || null == tvSimilarityFaceVerify || null == tvCostFaceVerify) return;
                        if (null == faceVerifyResult) {
                            tvSimilarityFaceVerify.setText("0.00");
                            tvCostFaceVerify.setText("0ms");
                            tvResultFaceVerify.setText("");
                        }else {
                            tvSimilarityFaceVerify.setText(df.format(faceVerifyResult.getSimilarity()));
                            tvCostFaceVerify.setText(faceVerifyResult.getCost()+"ms");
                            if (SAME_FACE_SCORE.compareTo(faceVerifyResult.getSimilarity()) < 0){
                                tvResultFaceVerify.setText(R.string.face_verify_detect);
                            }else{
                                tvResultFaceVerify.setText(R.string.face_verify_no_detect);
                            }
                        }

                    }
                });


            }

        });
    }


//    private void onBeautyClicked(boolean flag) {
//        if (null == effectRenderHelper) return;
//        if (!flag) {
//            effectRenderHelper.setBeauty("");
//            effectRenderHelper.setReshape("");
//
//        } else {
//            File[] reshapes = ResourceHelper.getReshapeResource();
//            File[] beautys = ResourceHelper.getBeautyResources();
//
//            if (beautys != null && beautys.length > 0) {
//                effectRenderHelper.setBeauty(beautys[0].getAbsolutePath());
//            }
//
//            if (reshapes != null && reshapes.length > 0) {
//                boolean ret = effectRenderHelper.setReshape(reshapes[0].getAbsolutePath());
//
//            }
//        }
//    }

//    private void onMakeUpClicked(boolean flag) {
//        if (null == effectRenderHelper) return;
//        if (!flag) {
//            effectRenderHelper.setComposeNodes(new String[0]);
//        }
//    }

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
        CameraDevice.get().setPreviewSize(0, 0);
        super.onDestroy();
        mSurfaceView = null;
//        featureBoardFragment = null;
        mEffectFragment = null;
        mIdentifyFragment = null;
        mStickerFragment = null;
        mQrScannerHandler.release();
        mFaceVerifyHandler.release();
        mVideoRecord.release();
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
                if (!isStartedRecord) {
                    takePic();
                } else {
                    stopRecord();
                }
                break;
            case R.id.iv_qr_code:
                hideAndShowQr(!isShowQr);
                break;
//            case R.id.ll_identify :
//                showFeature(TAG_IDENTIFY);
//                break;
            case R.id.ll_effect:
                showFeature(TAG_EFFECT);
                break;
            case R.id.ll_sticker:
                showFeature(TAG_STICKER);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take_pic:
                if (!isStartedRecord) {
                    startRecord();
                }
                break;
        }
        return true;
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
                    CaptureResult captureResult= effectRenderHelper.capture();

                    if (null == captureResult || captureResult.getWidth() == 0 || captureResult.getHeight() == 0|| null == captureResult.getByteBuffer()){
                        mHandler.sendEmptyMessage(CAPTURE_FAIL);

                    }else {
                        Message msg = mHandler.obtainMessage(CAPTURE_SUCCESS, captureResult);
                        mHandler.sendMessage(msg);
                    }
                }
            });
        }
    }

    private void startRecord() {
        isStartedRecord = true;
        mVideoPath = FileUtils.generateVideoFile();
        mVideoRecord.start(mVideoPath, AppUtils.isTv(this));
        vbTakePic.start();
    }

    private void stopRecord() {
        isStartedRecord = false;
        mVideoRecord.stop();
        vbTakePic.stop();
        ToasUtils.show("视频保存在：" + mVideoPath);
    }


    @Override
    public void onDownloadSuccess(String dir, String path) {
        LogUtils.d("onDownloadSuccess " + path);
        if (null == mHandler) return;
        mHandler.sendEmptyMessage(DOWNLOAD_SUCCESS);
        dir = dir+System.currentTimeMillis();
        final String dstDir = DownloadStickerManager.getStickerPath(mContext, dir);
        if (DownloadStickerManager.unzip(path, dstDir)) {
            final String licensePath = DownloadStickerManager.getLicensePath(dstDir);
            if (TextUtils.isEmpty(licensePath)){
                LogUtils.e("license file not found in sticker!!");
                mHandler.sendEmptyMessage(FILE_CHECK_FAIL);
                return;
            }
            LogUtils.d("licensePath =" + licensePath);
            LogUtils.d("stickerPath =" + dstDir);


            if (null != mSurfaceView) {
                mSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        int ret = effectRenderHelper.initTest(mContext, licensePath);

                        if (ret != BytedEffectConstants.BytedResultCode.BEF_RESULT_SUC) {
                            LogUtils.e("effectRenderHelper.initTest failure!! ret =" + ret);
                            mHandler.sendEmptyMessage(LICENSE_CHECK_FAIL);
                            return;
                        }
                        effectRenderHelper.setSticker("");
                        boolean result = effectRenderHelper.setSticker(dstDir);
                        if (result){
                            mHandler.sendEmptyMessage(STICKER_LOAD_SUCCESS);
                        } else {
                            mHandler.sendEmptyMessage(STICKER_LOAD_FAIL);
                            LogUtils.e("setSticker return " + result);


                        }


                    }
                });
            }

        } else {
                mHandler.sendEmptyMessage(UNZIP_FAIL);
            LogUtils.e("sticker unizp fail!!");


        }

    }

    @Override
    public void onDownloading(int progress) {
        updateProgressDialog(progress);

    }

    @Override
    public void onDownloadFail() {
        LogUtils.e("onDownloadFail");
        if (null != mHandler) {
            mHandler.sendEmptyMessage(DOWNLOAD_FAIL);

        }

    }

    @Override
    public void onDecodeSuccess(Result result) {
        String id = StickerCodeParser.parseId(result);
        LogUtils.d(" sticker id =" + id);
        if (TextUtils.isEmpty(id)) {
            ToasUtils.show(getString(R.string.sticker_parse_fail));
            mQrScannerHandler.resume();
            return;
        }

        hideAndShowQr(false);
        if (!NetworkUtil.isNetworkConnected(MainActivity.this)){
            ToasUtils.show(getString(R.string.network_error));
            return;
        }
        showProgressDialog(getString(R.string.download_sticker));
        DownloadUtil.get().download(id, ResourceHelper.getDownloadedStickerDir(mContext), this);

    }

    @Override
    public void onVerifyCallback(FaceVerifyResult result) {
        effectRenderHelper.dispatchResult(FaceVerifyResult.class, result, 0);

    }

    @Override
    public void requestDecodeFrame(Message msg) {
        effectRenderHelper.requestPreviewFrame(msg);

    }

    @Override
    public void requestVerifyFrame(Message msg) {
        effectRenderHelper.requestPreviewFrame(msg);


    }

    @Override
    public void onPicChoose(int validNum) {
        if (validNum < 1){
            ToasUtils.show(getString(R.string.no_face_detected));
        } else if (validNum > 1){
            ToasUtils.show(getString(R.string.face_more_than_one));

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
