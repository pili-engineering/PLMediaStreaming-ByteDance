package com.qiniu.pili.droid.streaming.effect;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bytedance.labcv.demo.core.EffectRenderHelper;
import com.bytedance.labcv.demo.fragment.EffectFragment;
import com.bytedance.labcv.demo.fragment.StickerFragment;
import com.bytedance.labcv.demo.model.ComposerNode;
import com.bytedance.labcv.demo.opengl.GlUtil;
import com.bytedance.labcv.demo.utils.CommonUtils;
import com.bytedance.labcv.demo.utils.ToasUtils;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.FrameCapturedCallback;
import com.qiniu.pili.droid.streaming.MediaStreamingManager;
import com.qiniu.pili.droid.streaming.MicrophoneStreamingSetting;
import com.qiniu.pili.droid.streaming.StreamingPreviewCallback;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.SurfaceTextureCallback;
import com.qiniu.pili.droid.streaming.WatermarkSetting;
import com.qiniu.pili.droid.streaming.av.common.PLFourCC;
import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.demo.activity.StreamingBaseActivity;
import com.qiniu.pili.droid.streaming.demo.gles.FBO;
import com.qiniu.pili.droid.streaming.demo.plain.CameraConfig;
import com.qiniu.pili.droid.streaming.demo.ui.CameraPreviewFrameView;
import com.qiniu.pili.droid.streaming.demo.ui.RotateLayout;
import com.qiniu.pili.droid.streaming.demo.utils.Cache;
import com.qiniu.pili.droid.streaming.demo.utils.Config;
import com.qiniu.pili.droid.streaming.microphone.AudioMixer;
import com.qiniu.pili.droid.streaming.microphone.OnAudioMixListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TestBeautyStreamingActivity extends StreamingBaseActivity implements
        StreamingPreviewCallback,
        CameraPreviewFrameView.Listener,
        SurfaceTextureCallback, View.OnClickListener {
    private static final String TAG = "AVStreamingActivity";

    private CameraStreamingSetting mCameraStreamingSetting;
    private CameraConfig mCameraConfig;

    private Button mMuteButton;
    private Button mTorchBtn;
    private Button mCameraSwitchBtn;
    private Button mCaptureFrameBtn;
    private Button mEncodingOrientationSwitcherBtn;
    private Button mFaceBeautyBtn;
    private RotateLayout mRotateLayout;

    private Button mMixToggleBtn;
    private SeekBar mMixProgress;

    private boolean mIsTorchOn = false;
    private boolean mIsNeedMute = false;
    private boolean mIsNeedFB = false;
    private boolean mIsPreviewMirror = false;
    private boolean mIsEncodingMirror = false;
    private boolean mIsPlayingback = false;

    private int mCurrentZoom = 0;
    private int mMaxZoom = 0;
    private boolean mOrientationChanged = false;
    private int mCurrentCamFacingIndex;

    private FBO mFBO = new FBO();

    private ScreenShooter mScreenShooter = new ScreenShooter();
    private Switcher mSwitcher = new Switcher();
    private EncodingOrientationSwitcher mEncodingOrientationSwitcher = new EncodingOrientationSwitcher();
    private ImageSwitcher mImageSwitcher;

    private MediaStreamingManager mMediaStreamingManager;
    private AudioMixer mAudioMixer;
    private String mAudioFile;

    private Handler mHandler;
    private int mTimes = 0;
    private boolean mIsPictureStreaming = false;

    private CameraPreviewFrameView cameraPreviewFrameView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        llFeature = findViewById(R.id.ll_feature);
        llEffect = findViewById(R.id.ll_effect);
        llSticker = findViewById(R.id.ll_sticker);
        llEffect.setOnClickListener(this);
        llSticker.setOnClickListener(this);
        effectRenderHelper = new EffectRenderHelper(this);
        mMediaStreamingManager.setSurfaceTextureCallback(new SurfaceTextureCallback() {
            private boolean isInit = false;

            @Override
            public void onSurfaceCreated() {

            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                effectRenderHelper.onSurfaceChanged(width, height);
            }

            @Override
            public void onSurfaceDestroyed() {

            }

            @Override
            public int onDrawFrame(int texId, int width, int height, float[] floats) {
//                if (!isInit){
//                    effectRenderHelper.initEffectSDK(width, height);
//                    isInit = true;
//                }

                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                BytedEffectConstants.Rotation rotation = OrientationSensor.getOrientation();
                long time = SystemClock.currentThreadTimeMillis();
                int dstTexture = effectRenderHelper.processTexure(texId, BytedEffectConstants.TextureFormat.Texture_Oes, width, height, 0, true,  rotation, time);
//        mEffectRenderHelper.drawFrame(mCameraProxy.getPreviewTexture(),BytedEffectConstants.TextureFormat.Texture_Oes, mCameraProxy.getPreviewWidth(),mCameraProxy.getPreviewHeight(), 360- mCameraProxy.getOrientation(), mCameraProxy.isFrontCamera(),false);
//                if (dstTexture != GlUtil.NO_TEXTURE){
//                    effectRenderHelper.drawFrame(dstTexture, BytedEffectConstants.TextureFormat.Texure2D, width, height, 360- 0, true,false);
//                }

//        mbitmap = BitmapUtils.bitmapFromGLTexture(dstTexture, width, height, true);
//        beautySDKFragment.showPreTemp(mbitmap);
                if (dstTexture != GlUtil.NO_TEXTURE){
                    return dstTexture;
                }else{
                    LogUtils.d("dstTexture = GlUtil.NO_TEXTURE");
                    return texId;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaStreamingManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        normalPause();
    }

    private void normalPause() {
        mIsReady = false;
        mShutterButtonPressed = false;
        mIsPictureStreaming = false;
        if (mHandler != null) {
            mHandler.getLooper().quit();
        }
        cameraPreviewFrameView.queueEvent(new Runnable() {
            @Override
            public void run() {
                effectRenderHelper.destroyEffectSDK();
            }
        });
        mMediaStreamingManager.pause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaStreamingManager.destroy();
    }

    @Override
    protected void initStreamingManager() {

        cameraPreviewFrameView = (CameraPreviewFrameView) findViewById(R.id.cameraPreview_surfaceView);
        mMediaStreamingManager = new MediaStreamingManager(this, cameraPreviewFrameView, mEncodingConfig.mCodecType);
        if (mEncodingConfig.mIsPictureStreamingEnabled) {
            if (mEncodingConfig.mPictureStreamingFilePath == null) {
                mProfile.setPictureStreamingResourceId(R.drawable.pause_publish);
            } else {
                mProfile.setPictureStreamingFilePath(mEncodingConfig.mPictureStreamingFilePath);
            }
        }
        MicrophoneStreamingSetting microphoneStreamingSetting = null;
        if (mAudioStereoEnable) {
            /**
             * Notice !!! {@link AudioFormat#CHANNEL_IN_STEREO} is NOT guaranteed to work on all devices.
             */
            microphoneStreamingSetting = new MicrophoneStreamingSetting();
            microphoneStreamingSetting.setChannelConfig(AudioFormat.CHANNEL_IN_STEREO);
        }
        mMediaStreamingManager.prepare(mCameraStreamingSetting, microphoneStreamingSetting, buildWatermarkSetting(), mProfile);
        mMediaStreamingManager.setAutoRefreshOverlay(true);
        if (mCameraConfig.mIsCustomFaceBeauty) {
            mMediaStreamingManager.setSurfaceTextureCallback(this);
        }
        cameraPreviewFrameView.setListener(this);
        mMediaStreamingManager.setStreamingSessionListener(this);
        mMediaStreamingManager.setStreamStatusCallback(this);
        mMediaStreamingManager.setAudioSourceCallback(this);
        mMediaStreamingManager.setStreamingStateListener(this);


        mAudioMixer = mMediaStreamingManager.getAudioMixer();
        mAudioMixer.setOnAudioMixListener(new OnAudioMixListener() {
            @Override
            public void onStatusChanged(MixStatus mixStatus) {
                mMixToggleBtn.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TestBeautyStreamingActivity.this, "mix finished", Toast.LENGTH_LONG).show();
                        updateMixBtnText();
                    }
                });
            }

            @Override
            public void onProgress(long l, long l1) {
                mMixProgress.setProgress((int) l);
                mMixProgress.setMax((int) l1);
            }
        });
        mAudioFile = Cache.getAudioFile(this);
        if (mAudioFile != null) {
            try {
                mAudioMixer.setFile(mAudioFile, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected boolean startStreaming() {
        return mMediaStreamingManager.startStreaming();
    }

    @Override
    protected boolean stopStreaming() {
        return mMediaStreamingManager.stopStreaming();
    }



    private class EncodingOrientationSwitcher implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "mIsEncOrientationPort:" + mIsEncOrientationPort);
            mOrientationChanged = true;
            mIsEncOrientationPort = !mIsEncOrientationPort;
            mProfile.setEncodingOrientation(mIsEncOrientationPort ? StreamingProfile.ENCODING_ORIENTATION.PORT : StreamingProfile.ENCODING_ORIENTATION.LAND);
            mMediaStreamingManager.setStreamingProfile(mProfile);
            stopStreamingInternal();
            setRequestedOrientation(mIsEncOrientationPort ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mMediaStreamingManager.notifyActivityOrientationChanged();
            updateOrientationBtnText();
            Toast.makeText(TestBeautyStreamingActivity.this, Config.HINT_ENCODING_ORIENTATION_CHANGED,
                    Toast.LENGTH_SHORT).show();
            Log.i(TAG, "EncodingOrientationSwitcher -");
        }
    }

    private class Switcher implements Runnable {
        @Override
        public void run() {
            mCurrentCamFacingIndex = (mCurrentCamFacingIndex + 1) % CameraStreamingSetting.getNumberOfCameras();
            CameraStreamingSetting.CAMERA_FACING_ID facingId;
            if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK.ordinal()) {
                facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
            } else if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal()) {
                facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
            } else {
                facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
            }
            Log.i(TAG, "switchCamera:" + facingId);
            mMediaStreamingManager.switchCamera(facingId);

            mIsEncodingMirror = mCameraConfig.mEncodingMirror;
            mIsPreviewMirror = facingId == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT ? mCameraConfig.mPreviewMirror : false;
        }
    }

    private class ScreenShooter implements Runnable {
        @Override
        public void run() {
            final String fileName = "PLStreaming_" + System.currentTimeMillis() + ".jpg";
            mMediaStreamingManager.captureFrame(100, 100, new FrameCapturedCallback() {
                private Bitmap bitmap;
                @Override
                public void onFrameCaptured(Bitmap bmp) {
                    if (bmp == null) {
                        return;
                    }
                    bitmap = bmp;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                saveToSDCard(fileName, bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (bitmap != null) {
                                    bitmap.recycle();
                                    bitmap = null;
                                }
                            }
                        }
                    }).start();
                }
            });
        }
    }

    /**
     * switch picture during streaming
     */
    private class ImageSwitcher implements Runnable {
        @Override
        public void run() {
            if (!mIsPictureStreaming) {
                Log.d(TAG, "is not picture streaming!!!");
                return;
            }

            if (mTimes % 2 == 0) {
                if (mEncodingConfig.mPictureStreamingFilePath != null) {
                    mMediaStreamingManager.setPictureStreamingFilePath(mEncodingConfig.mPictureStreamingFilePath);
                } else {
                    mMediaStreamingManager.setPictureStreamingResourceId(R.drawable.qiniu_logo);
                }
            } else {
                mMediaStreamingManager.setPictureStreamingResourceId(R.drawable.pause_publish);
            }
            mTimes++;
            if (mHandler != null && mIsPictureStreaming) {
                mHandler.postDelayed(this, 1000);
            }
        }
    }

    private boolean isPictureStreaming() {
        if (mIsPictureStreaming) {
            Toast.makeText(TestBeautyStreamingActivity.this, "is picture streaming, operation failed!", Toast.LENGTH_SHORT).show();
        }
        return mIsPictureStreaming;
    }

    private void togglePictureStreaming() {
        boolean isOK = mMediaStreamingManager.togglePictureStreaming();
        if (!isOK) {
            Toast.makeText(TestBeautyStreamingActivity.this, "toggle picture streaming failed!", Toast.LENGTH_SHORT).show();
            return;
        }

        mIsPictureStreaming = !mIsPictureStreaming;

        mTimes = 0;
        if (mIsPictureStreaming) {
            if (mImageSwitcher == null) {
                mImageSwitcher = new ImageSwitcher();
            }

            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());
            mHandler.postDelayed(mImageSwitcher, 1000);
        } else {
            if (mHandler != null) {
                mHandler.getLooper().quit();
            }
        }
    }

    private void saveToSDCard(String filename, Bitmap bmp) throws IOException {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(file));
                bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                bmp.recycle();
                bmp = null;
            } finally {
                if (bos != null) bos.close();
            }

            final String info = "Save frame to:" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TestBeautyStreamingActivity.this, info, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Accept only 32 bit png (ARGB)
     * @return
     */
    private WatermarkSetting buildWatermarkSetting() {
        if (!mEncodingConfig.mIsWatermarkEnabled) {
            return null;
        }
        WatermarkSetting watermarkSetting = new WatermarkSetting(this);
        watermarkSetting.setResourceId(R.drawable.qiniu_logo);
        watermarkSetting.setAlpha(mEncodingConfig.mWatermarkAlpha);
        watermarkSetting.setSize(mEncodingConfig.mWatermarkSize);
        if (mEncodingConfig.mWatermarkCustomWidth != 0 || mEncodingConfig.mWatermarkCustomHeight != 0) {
            watermarkSetting.setCustomSize(mEncodingConfig.mWatermarkCustomWidth, mEncodingConfig.mWatermarkCustomHeight);
        }
        if (mEncodingConfig.mIsWatermarkLocationPreset) {
            watermarkSetting.setLocation(mEncodingConfig.mWatermarkLocationPreset);
        } else {
            watermarkSetting.setCustomPosition(mEncodingConfig.mWatermarkLocationCustomX, mEncodingConfig.mWatermarkLocationCustomY);
        }

        return watermarkSetting;
    }

    private CameraStreamingSetting buildCameraStreamingSetting() {
        mCameraConfig = (CameraConfig) getIntent().getSerializableExtra("CameraConfig");

        CameraStreamingSetting cameraStreamingSetting = new CameraStreamingSetting();
        cameraStreamingSetting.setCameraId(mCameraConfig.mFrontFacing ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK)
                .setCameraPrvSizeLevel(mCameraConfig.mSizeLevel)
                .setCameraPrvSizeRatio(mCameraConfig.mSizeRatio)
                .setFocusMode(mCameraConfig.mFocusMode)
                .setContinuousFocusModeEnabled(mCameraConfig.mContinuousAutoFocus)
                .setFrontCameraPreviewMirror(mCameraConfig.mPreviewMirror)
                .setFrontCameraMirror(mCameraConfig.mEncodingMirror).setRecordingHint(false)
                .setResetTouchFocusDelayInMs(3000)
                .setBuiltInFaceBeautyEnabled(!mCameraConfig.mIsCustomFaceBeauty)
                .setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(1.0f, 1.0f, 0.8f));

        if (false) {
            cameraStreamingSetting.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY);
        } else {
            cameraStreamingSetting.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_NONE);
        }

        return cameraStreamingSetting;
    }

    @Override
    public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
        /**
         * You should choose a suitable size to avoid image scale
         * eg: If streaming size is 1280 x 720, you should choose a camera preview size >= 1280 x 720
         */
        Camera.Size size = null;
        if (list != null) {
            StreamingProfile.VideoEncodingSize encodingSize = mProfile.getVideoEncodingSize(mCameraConfig.mSizeRatio);
            for (Camera.Size s : list) {
                if (s.width >= encodingSize.width && s.height >= encodingSize.height) {
                    if (mEncodingConfig.mIsVideoSizePreset) {
                        size = s;
                        Log.d(TAG, "selected size :" + size.width + "x" + size.height);
                    }
                    break;
                }
            }
        }
        //todo
        final Camera.Size finalSize = size;
        cameraPreviewFrameView.queueEvent(new Runnable() {
            @Override
            public void run() {
                //TODO
                effectRenderHelper.initEffectSDK(finalSize.width, finalSize.height);
                effectRenderHelper.recoverStatus();
            }
        });
        return size;
    }

    @Override
    public void initView() {
        mCameraStreamingSetting = buildCameraStreamingSetting();
        mIsEncOrientationPort = mEncodingConfig.mVideoOrientationPortrait;
        mIsNeedFB = mCameraConfig.mIsFaceBeautyEnabled;
        mIsPreviewMirror = mCameraConfig.mPreviewMirror;
        mIsEncodingMirror = mCameraConfig.mEncodingMirror;
        mCurrentCamFacingIndex = mCameraConfig.mFrontFacing ? 1 : 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setRequestedOrientation(mIsEncOrientationPort ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_test_beauty_streaming);

        mMuteButton = (Button) findViewById(R.id.mute_btn);
        mTorchBtn = (Button) findViewById(R.id.torch_btn);
        mCameraSwitchBtn = (Button) findViewById(R.id.camera_switch_btn);
        mCaptureFrameBtn = (Button) findViewById(R.id.capture_btn);
        mFaceBeautyBtn = (Button) findViewById(R.id.fb_btn);
        Button previewMirrorBtn = (Button) findViewById(R.id.preview_mirror_btn);
        Button encodingMirrorBtn = (Button) findViewById(R.id.encoding_mirror_btn);
        Button picStreamingBtn = (Button) findViewById(R.id.pic_streaming_btn);
        Button addOverlayBtn = (Button) findViewById(R.id.add_overlay_btn);

        mFaceBeautyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsNeedFB = !mIsNeedFB;
                mMediaStreamingManager.setVideoFilterType(mIsNeedFB ?
                        CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY
                        : CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_NONE);
                updateFBButtonText();
            }
        });

        mMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsNeedMute = !mIsNeedMute;
                mMediaStreamingManager.mute(mIsNeedMute);
                updateMuteButtonText();
            }
        });

        previewMirrorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPictureStreaming()) {
                    return;
                }

                mIsPreviewMirror = !mIsPreviewMirror;
                mMediaStreamingManager.setPreviewMirror(mIsPreviewMirror);
                Toast.makeText(TestBeautyStreamingActivity.this, "镜像成功", Toast.LENGTH_SHORT).show();
            }
        });

        encodingMirrorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPictureStreaming()) {
                    return;
                }

                mIsEncodingMirror = !mIsEncodingMirror;
                mMediaStreamingManager.setEncodingMirror(mIsEncodingMirror);
                Toast.makeText(TestBeautyStreamingActivity.this, "镜像成功", Toast.LENGTH_SHORT).show();
            }
        });

        picStreamingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProfile.setPictureStreamingFps(10);
                togglePictureStreaming();
            }
        });

        if (mEncodingConfig.mCodecType == AVCodecType.HW_VIDEO_SURFACE_AS_INPUT_WITH_HW_AUDIO_CODEC ||
                mEncodingConfig.mCodecType == AVCodecType.HW_VIDEO_SURFACE_AS_INPUT_WITH_SW_AUDIO_CODEC ||
                mEncodingConfig.mCodecType == AVCodecType.HW_VIDEO_WITH_HW_AUDIO_CODEC ||
                mEncodingConfig.mCodecType == AVCodecType.HW_VIDEO_CODEC) {
            addOverlayBtn.setVisibility(View.VISIBLE);
            addOverlayBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageView imageOverlay = new ImageView(TestBeautyStreamingActivity.this);
                    imageOverlay.setImageResource(R.drawable.qiniu_logo);
                    imageOverlay.setOnTouchListener(new ViewTouchListener(imageOverlay));
                    ((FrameLayout) findViewById(R.id.content)).addView(imageOverlay, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

                    mMediaStreamingManager.addOverlay(imageOverlay);
                    Toast.makeText(TestBeautyStreamingActivity.this, "双击删除贴图!", Toast.LENGTH_LONG).show();
                }
            });
        }

        mTorchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPictureStreaming()) {
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsTorchOn) {
                            mMediaStreamingManager.turnLightOff();
                        } else {
                            mMediaStreamingManager.turnLightOn();
                        }
                        mIsTorchOn = !mIsTorchOn;
                        setTorchEnabled(mIsTorchOn);
                    }
                }).start();
            }
        });

        mCameraSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPictureStreaming()) {
                    return;
                }

                mCameraSwitchBtn.removeCallbacks(mSwitcher);
                mCameraSwitchBtn.postDelayed(mSwitcher, 100);
            }
        });

        mCaptureFrameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPictureStreaming()) {
                    return;
                }

                mCaptureFrameBtn.removeCallbacks(mScreenShooter);
                mCaptureFrameBtn.postDelayed(mScreenShooter, 100);
            }
        });

        mEncodingOrientationSwitcherBtn = (Button) findViewById(R.id.orientation_btn);
        mEncodingOrientationSwitcherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPictureStreaming()) {
                    return;
                }

                mEncodingOrientationSwitcherBtn.removeCallbacks(mEncodingOrientationSwitcher);
                mEncodingOrientationSwitcherBtn.postDelayed(mEncodingOrientationSwitcher, 100);
            }
        });

        SeekBar seekBarBeauty = (SeekBar) findViewById(R.id.beautyLevel_seekBar);
        seekBarBeauty.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                CameraStreamingSetting.FaceBeautySetting fbSetting = mCameraStreamingSetting.getFaceBeautySetting();
                fbSetting.beautyLevel = progress / 100.0f;
                fbSetting.whiten = progress / 100.0f;
                fbSetting.redden = progress / 100.0f;

                mMediaStreamingManager.updateFaceBeautySetting(fbSetting);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        initButtonText();
        initAudioMixerPanel();
    }

    private class ViewTouchListener implements View.OnTouchListener {
        private float lastTouchRawX;
        private float lastTouchRawY;
        private boolean scale;
        private View mView;

        public ViewTouchListener(View view) {
            mView = view;
        }

        GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                ((FrameLayout) findViewById(R.id.content)).removeView(mView);
                mMediaStreamingManager.removeOverlay(mView);
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true;
            }
        };

        final GestureDetector gestureDetector = new GestureDetector(TestBeautyStreamingActivity.this, simpleOnGestureListener);

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }

            int action = event.getAction();
            float touchRawX = event.getRawX();
            float touchRawY = event.getRawY();
            float touchX = event.getX();
            float touchY = event.getY();

            if (action == MotionEvent.ACTION_DOWN) {
                boolean xOK = touchX >= v.getWidth() * 3 / 4 && touchX <= v.getWidth();
                boolean yOK = touchY >= v.getHeight() * 2 / 4 && touchY <= v.getHeight();
                scale = xOK && yOK;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                float deltaRawX = touchRawX - lastTouchRawX;
                float deltaRawY = touchRawY - lastTouchRawY;

                if (scale) {
                    // rotate
                    float centerX = v.getX() + (float) v.getWidth() / 2;
                    float centerY = v.getY() + (float) v.getHeight() / 2;
                    double angle = Math.atan2(touchRawY - centerY, touchRawX - centerX) * 180 / Math.PI;
                    v.setRotation((float) angle - 45);

                    // scale
                    float xx = (touchRawX >= centerX ? deltaRawX : -deltaRawX);
                    float yy = (touchRawY >= centerY ? deltaRawY : -deltaRawY);
                    float sf = (v.getScaleX() + xx / v.getWidth() + v.getScaleY() + yy / v.getHeight()) / 2;
                    v.setScaleX(sf);
                    v.setScaleY(sf);
                } else {
                    // translate
                    v.setTranslationX(v.getTranslationX() + deltaRawX);
                    v.setTranslationY(v.getTranslationY() + deltaRawY);
                }
            }

            if (action == MotionEvent.ACTION_UP) {
//                当 mMediaStreamingManager.setAutoRefreshOverlay(false) 时自动刷新关闭，建议在 UP 事件里进行手动刷新。
//                mMediaStreamingManager.refreshOverlay(v, false);
            }

            lastTouchRawX = touchRawX;
            lastTouchRawY = touchRawY;
            return true;
        }
    }

    private void initButtonText() {
        updateFBButtonText();
        updateCameraSwitcherButtonText(mCameraStreamingSetting.getReqCameraId());
        mCaptureFrameBtn.setText("Capture");
        updateFBButtonText();
        updateMuteButtonText();
        updateOrientationBtnText();
    }

    private void initAudioMixerPanel() {
        Button mixPanelBtn = (Button) findViewById(R.id.mix_panel_btn);
        mixPanelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View panel = findViewById(R.id.mix_panel);
                panel.setVisibility(panel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        mMixProgress = (SeekBar) findViewById(R.id.mix_progress);
        mMixProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mAudioMixer != null) {
                    mAudioMixer.seek(1.0f * seekBar.getProgress() / seekBar.getMax());
                }
            }
        });

        SeekBar mixVolume = (SeekBar) findViewById(R.id.mix_volume);
        mixVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mAudioMixer != null) {
                    mAudioMixer.setVolume(1.0f, 1.0f * seekBar.getProgress() / seekBar.getMax());
                }
            }
        });

        Button mixFileBtn = (Button) findViewById(R.id.mix_file_btn);
        mixFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = new File(DialogConfigs.STORAGE_DIR);
                properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = new String[]{"mp3"};

                FilePickerDialog dialog = new FilePickerDialog(TestBeautyStreamingActivity.this, properties);
                dialog.setTitle("Select a File");
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        String filePath = files[0];
                        try {
                            mAudioMixer.setFile(filePath, true);
                            Cache.setAudioFile(TestBeautyStreamingActivity.this, filePath);
                            Toast.makeText(TestBeautyStreamingActivity.this, "setup mix file " + filePath + " success. duration:" + mAudioMixer.getDuration(), Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(TestBeautyStreamingActivity.this, "setup mix file " + filePath + " failed !!!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                dialog.show();
            }
        });

        mMixToggleBtn = (Button) findViewById(R.id.mix_btn);
        mMixToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAudioMixer != null) {
                    String text;
                    if (mAudioMixer.isRunning()) {
                        boolean s = mAudioMixer.pause();
                        text = s ? "mixing pause success" : "mixing pause failed !!!";
                    } else {
                        boolean s = mAudioMixer.play();
                        text = s ? "mixing play success" : "mixing play failed !!!";
                    }
                    Toast.makeText(TestBeautyStreamingActivity.this, text, Toast.LENGTH_LONG).show();

                    updateMixBtnText();
                }
            }
        });

        Button mixStopBtn = (Button) findViewById(R.id.mix_stop_btn);
        mixStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAudioMixer != null) {
                    boolean stopSuccess = mAudioMixer.stop();
                    String text = stopSuccess ? "mixing stop success" : "mixing stop failed !!!";
                    Toast.makeText(TestBeautyStreamingActivity.this, text, Toast.LENGTH_LONG).show();
                    if (stopSuccess) {
                        updateMixBtnText();
                    }
                }
            }
        });

        Button playbackToggleBtn = (Button) findViewById(R.id.playback_btn);
        playbackToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsPlayingback) {
                    mMediaStreamingManager.stopPlayback();
                } else {
                    mMediaStreamingManager.startPlayback();
                }
                mIsPlayingback = !mIsPlayingback;
            }
        });

        updateMixBtnText();
    }

    private void updateMixBtnText() {
        if (mAudioMixer != null && mAudioMixer.isRunning()) {
            mMixToggleBtn.setText("Pause");
        } else {
            mMixToggleBtn.setText("Play");
        }
    }

    @Override
    public void onStateChanged(StreamingState streamingState, Object extra) {
        /**
         * general states are handled in the `StreamingBaseActivity`
         */
        super.onStateChanged(streamingState, extra);
        switch (streamingState) {
            case READY:
                mMaxZoom = mMediaStreamingManager.getMaxZoom();
                break;
            case SHUTDOWN:
                if (mOrientationChanged) {
                    mOrientationChanged = false;
                    startStreamingInternal();
                }
                break;
            case OPEN_CAMERA_FAIL:
                Log.e(TAG, "Open Camera Fail. id:" + extra);
                break;
            case CAMERA_SWITCHED:
                if (extra != null) {
                    Log.i(TAG, "current camera id:" + (Integer) extra);
                }
                Log.i(TAG, "camera switched");
                final int currentCamId = (Integer) extra;
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCameraSwitcherButtonText(currentCamId);
                    }
                });
                break;
            case TORCH_INFO:
                if (extra != null) {
                    final boolean isSupportedTorch = (Boolean) extra;
                    Log.i(TAG, "isSupportedTorch=" + isSupportedTorch);
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isSupportedTorch) {
                                mTorchBtn.setVisibility(View.VISIBLE);
                            } else {
                                mTorchBtn.setVisibility(View.GONE);
                            }
                        }
                    });
                }
                break;
        }
    }

    protected void setFocusAreaIndicator() {
        if (mRotateLayout == null) {
            mRotateLayout = (RotateLayout) findViewById(R.id.focus_indicator_rotate_layout);
            mMediaStreamingManager.setFocusAreaIndicator(mRotateLayout,
                    mRotateLayout.findViewById(R.id.focus_indicator));
        }
    }

    private void setTorchEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String flashlight = enabled ? getString(R.string.flash_light_off) : getString(R.string.flash_light_on);
                mTorchBtn.setText(flashlight);
            }
        });
    }

    private void updateOrientationBtnText() {
        if (mIsEncOrientationPort) {
            mEncodingOrientationSwitcherBtn.setText("Land");
        } else {
            mEncodingOrientationSwitcherBtn.setText("Port");
        }
    }

    private void updateFBButtonText() {
        if (mFaceBeautyBtn != null) {
            mFaceBeautyBtn.setText(mIsNeedFB ? "FB Off" : "FB On");
        }
    }

    private void updateMuteButtonText() {
        if (mMuteButton != null) {
            mMuteButton.setText(mIsNeedMute ? "Unmute" : "Mute");
        }
    }

    private void updateCameraSwitcherButtonText(int camId) {
        if (mCameraSwitchBtn == null) {
            return;
        }
        if (camId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraSwitchBtn.setText("Back");
        } else {
            mCameraSwitchBtn.setText("Front");
        }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.i(TAG, "onSingleTapUp X:" + e.getX() + ",Y:" + e.getY());
        if (mIsReady) {
            setFocusAreaIndicator();
            mMediaStreamingManager.doSingleTapUp((int) e.getX(), (int) e.getY());
            return true;
        }
        return false;
    }

    @Override
    public boolean onZoomValueChanged(float factor) {
        if (mIsReady && mMediaStreamingManager.isZoomSupported()) {
            mCurrentZoom = (int) (mMaxZoom * factor);
            mCurrentZoom = Math.min(mCurrentZoom, mMaxZoom);
            mCurrentZoom = Math.max(0, mCurrentZoom);
            Log.d(TAG, "zoom ongoing, scale: " + mCurrentZoom + ",factor:" + factor + ",maxZoom:" + mMaxZoom);
            mMediaStreamingManager.setZoomValue(mCurrentZoom);
        }
        return false;
    }

    @Override
    public void onSurfaceCreated() {
        Log.i(TAG, "onSurfaceCreated");
        /**
         * only used in custom beauty algorithm case
         */
        mFBO.initialize(this);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged width:" + width + ",height:" + height);
        /**
         * only used in custom beauty algorithm case
         */
        mFBO.updateSurfaceSize(width, height);
    }

    @Override
    public void onSurfaceDestroyed() {
        Log.i(TAG, "onSurfaceDestroyed");
        /**
         * only used in custom beauty algorithm case
         */
        mFBO.release();
    }

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, float[] transformMatrix) {
        /**
         * When using custom beauty algorithm, you should return a new texId from the SurfaceTexture.
         * newTexId should not equal with texId, Otherwise, there is no filter effect.
         */
        int newTexId = mFBO.drawFrame(texId, texWidth, texHeight);
        return newTexId;
    }

    @Override
    public boolean onPreviewFrame(byte[] bytes, int width, int height, int rotation, int fmt, long tsInNanoTime) {
        Log.i(TAG, "onPreviewFrame " + width + "x" + height + ",fmt:" + (fmt == PLFourCC.FOURCC_I420 ? "I420" : "NV21") + ",ts:" + tsInNanoTime + ",rotation:" + rotation);
        /**
         * When using custom beauty algorithm in sw encode mode, you should change the bytes array's values here
         * eg: byte[] beauties = readPixelsFromGPU();
         * System.arraycopy(beauties, 0, bytes, 0, bytes.length);
         */
        return true;
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
                        if (cameraPreviewFrameView != null) {
                            cameraPreviewFrameView.queueEvent(new Runnable() {
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
                        if (cameraPreviewFrameView != null) {
                            cameraPreviewFrameView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.updateComposeNode(node);
                                }
                            });
                        }
                    }

                    @Override
                    public void onFilterSelected(final File file) {
                        if (null != cameraPreviewFrameView) {
                            cameraPreviewFrameView.queueEvent(new Runnable() {
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
                        if (null != cameraPreviewFrameView) {
                            cameraPreviewFrameView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    effectRenderHelper.updateIntensity(BytedEffectConstants.IntensityType.Filter, cur);
                                }
                            });
                        }
                    }

                    @Override
                    public void setEffectOn(final boolean isOn) {
                        if (cameraPreviewFrameView != null) {
                            cameraPreviewFrameView.queueEvent(new Runnable() {
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
                        if (null != cameraPreviewFrameView) {
                            cameraPreviewFrameView.queueEvent(new Runnable() {
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
        if (cameraPreviewFrameView == null) return;
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
}
