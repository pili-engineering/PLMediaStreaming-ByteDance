// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.bytedance.labcv.demo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.opengl.GLES20;
import android.os.Message;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bytedance.labcv.demo.model.CaptureResult;
import com.bytedance.labcv.demo.model.ComposerNode;
import com.bytedance.labcv.demo.opengl.ShaderHelper;
import com.bytedance.labcv.demo.utils.AppUtils;
import com.bytedance.labcv.demo.utils.InputSizeManager;
import com.bytedance.labcv.effectsdk.BefDistanceInfo;
import com.bytedance.labcv.effectsdk.BefFaceInfo;
import com.bytedance.labcv.effectsdk.BefHandInfo;
import com.bytedance.labcv.effectsdk.BefPetFaceInfo;
import com.bytedance.labcv.effectsdk.BefSkeletonInfo;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;
import com.bytedance.labcv.effectsdk.FaceDetect;
import com.bytedance.labcv.effectsdk.FaceVerify;
import com.bytedance.labcv.effectsdk.HairParser;
import com.bytedance.labcv.effectsdk.HandDetect;
import com.bytedance.labcv.effectsdk.HumanDistance;
import com.bytedance.labcv.effectsdk.PetFaceDetect;
import com.bytedance.labcv.effectsdk.PortraitMatting;
import com.bytedance.labcv.effectsdk.RenderManager;
import com.bytedance.labcv.effectsdk.SkeletonDetect;
import com.bytedance.labcv.effectsdk.YUVUtils;
import com.bytedance.labcv.effectsdk.library.LogUtils;
import com.bytedance.labcv.effectsdk.library.OrientationSensor;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.bytedance.labcv.demo.ResourceHelper.BoxRegParamFile;
import static com.bytedance.labcv.demo.ResourceHelper.DetectParamFile;
import static com.bytedance.labcv.demo.ResourceHelper.GestureParamFile;
import static com.bytedance.labcv.demo.ResourceHelper.KeyPointParamFile;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.BEF_DETECT_SMALL_MODEL;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.BytedResultCode.BEF_RESULT_FAIL;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.BytedResultCode.BEF_RESULT_SUC;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAction.BEF_DETECT_FULL;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAttribute.BEF_FACE_ATTRIBUTE_AGE;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAttribute.BEF_FACE_ATTRIBUTE_ATTRACTIVE;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAttribute.BEF_FACE_ATTRIBUTE_EXPRESSION;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAttribute.BEF_FACE_ATTRIBUTE_GENDER;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAttribute.BEF_FACE_ATTRIBUTE_HAPPINESS;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceAttribute.BEF_FACE_ATTRIBUTE_RACIAL;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.FaceExtraModel.BEF_MOBILE_FACE_280_DETECT;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.PetFaceDetectConfig.BEF_PET_FACE_DETECT_CAT;
import static com.bytedance.labcv.effectsdk.BytedEffectConstants.PetFaceDetectConfig.BEF_PET_FACE_DETECT_DOG;


public class EffectRenderHelper {
    /**
     * license有效时间2019-05-23到2019-06-30
     * license只是为了追踪使用情况，可以随时申请无任何限制license
     */

    private static final String TAG = "EffectRenderHelper";

    private static final int HAND_DETECT_DELAY_FRAME_COUNT = 4;


    private RenderManager mRenderManager;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mImageWidth;
    private int mImageHeight;

    private OpenGLRender mGLRender;

    private FaceDetect mFaceDetector;
    private HandDetect mHandDetector;
    private PortraitMatting mPortaitMatting;
    private HairParser mHairParser;
    private HumanDistance mHumanDistance;
    private PetFaceDetect mPetFaceDetector;

    private FaceVerify mFaceVerify;
    public volatile boolean isDetect106Face;
    public volatile boolean isDetectFaceExtra;
    public volatile boolean isDetectFaceAttr;

    public volatile boolean isMattingPortrait;
    public volatile boolean isParsingHair;
    public volatile boolean isDetectHand;

    public void setQrDecoding(boolean qrDecoding) {
        isQrDecoding = qrDecoding;
    }

    public volatile boolean isQrDecoding;

    public void setFaceVerify(boolean faceVerify) {
        isFaceVerify = faceVerify;
    }

    public volatile boolean isFaceVerify;

    // 设置了贴纸后会与 Composer 冲突，所以再使用 Composer 功能的时候
    // 需要重新设置 Composer 路径，以 isShouldResetComposer 标志
    private boolean isShouldResetComposer = false;
    private String mFilterResource;
    private String[] mComposeNodes = new String[0];
    private String mStickerResource;
    private Set<ComposerNode> mSavedComposerNodes = new HashSet<>();
    private ArrayMap<BytedEffectConstants.IntensityType, Float> storedIntensities = new ArrayMap<>();

    private SkeletonDetect mSkeletonDetector;
    public volatile boolean isDetectSkeleton;
    public volatile boolean isDetectDistance;
    public volatile boolean isDetectPetFace;

    public volatile boolean isEffectOn = true;

    private Context mContext;

    private MainActivity mainView;

    public void setMainActivity(MainActivity mainActivity) {
        this.mainView = mainActivity;
        mGLRender.setMainActivity(mainActivity);

    }




    static abstract class ResultCallback<T> {
        private Class<T> clazz;

        abstract void doResult(T t, int framecount);

        // 使用反射得到T的真实类型
        public Class getRealGenericType() {
            ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
            Type[] types = pt.getActualTypeArguments();
            if (types.length > 0) {
                return (Class<T>) pt.getActualTypeArguments()[0];
            }
            return null;
        }
    }

    private List<ResultCallback> callbacks = new ArrayList<>();

    public EffectRenderHelper(Context context) {
        mContext = context;
        mRenderManager = new RenderManager();
        mGLRender = new OpenGLRender(context);
        mFaceDetector = new FaceDetect();
        mHandDetector = new HandDetect();
        mSkeletonDetector = new SkeletonDetect();
        mPortaitMatting = new PortraitMatting();
        mHairParser = new HairParser();
        mFaceVerify = new FaceVerify();
        mHumanDistance = new HumanDistance();
        mPetFaceDetector = new PetFaceDetect();

    }


    public int initSkeleton(Context context) {
        String path = ResourceHelper.getSkeletonModelPath(context);
        return mSkeletonDetector.init(context, path, ResourceHelper.getLicensePath(context));
    }

    public int initFace(Context context) {
        String path = ResourceHelper.getFaceModelPath(context);
        return mFaceDetector.init(context, path, BEF_DETECT_SMALL_MODEL | BEF_DETECT_FULL, ResourceHelper.getLicensePath(context));

    }

    public int initFaceExtra(Context context) {
        String path = ResourceHelper.getFaceExtaModelPath(context);
        return mFaceDetector.initExtra(context, path, BEF_MOBILE_FACE_280_DETECT);

    }

    public int initFaceAttri(Context context) {
        String path = ResourceHelper.getFaceAttriModelPath(context);
        return mFaceDetector.initAttri(context, path, ResourceHelper.getLicensePath(context));

    }

    /**
     * 初始化特效入口，需要提前设置特效输入的图像尺寸，底层初始化人脸检测算法时需要
     * fix 开启美颜后切换相机导致的瘦脸 大眼 特效不生效的问题
     * @param context
     * @return
     */
    public int initEffect(Context context) {
        if (mImageHeight == 0 || mImageWidth == 0){
            LogUtils.e("mImageHeight & mImageWidth shoule be initialized before call initEffect()");
            return BEF_RESULT_FAIL;
        }
        int ret = mRenderManager.init(context, ResourceHelper.getModelDir(context), ResourceHelper.getLicensePath(context),mImageWidth, mImageHeight);
        if (ret != BEF_RESULT_SUC){
            LogUtils.e("mRenderManager.init failed!! ret ="+ret);
            return ret;
        }
        ret = mRenderManager.setComposer(ResourceHelper.getComposeMakeupComposerPath(context));
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("mRenderManager.setComposer failed!! ret ="+ret);

        }
        return ret;

    }

    public int initTest(Context context, String licensePath) {
        if (mImageHeight == 0 || mImageWidth == 0){
            LogUtils.e("mImageHeight & mImageWidth shoule be initialized before call initEffect()");
            return BEF_RESULT_FAIL;
        }
        return mRenderManager.initTest(context, ResourceHelper.getModelDir(context), licensePath, mImageWidth, mImageHeight);
    }


    /**
     * 初始化手势检测器
     *
     * @param context
     * @return
     */
    public int initHandDetector(Context context) {
        int flag = mHandDetector.createHandle(context, ResourceHelper.getLicensePath(context));
        if (flag != BEF_RESULT_SUC) {
            LogUtils.e("mHandDetector createHandle fail！ret =" + flag);
            return flag;
        }
        int ret = mHandDetector.setModel(BytedEffectConstants.HandModelType.BEF_HAND_MODEL_DETECT, ResourceHelper.getHandModelPath(context,DetectParamFile));
        if (BEF_RESULT_SUC != ret) {
            LogUtils.e("mHandDetector set model fail, path =" + ResourceHelper.getHandModelPath(context,DetectParamFile));
            return ret;
        }

        ret = mHandDetector.setModel(BytedEffectConstants.HandModelType.BEF_HAND_MODEL_BOX_REG, ResourceHelper.getHandModelPath(context,BoxRegParamFile));
        if (BEF_RESULT_SUC != ret) {
            LogUtils.e("mHandDetector set model fail, path =" + ResourceHelper.getHandModelPath(context,BoxRegParamFile));
            return ret;

        }

        ret = mHandDetector.setModel(BytedEffectConstants.HandModelType.BEF_HAND_MODEL_GESTURE_CLS, ResourceHelper.getHandModelPath(context,GestureParamFile));

        if (BEF_RESULT_SUC != ret) {
            LogUtils.e("mHandDetector set model fail, path =" + ResourceHelper.getHandModelPath(context,GestureParamFile));
            return ret;

        }

        ret = mHandDetector.setModel(BytedEffectConstants.HandModelType.BEF_HAND_MODEL_KEY_POINT, ResourceHelper.getHandModelPath(context,KeyPointParamFile));
        LogUtils.d("mHandDetector.setModel ret =" + ret);

        if (BEF_RESULT_SUC != ret) {
            LogUtils.e("mHandDetector set model fail, path =" + ResourceHelper.getHandModelPath(context,KeyPointParamFile));
            return ret;

        }

        ret = mHandDetector.setParam(BytedEffectConstants.HandParamType.BEF_HAND_MAX_HAND_NUM, 1);

        if (BEF_RESULT_SUC != ret) {
            Log.e(TAG, "mHandDetector setParam fail！ret =" + ret);
            return ret;
        }
        ret = mHandDetector.setParam(BytedEffectConstants.HandParamType.BEF_HNAD_ENLARGE_FACTOR_REG, 2.0f);
        if (BEF_RESULT_SUC != ret) {
            Log.e(TAG, "mHandDetector setParam fail！ret =" + ret);
            return ret;
        }
        ret = mHandDetector.setParam(BytedEffectConstants.HandParamType.BEF_HAND_NARUTO_GESTUER, 1);
        if (BEF_RESULT_SUC != ret) {
            Log.e(TAG, "mHandDetector setParam fail！ret =" + ret);
            return ret;
        }

        return ret;

    }

    /**
     * 初始化人体分割
     *
     * @param context
     * @param modelType 使用模型类型，
     *                  {@link BytedEffectConstants}
     *                  BEF_PORTAITMATTING_LARGE_MODEL(0), 大模型，耗时比小模型稍长；
     *                  BEF_PORTAITMATTING_SMALL_MODEL(1); 小模型,较快；
     * @return
     */
    public int initPortraitMatting(Context context, BytedEffectConstants.PortraitMatting modelType) {
        String path = ResourceHelper.getPortraitmattingModelPath(context);
        return mPortaitMatting.init(context, path, modelType, ResourceHelper.getLicensePath(context));
    }


    public int initPetFaceDetect(Context context) {
        String path = ResourceHelper.getPetFaceModelPath(context);
        return mPetFaceDetector.init(context, path, BEF_PET_FACE_DETECT_CAT | BEF_PET_FACE_DETECT_DOG, ResourceHelper.getLicensePath(context));
    }

    /**
     * 初始化头发分割
     *
     * @param context
     * @param width
     * @param height
     * @return
     */
    public int initHairParse(Context context, int width, int height) {
        int ret = mHairParser.init(context, ResourceHelper.getHairParsingModelPath(context), ResourceHelper.getLicensePath(context));
        if (BEF_RESULT_SUC != ret) {
            LogUtils.e("mHairParser init fail, ret=" + ret);
            return ret;
        }
        return mHairParser.setParam(width, height, true, true);


    }

    /**
     * 初始化距离估计
     *
     * @param context
     * @return
     */
    public int initHumanDistance(Context context, float cameraFov) {
        int ret = mHumanDistance.init(context, ResourceHelper.getFaceModelPath(context), ResourceHelper.getFaceAttriModelPath(context),cameraFov, ResourceHelper.getLicensePath(context));
        if (BEF_RESULT_SUC != ret) {
            LogUtils.e("mHumanDistance init fail, ret=" + ret);
        }
        return ret;
    }


    public void addResultCallback(ResultCallback callback) {
        if (null == callbacks) {
            callbacks = new ArrayList<>();
        }
        callbacks.add(callback);

    }

    private int framecounts = 0;

    Bitmap mCameraBitmap = null;


    Bitmap getBitmapFromPixels(ByteBuffer byteBuffer, int width, int height) {

        mCameraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        byteBuffer.position(0);
        mCameraBitmap.copyPixelsFromBuffer(byteBuffer);
        byteBuffer.position(0);
        return mCameraBitmap;
    }

    Bitmap getBitmapFromYuv(ByteBuffer data, int width, int height) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(data.array(), ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        byte[] imageBytes = out.toByteArray();
        mCameraBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        return mCameraBitmap;
    }


    /**
     * 处理yuv输入
     *
     * @param data
     * @param cameraRotation
     * @param isFront
     */
    public void processBufferEffect(byte[] data, int imageFormat, int cameraRotation, boolean isFront) {
        framecounts++;
        if (framecounts == 1000000)
            framecounts = 0;

        int deviceRotation = OrientationSensor.getSensorOrientation();
        if (AppUtils.isTv(mContext)) {
            deviceRotation = 0;
        }
        int totalRotation = (cameraRotation - deviceRotation + 360)%360;
        BytedEffectConstants.Rotation rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0;
        switch (totalRotation) {
            case 90:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_90;
                break;
            case 180:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_180;
                break;
            case 270:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_270;
                break;
        }

        BytedEffectConstants.PixlFormat pixlFormat = BytedEffectConstants.PixlFormat.BEF_AI_PIX_FMT_NV21;
        switch (imageFormat) {
            case ImageFormat.NV21:
                pixlFormat = BytedEffectConstants.PixlFormat.BEF_AI_PIX_FMT_NV21;
                break;

            case ImageFormat.YV12:
                LogUtils.e(" YV12 format not supported");
                break;
            default:
                    break;
        }

        ByteBuffer yuvInputBuffer = ByteBuffer.allocateDirect((mImageWidth * mImageHeight * 3 /2));
        yuvInputBuffer.put(data);
        ByteBuffer rgbaOutputBuffer = ByteBuffer.allocateDirect((mImageWidth * mImageHeight * 3 / 2));
        rgbaOutputBuffer.position(0);


        mRenderManager.processBuffer(yuvInputBuffer,rotation, pixlFormat.getValue(),mImageHeight,mImageWidth,mImageHeight * 4,rgbaOutputBuffer.array(), BytedEffectConstants.PixlFormat.BEF_AI_PIX_FMT_NV21.getValue());
        if (AppUtils.isDebug() && mainView.mImageView.getVisibility() == View.VISIBLE) {
            if (rgbaOutputBuffer != null) {
                getBitmapFromYuv(rgbaOutputBuffer, mImageHeight, mImageWidth);
                mainView.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainView.mImageView.setImageBitmap(mCameraBitmap);
                    }
                });
            }


        }

    }


    /**
     * 处理yuv输入
     *
     * @param data
     * @param cameraRotation
     * @param isFront
     */
    public void processBufferAlgoritm(byte[] data, int imageFormat, int cameraRotation, boolean isFront) {
        framecounts++;
        if (framecounts == 1000000)
            framecounts = 0;

        BytedEffectConstants.Rotation deviceRotation = OrientationSensor.getOrientation();
        // tv sensor get an 270 ……
        if (AppUtils.isTv(mContext)) {
            deviceRotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0;
        }
        BytedEffectConstants.Rotation rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0;
        switch (cameraRotation) {
            case 90:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_90;
                break;
            case 180:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_180;
                break;
            case 270:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_270;
                break;
        }

        BytedEffectConstants.PixlFormat pixlFormat = BytedEffectConstants.PixlFormat.BEF_AI_PIX_FMT_NV21;
        switch (imageFormat) {
            case ImageFormat.NV21:
                pixlFormat = BytedEffectConstants.PixlFormat.BEF_AI_PIX_FMT_NV21;
                break;

            case ImageFormat.YV12:
                LogUtils.e(" YV12 format not supported");
                break;
        }
        //根据开启的算法组合动态设置resize ratio
        mGLRender.setResizeRatio(InputSizeManager.getPreferSampleSize(mContext,this, mImageWidth, mImageHeight));
        // 因为可能动态改变retio 所以每次重新分配内存 上线应用建议使用预分配一次的方式
        int mInputWidth = (int) (mImageWidth * mGLRender.getResizeRatio());
        int mInputHeight = (int) (mImageHeight * mGLRender.getResizeRatio());
        // YUV转换要求输入的YUV的长宽必须是偶数
        if (mInputWidth % 2 != 0 || mInputHeight % 2 != 0) {
            mInputWidth = (mInputWidth >> 1) << 1;
            mInputHeight = (mInputHeight >> 1) << 1;
        }
        ByteBuffer resizeInputBuffer = ByteBuffer.allocateDirect((mInputWidth * mInputHeight * 4));
        resizeInputBuffer.position(0);
        if (cameraRotation % 180 == 90) {
            YUVUtils.YUV2RGBA(data, resizeInputBuffer.array(), pixlFormat.getValue(), mImageHeight, mImageWidth, mInputHeight, mInputWidth, rotation.id, isFront);
        } else {
            YUVUtils.YUV2RGBA(data, resizeInputBuffer.array(), pixlFormat.getValue(), mImageWidth, mImageHeight, mInputWidth, mInputHeight, rotation.id, isFront);

        }
        if (isDetect106Face) {
            BefFaceInfo faceInfo = mFaceDetector.detectFace(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                    mInputWidth, mInputHeight, mInputWidth * 4,
                    deviceRotation);
            LogUtils.d("faceInfo =" + faceInfo.toString());

            mGLRender.setFaceInfo(faceInfo);

            dispatchResult(BefFaceInfo.class, faceInfo, framecounts);

        }
        if (isDetectSkeleton) {
            BefSkeletonInfo skeletonInfo = mSkeletonDetector.detectSkeleton(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                    mInputWidth, mInputHeight, mInputWidth * 4,
                    deviceRotation);
            mGLRender.setSkeletonInfo(skeletonInfo);

        }

        if (isDetectHand) {
            BefHandInfo handInfo = mHandDetector.detectHand(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                    mInputWidth, mInputHeight, mInputWidth * 4,
                    deviceRotation, BytedEffectConstants.HandModelType.BEF_HAND_MODEL_DETECT.getValue() |
                            BytedEffectConstants.HandModelType.BEF_HAND_MODEL_BOX_REG.getValue() |
                            BytedEffectConstants.HandModelType.BEF_HAND_MODEL_GESTURE_CLS.getValue() |
                            BytedEffectConstants.HandModelType.BEF_HAND_MODEL_KEY_POINT.getValue(), HAND_DETECT_DELAY_FRAME_COUNT);
            mGLRender.setHandInfo(handInfo);
            dispatchResult(BefHandInfo.class, handInfo, framecounts);
        }

        if (isMattingPortrait) {
            PortraitMatting.MattingMask mattingMask = mPortaitMatting.detectMatting(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                    mInputWidth, mInputHeight, mInputWidth * 4,
                    deviceRotation, false);
            if (mattingMask == null) {
                LogUtils.d("mattingMask == null");
            }
            mGLRender.setMattingMask(mattingMask);
        }

        if (isParsingHair) {
            HairParser.HairMask hairMask = mHairParser.parseHair(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                    mInputWidth, mInputHeight, mInputWidth * 4,
                    deviceRotation, false);
            mGLRender.setmHairMask(hairMask);

        }

        if (isDetectPetFace) {
            BefPetFaceInfo faceInfo = mPetFaceDetector.detectFace(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                    mInputWidth, mInputHeight, mInputWidth * 4,
                    deviceRotation);
            LogUtils.d("petFaceInfo =" + faceInfo.toString());

            mGLRender.setPetFaceInfo(faceInfo);

            dispatchResult(BefPetFaceInfo.class, faceInfo, framecounts);

        }
        if (AppUtils.isDebug() && mainView.mImageView.getVisibility() == View.VISIBLE) {
            if (resizeInputBuffer != null) {
                getBitmapFromPixels(resizeInputBuffer, mInputWidth, mInputHeight);
            } else {
                resizeInputBuffer = ByteBuffer.wrap(data);
                getBitmapFromPixels(resizeInputBuffer, mInputWidth, mInputHeight);
            }

            mainView.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainView.mImageView.setImageBitmap(mCameraBitmap);
                }
            });
        }

    }

    public CaptureResult capture(){
        if (null == mGLRender){
            return null;
        }
        return new CaptureResult(mGLRender.captureRenderResult(),mImageWidth, mImageHeight);
    }

    public int processTexture(int textureID, BytedEffectConstants.Rotation rotation, double timestamp) {
        framecounts++;
        if (framecounts == 1000000)
            framecounts = 0;
        int srcTexture = mGLRender.preProcess(textureID);
        int dstTexture = mGLRender.getOutputTexture();
        if (dstTexture == ShaderHelper.NO_TEXTURE) {
            return srcTexture;
        }

        if (!isEffectOn || !mRenderManager.processTexture(srcTexture, dstTexture, mImageWidth, mImageHeight, rotation, (long) timestamp)) {
            dstTexture = srcTexture;
        }

        if (AppUtils.isUseYuv()) {
            if (isDetect106Face && mGLRender.getFaceInfo() != null) {
                mGLRender.drawFaces(mGLRender.getFaceInfo(), dstTexture);
            }

            if (isDetectSkeleton && mGLRender.getSkeletonInfo() != null) {
                mGLRender.drawSkeleton(mGLRender.getSkeletonInfo(), dstTexture);
            }
            if (isDetectHand && mGLRender.getHandInfo() != null) {
                mGLRender.drawHands(mGLRender.getHandInfo(), dstTexture);
            }
            if (isParsingHair && mGLRender.getmHairMask() != null) {
                mGLRender.drawHairMask(mGLRender.getmHairMask(), dstTexture);
            }
            if (isMattingPortrait && mGLRender.getMattingMask() != null) {
                mGLRender.drawMattingMask(mGLRender.getMattingMask(), dstTexture);
            }

            if (isDetectPetFace && mGLRender.getPetFaceInfo() != null) {
                mGLRender.drawPetFaces(mGLRender.getPetFaceInfo(), dstTexture);
            }

        } else {
            // 根据开启的算法组合动态设置resize ratio
            mGLRender.setResizeRatio(InputSizeManager.getPreferSampleSize(mContext,this, mImageWidth, mImageHeight));
            float ratio = mGLRender.getResizeRatio();
            int width = (int) (mImageWidth * ratio);
            int height = (int) (mImageHeight * ratio);

            ByteBuffer resizeInputBuffer = null;
            if (isDetect106Face) {
                resizeInputBuffer = mGLRender.getResizeOutputTextureBuffer(dstTexture);
                resizeInputBuffer.position(0);

                BefFaceInfo faceInfo = mFaceDetector.detectFace(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                        width, height, width * 4,
                        rotation);
                if (faceInfo != null) {
                    mGLRender.drawFaces(faceInfo, dstTexture);
                }

                dispatchResult(BefFaceInfo.class, faceInfo, framecounts);

            }
            if (isDetectSkeleton) {
                if (resizeInputBuffer == null)
                    resizeInputBuffer = mGLRender.getResizeOutputTextureBuffer(dstTexture);
                resizeInputBuffer.position(0);
                BefSkeletonInfo skeletonInfo = mSkeletonDetector.detectSkeleton(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                        width, height, width * 4,
                        rotation);
                if (skeletonInfo != null) {
                    mGLRender.drawSkeleton(skeletonInfo, dstTexture);
                }
            }

            if (isDetectHand) {
                if (resizeInputBuffer == null)
                    resizeInputBuffer = mGLRender.getResizeOutputTextureBuffer(dstTexture);
                resizeInputBuffer.position(0);
                BefHandInfo handInfo = mHandDetector.detectHand(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                        width, height, width * 4,
                        rotation, BytedEffectConstants.HandModelType.BEF_HAND_MODEL_DETECT.getValue() |
                                BytedEffectConstants.HandModelType.BEF_HAND_MODEL_BOX_REG.getValue() |
                                BytedEffectConstants.HandModelType.BEF_HAND_MODEL_GESTURE_CLS.getValue() |
                                BytedEffectConstants.HandModelType.BEF_HAND_MODEL_KEY_POINT.getValue(), HAND_DETECT_DELAY_FRAME_COUNT);
                if (handInfo != null) {
                    Log.d(TAG, handInfo.toString());
                    mGLRender.drawHands(handInfo, dstTexture);
                } else {
                    Log.d(TAG, "handInfo is null");

                }
                dispatchResult(BefHandInfo.class, handInfo, framecounts);
            }

            if (isMattingPortrait) {
                if (resizeInputBuffer == null)
                    resizeInputBuffer = mGLRender.getResizeOutputTextureBuffer(dstTexture);
                resizeInputBuffer.position(0);
                PortraitMatting.MattingMask mattingMask = mPortaitMatting.detectMatting(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                        width, height, width * 4,
                        rotation, false);
                if (mattingMask != null) {
                    Log.d(TAG, mattingMask.toString());
                    mGLRender.drawMattingMask(mattingMask, dstTexture);
                } else {
                    Log.d(TAG, "potrait mask is null");

                }
            }

            if (isParsingHair) {
                if (resizeInputBuffer == null)
                    resizeInputBuffer = mGLRender.getResizeOutputTextureBuffer(dstTexture);
                resizeInputBuffer.position(0);
                HairParser.HairMask hairMask = mHairParser.parseHair(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                        width, height, width * 4,
                        rotation, false);
                if (hairMask != null) {
                    Log.d(TAG, hairMask.toString());
                    mGLRender.drawHairMask(hairMask, dstTexture);
                } else {
                    Log.d(TAG, "hairMask is null");

                }
            }

            if (isDetectDistance) {
                if (resizeInputBuffer == null)
                    resizeInputBuffer = mGLRender.getResizeOutputTextureBuffer(dstTexture);
                resizeInputBuffer.position(0);

                BefDistanceInfo humanDistanceResult = mHumanDistance.detectDistance(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                        width, height, width * 4,
                        rotation);
                if (humanDistanceResult != null){
                    mGLRender.drawHumanDist(humanDistanceResult, dstTexture);
                }

                dispatchResult(BefDistanceInfo.class, humanDistanceResult, framecounts);


            }

            if (isDetectPetFace){
                if (resizeInputBuffer == null)
                    resizeInputBuffer = mGLRender.getResizeOutputTextureBuffer(dstTexture);
                resizeInputBuffer.position(0);
                BefPetFaceInfo petFaceInfo = mPetFaceDetector.detectFace(resizeInputBuffer, BytedEffectConstants.PixlFormat.RGBA8888,
                        width, height, width * 4,
                        rotation);
                if (petFaceInfo != null) {
                    Log.d(TAG, petFaceInfo.toString());
                    mGLRender.drawPetFaces(petFaceInfo, dstTexture);
                } else {
                    Log.d(TAG, "petFaceInfo is null");

                }
                dispatchResult(BefPetFaceInfo.class, petFaceInfo, framecounts);
            }

            if (mMsgList.size() > 0) {
                // 扫码
                if (resizeInputBuffer == null) {
                    resizeInputBuffer = mGLRender.getResizeOutputTextureBuffer(dstTexture);
                }
                resizeInputBuffer.position(0);
                Message message = mMsgList.poll();
                if (null != message) {
                    message.obj = resizeInputBuffer;
                    message.arg1 = width;
                    message.arg2 = height;
                    message.sendToTarget();
                }


            }

            if (AppUtils.isDebug() && mainView.mImageView.getVisibility() == View.VISIBLE) {
                if (resizeInputBuffer != null) {
                    getBitmapFromPixels(resizeInputBuffer, width, height);
                } else {
                    resizeInputBuffer = mGLRender.getResizeOutputTextureBuffer(dstTexture);
                    getBitmapFromPixels(resizeInputBuffer, width, height);
                }

                mainView.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainView.mImageView.setImageBitmap(mCameraBitmap);
                    }
                });
            }
        }


        return dstTexture;
    }

    private ConcurrentLinkedQueue<Message> mMsgList = new ConcurrentLinkedQueue<>();

    /**
     * 请求一帧
     *
     * @param msg 消息体
     */
    public void requestPreviewFrame(Message msg) {
        mMsgList.offer(msg);
    }

    /**
     * 根据监听器的泛型参数的类型 决定是否需要将结果分发到接口上
     *
     * @param filter      结果类型
     * @param o           结果
     * @param framecounts 帧数
     */
    public void dispatchResult(Class filter, Object o, int framecounts) {
        if (null != callbacks) {
            for (ResultCallback calback : callbacks) {
                Class realType = calback.getRealGenericType();
                if (null != realType && realType.getName().equals(filter.getName())) {
                    calback.doResult(o, framecounts);
                }
            }
        }
    }

    public void drawFrame(int textureId) {
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        mGLRender.onDrawFrame(textureId);
    }

    public void initViewPort(int width, int height) {
        if (width != 0 && height != 0) {
            this.mSurfaceWidth = width;
            this.mSurfaceHeight = height;
            GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
            mGLRender.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight);
            mGLRender.init(mImageWidth, mImageHeight);
        }
    }

    public void adjustTextureBuffer(int orientation, boolean flipHorizontal, boolean flipVertical) {
        mGLRender.adjustTextureBuffer(orientation, flipHorizontal, flipVertical);
    }

    /**
     * 设置SDK的输入尺寸，该尺寸是转至人脸为正后的宽高
     * @param mImageWidth
     * @param mImageHeight
     */
    public void setImageSize(int mImageWidth, int mImageHeight) {
        this.mImageHeight = mImageHeight;
        this.mImageWidth = mImageWidth;
    }

    /**
     * 工作在渲染线程
     */
    public void destroySDKModules() {
        LogUtils.d("EffectRenderHelper destroySDKModules");
        mRenderManager.release();
        mGLRender.destroy();
        mFaceDetector.release();
        mSkeletonDetector.release();
        mHandDetector.release();
        mPortaitMatting.release();
        mHairParser.release();
        mFaceVerify.release();
        mHumanDistance.release();
        mPetFaceDetector.release();

        initedSDKModules = false;
        if (mCameraBitmap != null && !mCameraBitmap.isRecycled()) {
            mCameraBitmap.recycle();
            mCameraBitmap = null;
        }
        LogUtils.d("destroySDKModules finish");
    }

    private void sendUIToastMsg(final String msg) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private volatile boolean initedSDKModules = false;

    public void initSDKModules() {
        if (initedSDKModules)
            return;

        // 初始化手势检测器
        int ret = initHandDetector(mContext);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initHandDetector ret ="+ret);
            sendUIToastMsg("Hand Initialization failed");
        }

        ret = initFace(mContext);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initFace ret ="+ret);
            sendUIToastMsg("FaceInitialization failed");
        }

        ret = initPetFaceDetect(mContext);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initPetFace ret ="+ret);
            sendUIToastMsg("PetFaceInitialization failed");
        }

        ret = initFaceExtra(mContext);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initFaceExtra ret ="+ret);
            sendUIToastMsg("Face Extra Initialization failed");
        }

        ret = initFaceAttri(mContext);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initFaceAttri ret ="+ret);
            sendUIToastMsg("FaceAttr Initialization failed");
        }

        ret = initEffect(mContext);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initEffect ret ="+ret);
            sendUIToastMsg("Effect Initialization failed");
        }

        ret = initSkeleton(mContext);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initSkeleton ret ="+ret);
            sendUIToastMsg("Skeleton Initialization failed");
        }

        ret = initPortraitMatting(mContext, BytedEffectConstants.PortraitMatting.BEF_PORTAITMATTING_SMALL_MODEL);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initPortraitMatting ret ="+ret);
            sendUIToastMsg("portraitmatting Initialization failed");
        }

        ret = initHairParse(mContext, InputSizeManager.HairCutInput.x, InputSizeManager.HairCutInput.y);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initHairParse ret ="+ret);
            sendUIToastMsg("hairparsing Initialization failed");
        }
        float cameraFov = 60;
        if (AppUtils.isTv(mContext)){
            cameraFov = 36;
        }
        ret = initHumanDistance(mContext, cameraFov);
        if (ret != BEF_RESULT_SUC) {
            LogUtils.e("initHumanDistance ret ="+ret);
            sendUIToastMsg("HumanDistance Initialization failed");
        }
        initedSDKModules = true;
    }

    public void setEffectOn(boolean isOn) {
        isEffectOn = isOn;
    }

    /**
     * 开启或者关闭滤镜 如果path为空 关闭滤镜
     *
     * @param path 滤镜资源文件路径
     */
    public boolean setFilter(String path) {
        mFilterResource = path;
        return mRenderManager.setFilter(path);
    }

    /**
     * 设置特效组合，目前仅支持美颜 美妆 两种特效的任意叠加
     * @param nodes
     * @return
     */
    public boolean setComposeNodes(String[] nodes) {
        if (isShouldResetComposer) {
            int ret = mRenderManager.setComposer(ResourceHelper.getComposeMakeupComposerPath(mContext));
            if (ret != BEF_RESULT_SUC) {
                return false;
            }
            isShouldResetComposer = false;
        }

        // clear mSavedComposerNodes cache when nodes length is 0
        if (nodes.length == 0) {
            mSavedComposerNodes.clear();
        }

        mComposeNodes = nodes;
        String prefix = ResourceHelper.getComposePath(mContext);
        String[] path = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            path[i] = prefix + nodes[i];
        }
        return mRenderManager.setComposerNodes(path) == BEF_RESULT_SUC;
    }

    /**
     * 更新组合特效中某个节点的强度
     * @param node 特效素材对应的 ComposerNode
     * @return
     */
    public boolean updateComposeNode(ComposerNode node) {
        mSavedComposerNodes.add(node);
        String path = ResourceHelper.getComposePath(mContext) + node.getNode();
        return mRenderManager.updateComposerNodes(path, node.getKey(), node.getValue()) == BEF_RESULT_SUC;
    }

    /**
     * 开启或者关闭贴纸 如果path为空 关闭贴纸
     * 注意 贴纸和Composer类型的特效（美颜、美妆）是互斥的，如果同时设置设置，后者会取消前者的效果
     * @param path 贴纸素材的文件路径
     */
    public boolean setSticker(String path) {
        isShouldResetComposer = true;
        mStickerResource = path;
        return mRenderManager.setSticker(path);
    }

    /**
     * 设置美颜/滤镜(除塑形)强度
     *
     * @param intensitytype 参数类型
     * @param intensity     参数值
     * @return true or false 是否成功
     */
    public boolean updateIntensity(BytedEffectConstants.IntensityType intensitytype, float intensity) {
        boolean result = mRenderManager.updateIntensity(intensitytype.getId(), intensity);
        if (result){
            storedIntensities.put(intensitytype, intensity);
        }
        return result;

    }

    /**
     * 开启人体关键点检测
     *
     * @param flag
     */
    public void setSkeletonOn(boolean flag) {

        isDetectSkeleton = flag;

    }

    /**
     * 开启/关闭 人脸106关键点检测
     *
     * @param flag
     */
    public void setFaceDetectOn(boolean flag) {
        if (mFaceDetector.isInited()) {
            int detectConfig = -1;
            if (flag) {
                detectConfig = BytedEffectConstants.FaceAction.BEF_FACE_DETECT | BytedEffectConstants.DetectMode.BEF_DETECT_MODE_VIDEO | BytedEffectConstants.FaceAction.BEF_DETECT_FULL;
                mFaceDetector.setFaceDetectConfig(detectConfig);
            }
            isDetect106Face = flag;


        } else {
            isDetect106Face = false;
            LogUtils.e("Face106 Initialization failed！");
        }


    }

     /**
     * 开启/关闭 宠物脸关键点检测
     *
     * @param flag
     */
    public void setPetDetectOn(boolean flag) {
        if (mPetFaceDetector.isInited()) {
            isDetectPetFace = flag;
        } else {
            isDetectPetFace = false;
            LogUtils.e("PetFace Initialization failed！");
        }


    }



    /**
     * 开启或者关闭距离估计
     * @param flag
     */
    public void setHumanDistOn(boolean flag){
        isDetectDistance  = flag;


    }

    /**
     * 开启/关闭人脸280关键点
     *
     * @param flag
     */
    public void setFaceExtraOn(boolean flag) {
        if (mFaceDetector.isInited() && isDetect106Face) {

            int detectConfig = (BytedEffectConstants.FaceAction.BEF_FACE_DETECT | BytedEffectConstants.DetectMode.BEF_DETECT_MODE_VIDEO | BytedEffectConstants.FaceAction.BEF_DETECT_FULL);

            if (flag) {
                detectConfig |= BEF_MOBILE_FACE_280_DETECT;

            }
            mFaceDetector.setFaceDetectConfig(detectConfig);

            isDetectFaceExtra = flag;


        } else {
            isDetectFaceExtra = false;
        }
    }

    /**
     * 开启/关闭人脸属性检测
     *
     * @param flag
     */
    public void setFaceAttriOn(boolean flag) {
        if (mFaceDetector.isInited() && mFaceDetector.isInitedAttri() && isDetect106Face) {
            int attrConfig = 0;
            if (flag) {
                attrConfig |= (BEF_FACE_ATTRIBUTE_EXPRESSION | BEF_FACE_ATTRIBUTE_HAPPINESS |
                        BEF_FACE_ATTRIBUTE_AGE | BEF_FACE_ATTRIBUTE_GENDER |
                        BEF_FACE_ATTRIBUTE_RACIAL | BEF_FACE_ATTRIBUTE_ATTRACTIVE);
            }

            mFaceDetector.setAttriDetectConfig(attrConfig);
            isDetectFaceAttr = flag;


        } else {
            isDetectFaceAttr = false;
        }

    }


    /**
     * 开启/关闭手势检测
     *
     * @param flag
     */
    public void setHandDetectOn(boolean flag) {
        isDetectHand = flag;

    }

    public void setPortraitMattingOn(boolean flag) {
        isMattingPortrait = flag;


    }

    public void setParsingHair(boolean parsingHair) {
        isParsingHair = parsingHair;


    }

    /**
     * 获取压缩率
     *
     * @return
     */
    public float getRadio() {
        return mGLRender.getResizeRatio();
    }

    /**
     * 恢复美颜、滤镜等设置参数
     */
    public void recoverStatus(Context mContext) {
        if (!TextUtils.isEmpty(mFilterResource)) {
            setFilter(mFilterResource);

        }
        if (!TextUtils.isEmpty(mStickerResource)){
            setSticker(mStickerResource);
        }

        if (mComposeNodes.length > 0) {
            setComposeNodes(mComposeNodes);

            for (ComposerNode node : mSavedComposerNodes) {
                updateComposeNode(node);
            }
        }
        for (BytedEffectConstants.IntensityType entryk : storedIntensities.keySet()) {
            updateIntensity(entryk, storedIntensities.get(entryk));
        }

    }

    public boolean isFaceAttrOn() {
        return isDetectFaceAttr;
    }


}
