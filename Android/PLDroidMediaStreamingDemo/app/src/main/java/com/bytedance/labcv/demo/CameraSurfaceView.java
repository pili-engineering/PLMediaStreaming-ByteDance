// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.bytedance.labcv.demo;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;

import com.bytedance.labcv.demo.opengl.ShaderHelper;
import com.bytedance.labcv.demo.utils.AppUtils;
import com.bytedance.labcv.demo.utils.FrameRator;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 相机画布
 */
public class CameraSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, Camera.PreviewCallback {

    //相机获取的图片尺寸
    private int mImageWidth;
    private int mImageHeight;

    private volatile boolean mCameraChanging = false;

    private volatile boolean mIsPaused = false;
    private EffectRenderHelper mEffectRenderHelper;

    private FrameRator mFrameRator;

    private int mSurfaceTextureID = ShaderHelper.NO_TEXTURE;
    private SurfaceTexture mSurfaceTexture;
    private Context mContext;

    private int dstTexture = ShaderHelper.NO_TEXTURE;

    public CameraSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtils.i("onSurfaceCreated: ");
        GLES20.glEnable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);
        mEffectRenderHelper.initSDKModules();
        mEffectRenderHelper.recoverStatus(mContext);
        mFrameRator.start();

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (mIsPaused) {
            return;
        }
        mEffectRenderHelper.initViewPort(width, height);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mCameraChanging || mIsPaused) {
            return;
        }
        //清空缓冲区颜色
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mSurfaceTexture.updateTexImage();

        BytedEffectConstants.Rotation rotation = OrientationSensor.getOrientation();
        // tv sensor get an 270 ……
        if (AppUtils.isTv(mContext)){
            rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0;
        }
        dstTexture = mEffectRenderHelper.processTexture(mSurfaceTextureID, rotation, getSurfaceTimeStamp());

        if (dstTexture != ShaderHelper.NO_TEXTURE) {
            mEffectRenderHelper.drawFrame(dstTexture);
        }
        mFrameRator.addFrameStamp();
    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        queueEvent(new Runnable() {
            @Override
            public void run() {

                CameraDevice.get().getCamera().addCallbackBuffer(data);
                if (AppUtils.isTestEffectWithBuffer()){

                    mEffectRenderHelper.processBufferEffect(data, CameraDevice.get().getPreviewFormat(), CameraDevice.get().getOrientation(), CameraDevice.get().isFlipHorizontal() );

                } else {
                    mEffectRenderHelper.processBufferAlgoritm(data, CameraDevice.get().getPreviewFormat(), CameraDevice.get().getOrientation(), CameraDevice.get().isFlipHorizontal() );

                }

            }
        });

    }

    @Override
    public void onResume() {
        LogUtils.i("onResume");
        mIsPaused = false;
        CameraDevice.get().openCamera();
        setUpCamera(mContext);
        super.onResume();
    }

    /**
     * becareful this func will called twice while switch camera
     */
    @Override
    public void onPause() {
        LogUtils.i("onPause");
        mIsPaused = true;
        CameraDevice.get().closeCamera();
        mFrameRator.stop();
        queueEvent(new Runnable() {
            @Override
            public void run() {
                deleteCameraPreviewTexture();
                mEffectRenderHelper.destroySDKModules();
            }
        });
        super.onPause();
    }

    /**
     * 初始化
     */
    private void init(Context context) {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        mEffectRenderHelper = new EffectRenderHelper(context);
        mFrameRator = new FrameRator();
        mContext = context;
    }

    /**
     * 初始化camera信息(纹理等)
     */
    private void setUpCamera(Context context) {
        LogUtils.v("CameraSurfaceView setUpCamera");
        //获取纹理id,绑定纹理信息
        mImageHeight = CameraDevice.get().getPreviewHeight();
        mImageWidth = CameraDevice.get().getPreviewWidth();
        if (CameraDevice.get().getOrientation()%180 == 90){
            mEffectRenderHelper.setImageSize(mImageHeight, mImageWidth);
        } else {
            mEffectRenderHelper.setImageSize(mImageWidth, mImageHeight);

        }

        boolean flipHoriontal = CameraDevice.get().isFlipHorizontal();
        // 特殊设备适配 前置摄像头不需要镜像
        if (AppUtils.getSupportMode() == AppUtils.SupportMode.HR) {
            flipHoriontal = false;
        }
        mEffectRenderHelper.adjustTextureBuffer(CameraDevice.get().getOrientation(),flipHoriontal, false);
        prepareSurfaceTexture(this);
        if (AppUtils.isUseYuv()) {
            // NV21 is recommended
            CameraDevice.get().setPreviewFormat(ImageFormat.NV21);
            CameraDevice.get().startPreview(mSurfaceTexture, this);

        } else {
            CameraDevice.get().startPreview(mSurfaceTexture);
        }
    }


    /**
     * 初始化SurfaceTexture
     *
     * @param listener
     */
    public void prepareSurfaceTexture(final SurfaceTexture.OnFrameAvailableListener listener) {
        if (mSurfaceTextureID == ShaderHelper.NO_TEXTURE) {
            mSurfaceTextureID = ShaderHelper.getExternalOESTextureID();
            mSurfaceTexture = new SurfaceTexture(mSurfaceTextureID);
            mSurfaceTexture.setOnFrameAvailableListener(listener);
        }
    }

    /**
     * 删除camera的纹理 必须在Gl Render线程中调用
     */
    public void deleteCameraPreviewTexture() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mSurfaceTextureID != ShaderHelper.NO_TEXTURE) {
            GLES20.glDeleteTextures(1, new int[]{mSurfaceTextureID}, 0);
        }
        mSurfaceTextureID = ShaderHelper.NO_TEXTURE;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (!mCameraChanging) {
            requestRender();
        }

    }

    public EffectRenderHelper getEffectRenderHelper() {
        return mEffectRenderHelper;
    }

    /**
     * 获取帧率
     *
     * @return
     */
    public int getFrameRate() {
        return mFrameRator.getFrameRate();
    }

    public double getSurfaceTimeStamp() {
        if (mSurfaceTexture == null) {
            return -1;
        }
        long cur_time_nano = System.nanoTime();
        long delta_nano_time = Math.abs(cur_time_nano - mSurfaceTexture.getTimestamp());
        long delta_elapsed_nano_time = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? Math.abs(SystemClock.elapsedRealtimeNanos() - mSurfaceTexture.getTimestamp()) : Long.MAX_VALUE;
        long delta_uptime_nano = Math.abs(SystemClock.uptimeMillis() * 1000000 - mSurfaceTexture.getTimestamp());
        double lastTimeStamp = cur_time_nano - Math.min(Math.min(delta_nano_time, delta_elapsed_nano_time), delta_uptime_nano);
        return lastTimeStamp / 1e9;
    }



}
