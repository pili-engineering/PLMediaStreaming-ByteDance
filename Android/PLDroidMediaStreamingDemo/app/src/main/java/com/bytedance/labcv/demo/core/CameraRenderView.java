package com.bytedance.labcv.demo.core;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.bytedance.labcv.demo.camera.CameraListener;
import com.bytedance.labcv.demo.camera.CameraProxy;
import com.bytedance.labcv.demo.opengl.GlUtil;
import com.bytedance.labcv.demo.utils.AppUtils;
import com.bytedance.labcv.demo.utils.FrameRator;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRenderView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private volatile boolean mCameraChanging = false;

    private volatile boolean mIsPaused = false;
    private EffectRenderHelper mEffectRenderHelper;

    private FrameRator mFrameRator;

    private Context mContext;

    //cameraId（前后）
    private int mCameraID  = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;




    private int dstTexture = GlUtil.NO_TEXTURE;

    private CameraProxy mCameraProxy;

    public CameraRenderView(Context context) {
        super(context);
        init(context);
    }

    public CameraRenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtils.d("onSurfaceCreated: ");
        GLES20.glEnable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (mIsPaused) {
            return;
        }
        mEffectRenderHelper.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mCameraChanging || mIsPaused) {
            return;
        }
        //清空缓冲区颜色
        //Clear buffer color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mCameraProxy.updateTexture();

        BytedEffectConstants.Rotation rotation = OrientationSensor.getOrientation();
        // tv sensor get an 270 ……
        if (AppUtils.isTv(mContext)){
            rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0;
        }
        dstTexture = mEffectRenderHelper.processTexure(mCameraProxy.getPreviewTexture(), BytedEffectConstants.TextureFormat.Texture_Oes, mCameraProxy.getPreviewWidth(),mCameraProxy.getPreviewHeight(), mCameraProxy.getOrientation(), mCameraProxy.isFrontCamera(),  rotation, mCameraProxy.getTimeStamp());
//        mEffectRenderHelper.drawFrame(mCameraProxy.getPreviewTexture(),BytedEffectConstants.TextureFormat.Texture_Oes, mCameraProxy.getPreviewWidth(),mCameraProxy.getPreviewHeight(), 360- mCameraProxy.getOrientation(), mCameraProxy.isFrontCamera(),false);
        if (dstTexture != GlUtil.NO_TEXTURE){
            mEffectRenderHelper.drawFrame(dstTexture, BytedEffectConstants.TextureFormat.Texure2D,mCameraProxy.getPreviewWidth(),mCameraProxy.getPreviewHeight(), 360- mCameraProxy.getOrientation(), mCameraProxy.isFrontCamera(),false);

        }

        mFrameRator.addFrameStamp();
    }

    @Override
    public void onResume() {
        LogUtils.d("onResume");
        mIsPaused = false;
        if (!mCameraProxy.isCameraValid()) {
            mCameraProxy.openCamera(mCameraID, new CameraListener() {
                @Override
                public void onOpenSuccess() {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            LogUtils.d("onOpenSuccess");
                            onCameraOpen();

                        }
                    });
                }

                @Override
                public void onOpenFail() {

                }
            });
        }

        super.onResume();
    }


    @Override
    public void onPause() {
        LogUtils.d("onPause");
        mIsPaused = true;
        mCameraProxy.releaseCamera();
        mFrameRator.stop();
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraProxy.deleteTexture();
                mEffectRenderHelper.destroyEffectSDK();
            }
        });
        super.onPause();
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        mCameraProxy = new CameraProxy(context);
        mEffectRenderHelper = new EffectRenderHelper(context);
        mFrameRator = new FrameRator();
        mContext = context;
    }

    /**
     * 相机打开成功时回调，初始化特效SDK
     * Initialize camera information (texture, etc.)
     */
    private void onCameraOpen() {
        LogUtils.d("CameraSurfaceView onCameraOpen");
        mCameraProxy.startPreview(CameraRenderView.this);
        if (mCameraProxy.getOrientation()%180 == 90){
            mEffectRenderHelper.initEffectSDK(mCameraProxy.getPreviewHeight(),  mCameraProxy.getPreviewWidth());
        } else {
            mEffectRenderHelper.initEffectSDK( mCameraProxy.getPreviewWidth(), mCameraProxy.getPreviewHeight());
        }
        mEffectRenderHelper.recoverStatus();
        mFrameRator.start();

    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (!mCameraChanging) {
            requestRender();
        }
    }


    /**
     *  切换前后置相机
     */
    public void switchCamera() {
        LogUtils.d("switchCamera");

        if (Camera.getNumberOfCameras() == 1
                || mCameraChanging) {
            return;
        }
        mCameraID = 1 - mCameraID;
        mCameraChanging = true;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraProxy.changeCamera(mCameraID, new CameraListener() {
                    @Override
                    public void onOpenSuccess() {
                        LogUtils.d("onOpenSuccess");
                        deleteCameraPreviewTexture();
                        onCameraOpen();
                        mCameraChanging = false;
                        requestRender();
                    }

                    @Override
                    public void onOpenFail() {
                        LogUtils.e("camera openFail!!");


                    }
                });

            }
        });


    }

    /**
     * 删除camera的纹理
     */
    private void deleteCameraPreviewTexture() {
        mCameraProxy.deleteTexture();

    }

    public EffectRenderHelper getEffectRenderHelper() {
        return mEffectRenderHelper;
    }

    public int getFrameRate() {
        return mFrameRator.getFrameRate();
    }



}
