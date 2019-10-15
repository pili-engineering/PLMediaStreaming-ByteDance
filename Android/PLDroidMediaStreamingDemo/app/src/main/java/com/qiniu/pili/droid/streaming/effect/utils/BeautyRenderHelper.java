package com.qiniu.pili.droid.streaming.effect.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.SystemClock;

import com.bef.effectsdk.OpenGLUtils;
import com.bytedance.labcv.demo.core.EffectRenderHelper;
import com.bytedance.labcv.demo.opengl.GlUtil;
import com.bytedance.labcv.demo.utils.BitmapUtils;
import com.bytedance.labcv.demo.utils.FrameRator;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;
import com.qiniu.pili.droid.streaming.SurfaceTextureCallback;
import com.qiniu.pili.droid.streaming.effect.BeautySDKFragment;

import javax.microedition.khronos.opengles.GL10;

public class BeautyRenderHelper implements SurfaceTextureCallback {
    private EffectRenderHelper mEffectRenderHelper;
    private FrameRator mFrameRator;
    private int dstTexture = GlUtil.NO_TEXTURE;
    private volatile boolean mIsPaused = false;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mOrientation;
    private boolean mIsFront = true;
    private boolean isInit = false;
    public Bitmap mbitmap;
    public BeautySDKFragment beautySDKFragment;

    public BeautyRenderHelper(Context context, BeautySDKFragment beautySDKFragment){
        mEffectRenderHelper = new EffectRenderHelper(context);
        mFrameRator = new FrameRator();
        this.beautySDKFragment = beautySDKFragment;
    }

    public void onResume(){
        LogUtils.d("BeautyRenderHelper onResume");

    }

    public void onCameraOpen(int orientation, int previewWidth, int previewHeight){
        LogUtils.d("BeautyRenderHelper onCameraOpen");
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        mOrientation = orientation;
        mIsPaused = false;
        if (mPreviewHeight % 180 == 90){
            mEffectRenderHelper.initEffectSDK(mPreviewHeight,  mPreviewWidth);
        } else {
            mEffectRenderHelper.initEffectSDK( mPreviewWidth, mPreviewHeight);
        }
        mEffectRenderHelper.recoverStatus();
        mFrameRator.start();
    }

    public void onPause(){
        LogUtils.d("BeautyRenderHelper onPause");
        mIsPaused = true;
        mFrameRator.stop();
        deleteTexture();
        mEffectRenderHelper.destroyEffectSDK();
    }

    @Override
    public void onSurfaceCreated() {
        LogUtils.d("BeautyRenderHelper onSurfaceCreated: ");
        GLES20.glEnable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        LogUtils.d("BeautyRenderHelper onSurfaceChanged");
        if (mIsPaused) {
            return;
        }
        if (isInit){
            mEffectRenderHelper.onSurfaceChanged(width, height);
        }
    }

    @Override
    public void onSurfaceDestroyed() {
        LogUtils.d("BeautyRenderHelper onSurfaceDestroyed");
    }

    @Override
    public int onDrawFrame(int texId, int width, int height, float[] floats) {
        if (mIsPaused) {
            return texId;
        }

        //清空缓冲区颜色
        //Clear buffer color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        BytedEffectConstants.Rotation rotation = OrientationSensor.getOrientation();
        long time = SystemClock.currentThreadTimeMillis();
        dstTexture = mEffectRenderHelper.processTexure(texId, BytedEffectConstants.TextureFormat.Texture_Oes, width, height, mOrientation, mIsFront,  rotation, time);
//        mEffectRenderHelper.drawFrame(mCameraProxy.getPreviewTexture(),BytedEffectConstants.TextureFormat.Texture_Oes, mCameraProxy.getPreviewWidth(),mCameraProxy.getPreviewHeight(), 360- mCameraProxy.getOrientation(), mCameraProxy.isFrontCamera(),false);
//        if (dstTexture != GlUtil.NO_TEXTURE){
//            mEffectRenderHelper.drawFrame(dstTexture, BytedEffectConstants.TextureFormat.Texure2D, width, height, 360- mOrientation, mIsFront,false);
//
//        }

//        mbitmap = BitmapUtils.bitmapFromGLTexture(dstTexture, width, height, true);
//        beautySDKFragment.showPreTemp(mbitmap);
        mFrameRator.addFrameStamp();
        if (dstTexture != GlUtil.NO_TEXTURE){
            return dstTexture;
        }else{
            return texId;
        }
    }

    public EffectRenderHelper getEffectRenderHelper() {
        return mEffectRenderHelper;
    }

    public int getFrameRate() {
        return mFrameRator.getFrameRate();
    }

    public void deleteTexture() {
        LogUtils.d("BeautyRenderHelper deleteTexture");
        if (dstTexture != OpenGLUtils.NO_TEXTURE) {
            GLES20.glDeleteTextures(1, new int[]{dstTexture}, 0);
        }
        dstTexture = OpenGLUtils.NO_TEXTURE;
    }
}
