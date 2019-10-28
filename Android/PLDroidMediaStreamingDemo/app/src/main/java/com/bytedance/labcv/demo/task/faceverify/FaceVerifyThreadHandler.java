package com.bytedance.labcv.demo.task.faceverify;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.bytedance.labcv.demo.ResourceHelper;
import com.bytedance.labcv.demo.model.FaceVerifyResult;
import com.bytedance.labcv.demo.utils.AppUtils;
import com.bytedance.labcv.demo.utils.BitmapUtils;
import com.bytedance.labcv.demo.utils.ToasUtils;
import com.bytedance.labcv.effectsdk.BefFaceFeature;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.FaceVerify;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;
import com.qiniu.pili.droid.streaming.demo.R;

import java.nio.ByteBuffer;

import static com.bytedance.labcv.effectsdk.BytedEffectConstants.BytedResultCode.BEF_RESULT_INVALID_LICENSE;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.BytedResultCode.BEF_RESULT_SUC;

public class FaceVerifyThreadHandler extends Handler {
    public static final int VERIFY = 2001;
    public static final int SET_ORIGINAL = 2002;
    public static final int SUCCESS = 2003;
    public static final int FACE_DETECT = 2005;

    private static final String THREAD_NAME = "FAVE VERIFY THREAD";

    private HandlerThread mHandlerThread;
    private Context mContext;
    private int mMaxFaceNum;
    private String mLicensePath;

    private FaceVerify mFaceVerify;
    private BefFaceFeature mOriginalFeature;
    private boolean isRunning = false;

    private FaceVerifyThreadHandler(HandlerThread thread, Context context, int maxFaceNum, String licensePath) {
        super(thread.getLooper());
        mHandlerThread = thread;
        mContext = context;
        mMaxFaceNum = maxFaceNum;
        mLicensePath = licensePath;

        initVerify(context);
    }

    private void initVerify(@NonNull final  Context context) {
        mFaceVerify = new FaceVerify();
        String faceModelPath = ResourceHelper.getFaceModelPath(context);
        String faceVerifyModel = ResourceHelper.getFaceVerifyModelPath(context);
        int ret = mFaceVerify.init(mContext, faceModelPath, faceVerifyModel, mMaxFaceNum, mLicensePath);
        if (ret == BEF_RESULT_INVALID_LICENSE) {
            ToasUtils.show(mContext.getResources().getString(R.string.tab_face_verify) + mContext.getResources().getString(R.string.invalid_license_file));
        } else if (ret != BEF_RESULT_SUC) {
            ToasUtils.show("FaceVerify Initialization failed");
        }
    }

    void resume() {
        isRunning = true;
    }

    void pause() {
        isRunning = false;
        removeCallbacksAndMessages(null);
    }

    void quit() {
        mHandlerThread.quit();
        removeCallbacksAndMessages(null);

        if (mFaceVerify != null) {
            mFaceVerify.release();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (!isRunning || null == mFaceVerify) {
            return;
        }
        Messenger messenger = msg.replyTo;
        Message resultMsg = obtainMessage();
        switch (msg.what) {
            case VERIFY:
                if (null == mOriginalFeature || mOriginalFeature.getValidFaceNum() != 1)
                    return;
                ByteBuffer resizeInputBuffer = (ByteBuffer) msg.obj;
                int width = msg.arg1;
                int height = msg.arg2;
                FaceVerifyResult faceVerifyResult = faceVerify(resizeInputBuffer, width, height);
                try {
                    resultMsg.what = SUCCESS;
                    resultMsg.obj = faceVerifyResult;
                    messenger.send(resultMsg);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
                break;
            case SET_ORIGINAL:
                Bitmap bitmap = (Bitmap) msg.obj;
                mOriginalFeature = getOriginalFaceFeature(bitmap);
                resultMsg.what = FACE_DETECT;
                resultMsg.arg1 = (mOriginalFeature == null ? 0 : mOriginalFeature.getValidFaceNum());
                try {
                    messenger.send(resultMsg);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
                break;
        }
    }

    private FaceVerifyResult faceVerify(ByteBuffer resizeInputBuffer, int width, int height) {
        BytedEffectConstants.Rotation rotation = OrientationSensor.getOrientation();
        if (AppUtils.isTv(mContext)) {
            rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0;
        }
        // 暂时修复横屏检测不到人脸的问题
        if (rotation == BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_270) {
            rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0;
        }
        long start = System.currentTimeMillis();
        BefFaceFeature currentFeature = mFaceVerify.extractFeatureSingle(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888, width, height, 4 * width, rotation);
        if (null != currentFeature && currentFeature.getValidFaceNum() > 0) {
            FaceVerifyResult result = new FaceVerifyResult();
            double dist = mFaceVerify.verify(mOriginalFeature.getFeatures()[0], currentFeature.getFeatures()[0]);
            long end = System.currentTimeMillis();
            result.setSimilarity(mFaceVerify.distToScore(dist));
            result.setValidFaceNum(currentFeature.getValidFaceNum());
            result.setCost((end - start));
            return result;
        }
        return null;
    }

    private BefFaceFeature getOriginalFaceFeature(Bitmap mOriginalBitmap) {
        if (mOriginalBitmap != null) {
            ByteBuffer buffer = BitmapUtils.bitmap2ByteBuffer(mOriginalBitmap);
            return mFaceVerify.extractFeature(buffer, BytedEffectConstants.PixlFormat.RGBA8888, mOriginalBitmap.getWidth(), mOriginalBitmap.getHeight(), 4 * mOriginalBitmap.getWidth(), BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0);
        }
        return null;

    }

    static FaceVerifyThreadHandler createFaceVerifyHandlerThread(final Context context, final int maxFaceNum, final String licensePath) {
        HandlerThread thread = new HandlerThread(THREAD_NAME);
        thread.start();
        return new FaceVerifyThreadHandler(thread, context, maxFaceNum, licensePath);
    }
}
