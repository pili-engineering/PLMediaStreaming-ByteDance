package com.qiniu.pili.droid.streaming.effect;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.bytedance.labcv.demo.CameraDevice;
import com.bytedance.labcv.demo.EffectRenderHelper;
import com.bytedance.labcv.demo.camera.CameraListener;
import com.bytedance.labcv.demo.camera.CameraProxy;
import com.bytedance.labcv.demo.fragment.EffectFragment;
import com.bytedance.labcv.demo.fragment.StickerFragment;
import com.bytedance.labcv.demo.model.ComposerNode;
import com.bytedance.labcv.demo.opengl.GlUtil;
import com.bytedance.labcv.demo.opengl.ShaderHelper;
import com.bytedance.labcv.demo.utils.AppUtils;
import com.bytedance.labcv.demo.utils.CommonUtils;
import com.bytedance.labcv.demo.utils.ToasUtils;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.StreamStatusCallback;
import com.qiniu.pili.droid.streaming.StreamingManager;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;
import com.qiniu.pili.droid.streaming.av.common.PLFourCC;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.demo.core.ExtAudioCapture;
import com.qiniu.pili.droid.streaming.demo.ui.CameraPreviewFrameView;
import com.qiniu.pili.droid.streaming.demo.utils.Config;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY;

public class BeautyImportStreamingActivity extends FragmentActivity implements StreamingSessionListener, StreamStatusCallback, StreamingStateChangedListener, View.OnClickListener {
    private static final String TAG = "ImportStreamingActivity";

    private CameraPreviewFrameView mSurfaceView;

    private ExtAudioCapture mExtAudioCapture;

    private StreamingManager mStreamingManager;
    private StreamingProfile mProfile;

    public static final String TAG_EFFECT = "effect";
    public static final String TAG_STICKER = "sticker";

    public static final int ANIMATOR_DURATION = 400;

    private LinearLayout llFeature;
    private LinearLayout llEffect;
    private LinearLayout llSticker;
    private EffectFragment mEffectFragment;
    private StickerFragment mStickerFragment;
    // 正在处于功能可用状态的面板
    // current panel
    private OnCloseListener mWorkingFragment;
    private EffectRenderHelper effectRenderHelper;
    private CameraProxy mCameraProxy;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExtAudioCapture = new ExtAudioCapture();
        initView();

        llFeature = findViewById(R.id.ll_feature);
        llEffect = findViewById(R.id.ll_effect);
        llSticker = findViewById(R.id.ll_sticker);
        llEffect.setOnClickListener(this);
        llSticker.setOnClickListener(this);
        effectRenderHelper = new EffectRenderHelper(this);

        initStreamingManager();
        initGL();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsPaused = false;
        mCameraProxy.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT, new CameraListener() {
            @Override
            public void onOpenSuccess() {
                mSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        LogUtils.d("onOpenSuccess");
                        //获取纹理id,绑定纹理信息
                        int mImageHeight = mCameraProxy.getPreviewHeight();
                        int mImageWidth = mCameraProxy.getPreviewWidth();
                        if (mCameraProxy.getOrientation()%180 == 90){
                            effectRenderHelper.setImageSize(mImageHeight, mImageWidth);
                        } else {
                            effectRenderHelper.setImageSize(mImageWidth, mImageHeight);

                        }
                        boolean flipHoriontal = mCameraProxy.isFlipHorizontal();
                        effectRenderHelper.adjustTextureBuffer(mCameraProxy.getOrientation(),flipHoriontal, false);
                        GLES20.glEnable(GL10.GL_DITHER);
                        GLES20.glClearColor(0, 0, 0, 0);
                        effectRenderHelper.initSDKModules();
                        effectRenderHelper.recoverStatus(BeautyImportStreamingActivity.this);
                        mCameraProxy.startPreview(new SurfaceTexture.OnFrameAvailableListener() {
                            @Override
                            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                                mSurfaceView.requestRender();
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mStreamingManager.startStreaming();
                            }
                        }).start();
                    }
                });
            }

            @Override
            public void onOpenFail() {

            }
        });
        mExtAudioCapture.startCapture();
        mExtAudioCapture.setOnAudioFrameCapturedListener(mOnAudioFrameCapturedListener);
        mStreamingManager.resume();
    }

    private volatile boolean mIsPaused = false;
    private void initGL() {
        mCameraProxy = new CameraProxy(this);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int width, int height) {
                if (mIsPaused) {
                    return;
                }
                effectRenderHelper.initViewPort(width, height);
            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                if (mIsPaused) {
                    return;
                }
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


                mCameraProxy.updateTexture();

                BytedEffectConstants.Rotation rotation = OrientationSensor.getOrientation();
                int dstTexture = effectRenderHelper.processTexture(mCameraProxy.getPreviewTexture(), rotation, getSurfaceTimeStamp());

                if (dstTexture != ShaderHelper.NO_TEXTURE) {
                    effectRenderHelper.drawFrame(dstTexture);
                }
                
                long time = SystemClock.currentThreadTimeMillis();
                ByteBuffer buffer = GlUtil.readPixlesBuffer(dstTexture, mCameraProxy.getPreviewWidth(), mCameraProxy.getPreviewHeight());
                if (buffer != null) {
                    mStreamingManager.inputVideoFrame(buffer, buffer.array().length, mCameraProxy.getPreviewWidth(), mCameraProxy.getPreviewHeight(), 0, false, PLFourCC.FOURCC_ABGR, time);
                }

            }
        });
        mSurfaceView.setRenderMode(RENDERMODE_WHEN_DIRTY);
    }



    @Override
    protected void onPause() {
        super.onPause();
        mIsPaused = true;
        mExtAudioCapture.stopCapture();
        mCameraProxy.releaseCamera();
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraProxy.deleteTexture();
                effectRenderHelper.destroySDKModules();
            }
        });
        mStreamingManager.stopStreaming();
        mStreamingManager.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStreamingManager.destroy();
    }


    public void initView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(Config.SCREEN_ORIENTATION);
        setContentView(R.layout.activity_test_beauty_import_streaming);
        mSurfaceView = (CameraPreviewFrameView) findViewById(R.id.ext_camerapreview_surfaceview);
    }


    protected void initStreamingManager() {
        mExtAudioCapture = new ExtAudioCapture();
        String publishURLFromServer;
        Intent intent = getIntent();
        publishURLFromServer = intent.getStringExtra("stream_publish_url");
        LogUtils.d("try connect "+ publishURLFromServer);
        try {
            //encoding setting
            mProfile = new StreamingProfile();
            mProfile.setPreferredVideoEncodingSize(400, 300);
            mProfile.setVideoQuality(StreamingProfile.VIDEO_QUALITY_HIGH1)
                    .setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2)
                    .setEncodingSizeLevel(StreamingProfile.VIDEO_ENCODING_HEIGHT_480)
                    .setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY)
                    .setPublishUrl(publishURLFromServer);

        }catch (Exception e){
            e.printStackTrace();
        }
        mStreamingManager = new StreamingManager(this, AVCodecType.SW_VIDEO_CODEC);
        mStreamingManager.prepare(mProfile);
        mStreamingManager.setStreamingSessionListener(this);
        mStreamingManager.setStreamStatusCallback(this);
        mStreamingManager.setStreamingStateListener(this);
    }


    private ExtAudioCapture.OnAudioFrameCapturedListener mOnAudioFrameCapturedListener = new ExtAudioCapture.OnAudioFrameCapturedListener() {
        @Override
        public void onAudioFrameCaptured(byte[] audioData) {
            long timestamp = System.nanoTime();
            mStreamingManager.inputAudioFrame(audioData, timestamp, false);
        }
    };


    @Override
    public void notifyStreamStatusChanged(StreamingProfile.StreamStatus status) {
        Log.e(TAG, "StreamStatus = " + status);
    }

    @Override
    public boolean onRecordAudioFailedHandled(int code) {
        Log.i(TAG, "onRecordAudioFailedHandled");
        return false;
    }
    @Override
    public boolean onRestartStreamingHandled(int code) {
        Log.i(TAG, "onRestartStreamingHandled");
        return false;
    }

    @Override
    public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
        final Camera.Size size = list.get(0);
        return size;
    }
    @Override
    public int onPreviewFpsSelected(List<int[]> list) {
        return -1;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onStateChanged(StreamingState streamingState, Object o) {

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
    public void onClick(View v) {
        LogUtils.d("onClick "+ v.toString());
        if (CommonUtils.isFastClick()) {
            ToasUtils.show("too fast click");
            return;
        }
        switch (v.getId()) {
            case R.id.ll_effect:
                showFeature(TAG_EFFECT);
                break;
            case R.id.ll_sticker:
                showFeature(TAG_STICKER);
                break;
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

    public double getSurfaceTimeStamp() {

        long cur_time_nano = System.nanoTime();
        long delta_nano_time = Math.abs(cur_time_nano - mCameraProxy.getTextureTime());
        long delta_elapsed_nano_time = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? Math.abs(SystemClock.elapsedRealtimeNanos() - mCameraProxy.getTextureTime()) : Long.MAX_VALUE;
        long delta_uptime_nano = Math.abs(SystemClock.uptimeMillis() * 1000000 - mCameraProxy.getTextureTime());
        double lastTimeStamp = cur_time_nano - Math.min(Math.min(delta_nano_time, delta_elapsed_nano_time), delta_uptime_nano);
        return lastTimeStamp / 1e9;
    }
}
