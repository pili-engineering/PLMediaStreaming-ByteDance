// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.bytedance.labcv.demo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;

import com.bef.effectsdk.OpenGLUtils;
import com.bytedance.labcv.demo.opengl.HairMaskProgram;
import com.bytedance.labcv.demo.opengl.LineProgram;
import com.bytedance.labcv.demo.opengl.MaskProgram;
import com.bytedance.labcv.demo.opengl.PointProgram;
import com.bytedance.labcv.demo.opengl.PortraitMaskProgram;
import com.bytedance.labcv.demo.opengl.ShaderHelper;
import com.bytedance.labcv.demo.utils.AppUtils;
import com.bytedance.labcv.demo.utils.InputSizeManager;
import com.bytedance.labcv.demo.utils.TextureRotationUtil;
import com.bytedance.labcv.effectsdk.BefDistanceInfo;
import com.bytedance.labcv.effectsdk.BefFaceInfo;
import com.bytedance.labcv.effectsdk.BefHandInfo;
import com.bytedance.labcv.effectsdk.BefPetFaceInfo;
import com.bytedance.labcv.effectsdk.BefSkeletonInfo;
import com.bytedance.labcv.effectsdk.HairParser;
import com.bytedance.labcv.effectsdk.PortraitMatting;
import com.bytedance.labcv.effectsdk.library.LogUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;


public class OpenGLRender {
    private final static String TAG = "OpenGLRender";
    private  static float DRAW_POINT_SIZE = 4f;

    /**
     * glReadPixel读取高分辨率图片速度较慢，可以根据情况使用CPU或者OpenGLES3.0读取数据，
     * 这里采用显示分辨率的一半做处理，稍微影响检测人脸的精度，较小的人脸可能检测不到
     */
    private  static float mResizeRatio = 0.5f;
    private static final String CAMERA_INPUT_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "	textureCoordinate = inputTextureCoordinate.xy;\n" +
            "	gl_Position = position;\n" +
            "}";

    private static final String CAMERA_INPUT_FRAGMENT_SHADER_OES = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "	gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    public static final String CAMERA_INPUT_FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    // portraitmatting  mask color
    private float[] mPortraitColor = {1.0f, 0f, 0f , 0.5f};
    // hair color
    private float[] mHairColor = {0.5f, 0.08f, 1f , 0.3f};


    private final static String PROGRAM_ID = "program";
    private final static String POSITION_COORDINATE = "position";
    private final static String TEXTURE_UNIFORM = "inputImageTexture";
    private final static String TEXTURE_COORDINATE = "inputTextureCoordinate";
    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private final FloatBuffer mGLTextureBufferNormal;

    private Context mContext;

    private MainActivity mainActivity;

    private FloatBuffer mTextureBuffer;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mOriginVertexBuffer;
    private FloatBuffer mResizeVertexBuffer;

    private PointProgram mPointProgram;
    private LineProgram mLineProgram;
    private MaskProgram mPortraitMaskProgram;
    private MaskProgram mHairMaskProgram;


    private boolean mIsInitialized;
    private ArrayList<HashMap<String, Integer>> mArrayPrograms = new ArrayList<HashMap<String, Integer>>(2) {
        {
            for (int i = 0; i < 2; ++i) {
                HashMap<String, Integer> hashMap = new HashMap<>();
                hashMap.put(PROGRAM_ID, 0);
                hashMap.put(POSITION_COORDINATE, -1);
                hashMap.put(TEXTURE_UNIFORM, -1);
                hashMap.put(TEXTURE_COORDINATE, -1);
                add(hashMap);
            }
        }
    };
    // 纹理宽度
    private int mViewPortWidth;
    // 纹理高度
    private int mViewPortHeight;
    private final static int FRAME_BUFFER_NUM = 3;
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;

    private ByteBuffer mImageBuffer;


    private IntBuffer mPboIds;
    private int mPboSize;

    private final int mPixelStride = 4;//RGBA 4字节
    private int mPboIndex;
    private int mPboNewIndex;
    private boolean mInitRecord;

    // yuv检测返回的人脸结果
    private BefFaceInfo faceInfo;
    // yuv检测返回的人脸结果
    private BefPetFaceInfo petFaceInfo;
    // yuv检测返回的骨骼数据
    private BefSkeletonInfo skeletonInfo;
    // yuv检测返回的手势数据
    private BefHandInfo handInfo;

    private PortraitMatting.MattingMask mattingMask;
    private HairParser.HairMask mHairMask;

    public void setFaceInfo(BefFaceInfo faceInfo) {
        this.faceInfo = faceInfo;
    }

    public void setPetFaceInfo(BefPetFaceInfo faceInfo) {
        this.petFaceInfo = faceInfo;
    }

    public BefFaceInfo getFaceInfo(){
        return faceInfo;
    }

    public BefPetFaceInfo getPetFaceInfo(){
        return petFaceInfo;
    }

    public BefSkeletonInfo getSkeletonInfo() {
        return skeletonInfo;
    }

    public void setSkeletonInfo(BefSkeletonInfo skeletonInfo) {
        this.skeletonInfo = skeletonInfo;
    }

    public BefHandInfo getHandInfo() {
        return handInfo;
    }

    public void setHandInfo(BefHandInfo handInfo) {
        this.handInfo = handInfo;
    }

    public PortraitMatting.MattingMask getMattingMask() {
        return mattingMask;
    }

    public void setMattingMask(PortraitMatting.MattingMask mattingMask) {
        this.mattingMask = mattingMask;
    }

    public HairParser.HairMask getmHairMask() {
        return mHairMask;
    }

    public void setmHairMask(HairParser.HairMask mHairMask) {
        this.mHairMask = mHairMask;
    }

    public OpenGLRender(Context context) {
        mContext = context;

        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_FLIPPED.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        /* */
        /**
         * 用来绘制到屏幕的纹理坐标 注意需要y轴翻转
         *          1  2
         *          3  4
         */
        mGLTextureBuffer.put(TextureRotationUtil.TEXTURE_FLIPPED).position(0);


        mGLTextureBufferNormal = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_FLIPPED.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        /**
         * 用来从纹理中读取像素 需要保证方向跟定点坐标一致 保证读取到的跟preprocess调整后的一致
         *       3 4
         *       1 2
         */
        mGLTextureBufferNormal.put(TextureRotationUtil.TEXTURE_ROTATED_0).position(0);
    }

    /**
     * 在性能较低的GPU上使用双PBO从GPU中读取像素能节省大部分时间
     *
     * @param width
     * @param height
     */
    public void initPixelBuffer(int width, int height) {
        if (mPboIds != null) {
            destroyPixelBuffers();
        }
        if (mPboIds != null) {
            return;
        }
        mPboSize = width * height * mPixelStride;

        mPboIds = IntBuffer.allocate(2);
        GLES30.glGenBuffers(2, mPboIds);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(0));
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mPboSize, null, GLES30.GL_STATIC_READ);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(1));
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mPboSize, null, GLES30.GL_STATIC_READ);

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
    }


    private void bindPixelBuffer(int width, int height) {
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(mPboIndex));
        OpenGLUtils.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);
        if (mInitRecord) {
            unbindPixelBuffer();
            mInitRecord = false;
            return;
        }
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(mPboNewIndex));

        mImageBuffer = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, mPboSize, GLES30.GL_MAP_READ_BIT);
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
        unbindPixelBuffer();
    }

    private void unbindPixelBuffer() {
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);

        mPboIndex = (mPboIndex + 1) % 2;
        mPboNewIndex = (mPboNewIndex + 1) % 2;
    }

    private void destroyPixelBuffers() {
        if (mPboIds != null) {
            GLES30.glDeleteBuffers(2, mPboIds);
            mPboIds = null;
        }
    }

    public void setMainActivity(MainActivity m) {
        mainActivity = m;
    }

    public void init(int width, int height) {
        if (mViewPortWidth == width && mViewPortHeight == height) {
            return;
        }
        if (height > 1000) {
            DRAW_POINT_SIZE = 4f;
        } else {
            DRAW_POINT_SIZE = 3f;
        }
        initProgram(CAMERA_INPUT_FRAGMENT_SHADER_OES, mArrayPrograms.get(0));
        initProgram(CAMERA_INPUT_FRAGMENT_SHADER, mArrayPrograms.get(1));
        mViewPortWidth = width;
        mViewPortHeight = height;
        float maxPossibleRatio = InputSizeManager.getMaxPossibleRatio(mContext, width, height);
        LogUtils.d("maxPossibleRatio ="+maxPossibleRatio);
        int resizedWidth = (int) Math.ceil(width *maxPossibleRatio);
        int resizedHeight = (int) Math.ceil(height * maxPossibleRatio);
        LogUtils.d("resizedWidth ="+resizedWidth);
        LogUtils.d("resizedHeight ="+resizedHeight);

        mImageBuffer = ByteBuffer.allocateDirect(resizedWidth *resizedHeight * 4);
        initFrameBuffers(width, height);
        if (AppUtils.isAccGlReadPixels()){
            // init two pbos to accumate glreadpixels, open it if you need to accumate glreadPixels
            initPixelBuffer(width, height);
        }

        mIsInitialized = true;
        mInitRecord = true;
        mPboIndex = 0;
        mPboNewIndex = 1;

    }

    private void initProgram(String fragment, HashMap<String, Integer> programInfo) {
        int proID = programInfo.get(PROGRAM_ID);
        if (proID == 0) {
            proID = ShaderHelper.buildProgram(CAMERA_INPUT_VERTEX_SHADER, fragment);
            programInfo.put(PROGRAM_ID, proID);
            programInfo.put(POSITION_COORDINATE, GLES20.glGetAttribLocation(proID, POSITION_COORDINATE));
            programInfo.put(TEXTURE_UNIFORM, GLES20.glGetUniformLocation(proID, TEXTURE_UNIFORM));
            programInfo.put(TEXTURE_COORDINATE, GLES20.glGetAttribLocation(proID, TEXTURE_COORDINATE));
        }
    }

    public void adjustTextureBuffer(int orientation, boolean flipHorizontal, boolean flipVertical) {
        float[] textureCords = TextureRotationUtil.getRotation(orientation, flipHorizontal, flipVertical);
        if (mTextureBuffer == null) {
            mTextureBuffer = ByteBuffer.allocateDirect(textureCords.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mTextureBuffer.clear();
        mTextureBuffer.put(textureCords).position(0);
    }

    private float[] calcVertex(int displayW, int displayH, int imageW, int imageH) {
        int outputHeight = displayH;
        int outputWidth = displayW;

        float ratio1 = (float) outputWidth / imageW;
        float ratio2 = (float) outputHeight / imageH;

        float ratio = Math.min(ratio1, ratio2);

        int imageWidthNew = Math.round(imageW * ratio);
        int imageHeightNew = Math.round(imageH * ratio);


        float ratioWidth = imageWidthNew / (float) outputWidth;
        float ratioHeight = imageHeightNew / (float) outputHeight;


        float[] cube = new float[]{
                TextureRotationUtil.CUBE[0] / ratioHeight, TextureRotationUtil.CUBE[1] / ratioWidth,
                TextureRotationUtil.CUBE[2] / ratioHeight, TextureRotationUtil.CUBE[3] / ratioWidth,
                TextureRotationUtil.CUBE[4] / ratioHeight, TextureRotationUtil.CUBE[5] / ratioWidth,
                TextureRotationUtil.CUBE[6] / ratioHeight, TextureRotationUtil.CUBE[7] / ratioWidth,
        };


        return cube;
    }


    public void calculateVertexBuffer(int displayW, int displayH, int imageW, int imageH) {
        float[] cube = calcVertex(displayW, displayH, imageW, imageH);
        if (mVertexBuffer == null) {
            mVertexBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mVertexBuffer.clear();
        mVertexBuffer.put(cube).position(0);
        if (AppUtils.isDebug()) {
            mainActivity.info = new StringBuilder();
            mainActivity.info.append("Camera ori=" + CameraDevice.get().getOrientation() + "  displayW =" + displayW + " displayH =" + displayH + " imageW =" + imageW + " imageH =" + imageH);
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.tvInfo.setText(mainActivity.info);

                }
            });
        }

    }

    /**
     * 此函数有三个功能
     * 1. 将OES的纹理转换为标准的GL_TEXTURE_2D格式
     * 2. 根据相机预览帧的旋转角度将纹理转正（即人脸为正），如果是前置摄像头，还会进行水平翻转
     * 3. 读取上面两个步骤后纹理的内容到cpu内存，存储为RGBA格式的buffer
     *
     * @param textureId 输入的OES的纹理id
     * @return 转换后的GL_TEXTURE_2D的纹理id
     */
    public int preProcess(int textureId) {
        if (mFrameBuffers == null
                || !mIsInitialized)
            return -2;

        GLES20.glUseProgram(mArrayPrograms.get(0).get(PROGRAM_ID));
        ShaderHelper.checkGlError("glUseProgram " + mArrayPrograms.get(0).get(PROGRAM_ID));
        //设置顶点的属性
        mGLCubeBuffer.position(0);
        int glAttribPosition = mArrayPrograms.get(0).get(POSITION_COORDINATE);
        //设置顶点buffer的类型和访问的数据偏移量
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        //接下来渲染使用缓存中的数据
        GLES20.glEnableVertexAttribArray(glAttribPosition);

        mTextureBuffer.position(0);
        int glAttribTextureCoordinate = mArrayPrograms.get(0).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);

        if (textureId != -1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mArrayPrograms.get(0).get(TEXTURE_UNIFORM), 0);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        ShaderHelper.checkGlError("glBindFramebuffer");

        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glUseProgram(0);

        return mFrameBufferTextures[0];
    }

    public int getOutputTexture() {
        if (mFrameBufferTextures != null) {
            return mFrameBufferTextures[1];
        }
        return ShaderHelper.NO_TEXTURE;
    }

    /**
     * 获取opengld读取图像时压缩的倍率
     *
     * @return
     */
    public float getResizeRatio() {
        return mResizeRatio;
    }

    public void setResizeRatio(float ratio) {
         mResizeRatio = ratio;
    }

    public ByteBuffer getResizeOutputTextureBuffer(int texture) {

        int width = (int) (mViewPortWidth * mResizeRatio);
        int height = (int) (mViewPortHeight * mResizeRatio);

        GLES20.glUseProgram(mArrayPrograms.get(1).get(PROGRAM_ID));
        float[] cube = calcVertex(width, height, mViewPortWidth, mViewPortHeight);
        if (mResizeVertexBuffer == null) {
            mResizeVertexBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mResizeVertexBuffer.clear();
        mResizeVertexBuffer.put(cube).position(0);
        int glAttribPosition = mArrayPrograms.get(1).get(POSITION_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mResizeVertexBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);

        mGLTextureBufferNormal.position(0);
        int glAttribTextureCoordinate = mArrayPrograms.get(1).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                mGLTextureBufferNormal);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);
        //将纹理绑定
        if (texture != ShaderHelper.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
            GLES20.glUniform1i(mArrayPrograms.get(1).get(TEXTURE_UNIFORM), 0);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[2]);

        GLES20.glViewport(0, 0, width, height);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[2], 0);
        mImageBuffer.position(0);
//        long start = System.currentTimeMillis();
        if (AppUtils.isAccGlReadPixels()) {
            // use two pbos to accumate glreadpixels, replace with it if you need to accumate glreadPixels
            bindPixelBuffer(width, height);
        } else {
            GLES20.glReadPixels(0, 0, width, height,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mImageBuffer);
        }
//        long end = System.currentTimeMillis();
//        LogUtils.d("glReadPixels cost ="+(end - start));
        GLES20.glViewport(0, 0, width, height);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return mImageBuffer;
    }

    /**
     * 源图像和目标图像几何中心的对齐
     * @param dstX
     * @param scale
     * @return
     */
    protected float transformCenterAlign(float dstX,float scale) {
        return dstX*scale+0.5f*(scale -1);
    }

    public void drawFaces(BefFaceInfo faceInfo, int texture) {
        if (mLineProgram == null) {
            mLineProgram = new LineProgram(mContext, mViewPortWidth, mViewPortHeight);
        }
        if (mPointProgram == null) {
            mPointProgram = new PointProgram(mContext, mViewPortWidth, mViewPortHeight);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);

        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);
        for (BefFaceInfo.Face106 face106 : faceInfo.getFace106s()) {
            BefFaceInfo.FaceRect rect = face106.getRect();
            float left = transformCenterAlign(rect.getLeft(), 1/mResizeRatio);
            float right = transformCenterAlign(rect.getRight(),1/mResizeRatio);
            float top = transformCenterAlign(rect.getTop(),1/mResizeRatio);
            float bottom =  transformCenterAlign(rect.getBottom(),1/mResizeRatio);

            mLineProgram.drawRect(new RectF(left, top, right, bottom), Color.RED, DRAW_POINT_SIZE);

            for (BefFaceInfo.FacePoint point : face106.getPoints_array()) {
                PointF target = point.asPoint();
                target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                mPointProgram.draw(target, Color.RED, DRAW_POINT_SIZE);
            }
        }
        if (faceInfo.getExtras() != null) {
            for (BefFaceInfo.ExtraInfo extraInfo : faceInfo.getExtras()) {
                for (BefFaceInfo.FacePoint point : extraInfo.getEye_left()) {
                    PointF target = point.asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.rgb(200, 0, 0), DRAW_POINT_SIZE - 1.0f);
                }
                for (BefFaceInfo.FacePoint point : extraInfo.getEye_right()) {
                    PointF target = point.asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.rgb(200, 0, 0), DRAW_POINT_SIZE - 1.0f);
                }
                for (BefFaceInfo.FacePoint point : extraInfo.getEyebrow_left()) {
                    PointF target = point.asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.rgb(220, 0, 0), DRAW_POINT_SIZE - 1.0f);
                }
                for (BefFaceInfo.FacePoint point : extraInfo.getEyebrow_right()) {
                    PointF target = point.asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.rgb(220, 0, 0), DRAW_POINT_SIZE - 1.0f);
                }
                for (BefFaceInfo.FacePoint point : extraInfo.getLeft_iris()) {
                    PointF target = point.asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.parseColor("#FFB400"), DRAW_POINT_SIZE - 1.0f);
                }
                if (extraInfo.getLeft_iris().length > 0) {
                    PointF target = extraInfo.getLeft_iris()[0].asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.GREEN, DRAW_POINT_SIZE - 1.0f);
                }
                for (BefFaceInfo.FacePoint point : extraInfo.getRight_iris()) {
                    PointF target = point.asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.parseColor("#FFB400"), DRAW_POINT_SIZE - 1.0f);
                }
                if (extraInfo.getRight_iris().length > 0) {
                    PointF target = extraInfo.getRight_iris()[0].asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.GREEN, DRAW_POINT_SIZE - 1.0f);
                }
                for (BefFaceInfo.FacePoint point : extraInfo.getLips()) {
                    PointF target = point.asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.rgb(200, 40, 40), DRAW_POINT_SIZE - 1.0f);
                }
            }
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void drawSkeleton(BefSkeletonInfo skeletonInfo, int textureid) {
        if (mPointProgram == null) {
            mPointProgram = new PointProgram(mContext, mViewPortWidth, mViewPortHeight);
        }
        if (mLineProgram == null) {
            mLineProgram = new LineProgram(mContext, mViewPortWidth, mViewPortHeight);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureid, 0);

        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        for (BefSkeletonInfo.Skeleton skeleton : skeletonInfo.getSkeletons()) {
            for (BefSkeletonInfo.SkeletonPoint skeletonPoint : skeleton.getKeypoints()) {
                if (skeletonPoint != null && skeletonPoint.isDetect()) {
                    PointF target = skeletonPoint.asPoint();
                    target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                    target.y = transformCenterAlign(target.y, 1/mResizeRatio);
                    mPointProgram.draw(target, Color.BLUE, DRAW_POINT_SIZE * 2);
                }
            }
            BefFaceInfo.FaceRect rect = skeleton.getSkeletonRect();
            float left = transformCenterAlign(rect.getLeft(), 1/mResizeRatio);
            float right = transformCenterAlign(rect.getRight(),1/mResizeRatio);
            float top = transformCenterAlign(rect.getTop(),1/mResizeRatio);
            float bottom =  transformCenterAlign(rect.getBottom(),1/mResizeRatio);

            mLineProgram.drawRect(new RectF(left, top, right, bottom), Color.RED, DRAW_POINT_SIZE);
        }
        mLineProgram.drawLines(getSkeletonLineDraws(skeletonInfo.getSkeletons()), Color.BLUE, 4);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }


    public void drawHumanDist(BefDistanceInfo humanDistanceResult, int texture){
        if (null == humanDistanceResult) return;
        BefFaceInfo.FaceRect[] rects = humanDistanceResult.getFaceRects();
        if (null == rects || rects.length == 0)return;

        if (mLineProgram == null) {
            mLineProgram = new LineProgram(mContext, mViewPortWidth, mViewPortHeight);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);

        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);
        for (BefFaceInfo.FaceRect rect:rects){
            float left = transformCenterAlign(rect.getLeft(), 1/mResizeRatio);
            float right = transformCenterAlign(rect.getRight(),1/mResizeRatio);
            float top = transformCenterAlign(rect.getTop(),1/mResizeRatio);
            float bottom =  transformCenterAlign(rect.getBottom(),1/mResizeRatio);

            mLineProgram.drawRect(new RectF(left, top, right, bottom), Color.RED, DRAW_POINT_SIZE);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);




    }

    public PointF[] getSkeletonLineDraws(BefSkeletonInfo.Skeleton[] skeletons) {
        ArrayList<PointF> skeleps = new ArrayList<PointF>();
        for (BefSkeletonInfo.Skeleton skeleton : skeletons) {
            int[] armsp = {4, 3, 3, 2, 2, 1, 1, 5, 5, 6, 6, 7,
                    16, 14, 14, 0, 17, 15, 15, 0,
                    1, 8, 8, 11, 11, 1, 1, 0,
                    8, 9, 9, 10, 11, 12, 12, 13};
            for (int armi = 0; armi < armsp.length; armi += 2) {
                BefSkeletonInfo.SkeletonPoint armp = skeleton.getKeypoints()[armsp[armi]];
                BefSkeletonInfo.SkeletonPoint armpn = skeleton.getKeypoints()[armsp[armi + 1]];
                if (armp.isDetect() && armpn.isDetect()) {
                    skeleps.add(new PointF(transformCenterAlign(armp.asPoint().x , 1/mResizeRatio), transformCenterAlign(armp.asPoint().y , 1/mResizeRatio)));
                    skeleps.add(new PointF(transformCenterAlign(armpn.asPoint().x , 1/mResizeRatio), transformCenterAlign(armpn.asPoint().y , 1/mResizeRatio)));
                }
            }
        }
        return skeleps.toArray(new PointF[0]);
    }

    /**
     * 绘制手势结果
     *
     * @param handInfo
     * @param texture
     */
    public void drawHands(BefHandInfo handInfo, int texture) {

        if (mLineProgram == null) {
            mLineProgram = new LineProgram(mContext, mViewPortWidth, mViewPortHeight);
        }
        if (mPointProgram == null) {
            mPointProgram = new PointProgram(mContext, mViewPortWidth, mViewPortHeight);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);

        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        if (handInfo.getHandCount() > 0 && handInfo.getHands() != null) {
            for (BefHandInfo.BefHand hand : handInfo.getHands()) {
                // 绘制手框
                Rect rect = hand.getRect();
                float left = transformCenterAlign(rect.left, 1/ mResizeRatio);
                float right = transformCenterAlign(rect.right, 1/ mResizeRatio);
                float top = transformCenterAlign(rect.top, 1/ mResizeRatio);
                float bottom = transformCenterAlign(rect.bottom, 1/ mResizeRatio);
                mLineProgram.drawRect(new RectF(left, top, right, bottom), Color.RED, 1f);

                if (hand.getKeyPoints() != null && hand.getKeyPoints().length == 22) {

                    // 绘制手指线段
                    PointF[] points = new PointF[5];
                    points[0] = hand.getKeyPoints()[0].asPoint();
                    points[0].x = transformCenterAlign( points[0].x, 1/mResizeRatio);
                    points[0].y = transformCenterAlign( points[0].y, 1/mResizeRatio);
                    for (int n = 0; n < 5; n++) {
                        int index = 4 * n + 1;
                        for (int k = 1; k < 5; k++) {
                            points[k] = hand.getKeyPoints()[index++].asPoint();
                            points[k].x = transformCenterAlign( points[k].x, 1/mResizeRatio);
                            points[k].y = transformCenterAlign( points[k].y, 1/mResizeRatio);
                        }
                        mLineProgram.drawLineStrip(points, Color.RED, DRAW_POINT_SIZE);
                    }

                    // 绘制关键点
                    for (BefHandInfo.BefKeyPoint point : hand.getKeyPoints()) {
                        PointF target = point.asPoint();
                        target.x = transformCenterAlign( target.x, 1/mResizeRatio);
                        target.y = transformCenterAlign( target.y, 1/mResizeRatio);
                        mPointProgram.draw(target, Color.RED, DRAW_POINT_SIZE * 2);
                    }
                } else {
                    LogUtils.e("hand.getKeyPoints() == null");
                }

            }
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    /**
     * 绘制手势结果
     *
     * @param mattingMask
     * @param texture
     */
    public void drawMattingMask(PortraitMatting.MattingMask mattingMask, int texture) {
        if (mPortraitMaskProgram == null)
            mPortraitMaskProgram = new PortraitMaskProgram(mContext, mViewPortWidth, mViewPortHeight);

        if (mOriginVertexBuffer == null) {
            mOriginVertexBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mOriginVertexBuffer.clear();
            mOriginVertexBuffer.put(TextureRotationUtil.CUBE).position(0);
        }
        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);
        mPortraitMaskProgram.drawMask(mattingMask.getBuffer(), mattingMask.getWidth(), mattingMask.getHeight(),
                mOriginVertexBuffer, mGLTextureBuffer, mFrameBuffers[1], texture, mPortraitColor);
    }



    public void drawHairMask(HairParser.HairMask hairMask, int texture) {
        if (mHairMaskProgram == null)
            mHairMaskProgram = new HairMaskProgram(mContext, mViewPortWidth, mViewPortHeight);
        if (mOriginVertexBuffer == null) {
            mOriginVertexBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mOriginVertexBuffer.clear();
            mOriginVertexBuffer.put(TextureRotationUtil.CUBE).position(0);
        }
        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        mHairMaskProgram.drawMask(hairMask.getBuffer(), hairMask.getWidth(), hairMask.getHeight(),
                mOriginVertexBuffer, mGLTextureBuffer, mFrameBuffers[1], texture, mHairColor);
    }

    private void destroyFrameBuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(FRAME_BUFFER_NUM, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(FRAME_BUFFER_NUM, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    /**
     * 绘制图像
     */
    public int onDrawFrame(final int textureId) {
        if (!mIsInitialized) {
            return ShaderHelper.NOT_INIT;
        }

        GLES20.glUseProgram(mArrayPrograms.get(1).get(PROGRAM_ID));

        mVertexBuffer.position(0);
        int glAttribPosition = mArrayPrograms.get(1).get(POSITION_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);

        mGLTextureBuffer.position(0);
        int glAttribTextureCoordinate = mArrayPrograms.get(1).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);
        //将纹理绑定
        if (textureId != ShaderHelper.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mArrayPrograms.get(1).get(TEXTURE_UNIFORM), 0);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return ShaderHelper.ON_DRAWN;
    }

    /**
     * frameBuffer和texture的初始化
     */
    private void initFrameBuffers(int width, int height) {
        destroyFrameBuffers();

        if (mFrameBuffers == null) {
            mFrameBuffers = new int[FRAME_BUFFER_NUM];
            mFrameBufferTextures = new int[FRAME_BUFFER_NUM];

            GLES20.glGenFramebuffers(FRAME_BUFFER_NUM, mFrameBuffers, 0);
            GLES20.glGenTextures(FRAME_BUFFER_NUM, mFrameBufferTextures, 0);

            bindFrameBuffer(mFrameBufferTextures[0], mFrameBuffers[0], width, height);
            bindFrameBuffer(mFrameBufferTextures[1], mFrameBuffers[1], width, height);
            // 加速glReadPixel的读取速度
            bindFrameBuffer(mFrameBufferTextures[2], mFrameBuffers[2],
                    (int) (width ), (int) (height ));
        }
    }

    /**
     * 纹理参数设置+buffer绑定
     */
    private void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private ByteBuffer mCaptureBuffer = null;

    public ByteBuffer captureRenderResult(){
        int textureId = getOutputTexture();
        if (textureId == ShaderHelper.NO_TEXTURE) {
            return null;
        }
        if (mViewPortHeight* mViewPortWidth == 0){
            return  null;
        }
        if (null == mCaptureBuffer){
            mCaptureBuffer = ByteBuffer.allocateDirect(mViewPortHeight* mViewPortWidth*4);
        }
        mCaptureBuffer.position(0);
        int[] frameBuffer = new int[1];
        GLES20.glGenFramebuffers(1,frameBuffer,0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
        GLES20.glReadPixels(0, 0, mViewPortWidth, mViewPortHeight,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mCaptureBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        if (frameBuffer != null) {
            GLES20.glDeleteFramebuffers(1, frameBuffer, 0);
        }
        return mCaptureBuffer;
    }

    public final void destroy() {
        mIsInitialized = false;

        mViewPortWidth = 0;
        mViewPortHeight = 0;

        destroyFrameBuffers();
        GLES20.glDeleteProgram(mArrayPrograms.get(0).get(PROGRAM_ID));
        mArrayPrograms.get(0).put(PROGRAM_ID, 0);
        GLES20.glDeleteProgram(mArrayPrograms.get(1).get(PROGRAM_ID));
        mArrayPrograms.get(1).put(PROGRAM_ID, 0);

        if (mPointProgram != null) {
            mPointProgram.release();
            mPointProgram = null;
        }

        if (mLineProgram != null) {
            mLineProgram.release();
            mLineProgram = null;
        }

        if (mPortraitMaskProgram != null) {
            mPortraitMaskProgram.release();
            mPortraitMaskProgram = null;
        }

        if (mHairMaskProgram != null) {
            mHairMaskProgram.release();
            mHairMaskProgram = null;
        }
    }

    public void drawPetFaces(BefPetFaceInfo petFaceInfo, int texture) {
        if (mLineProgram == null) {
            mLineProgram = new LineProgram(mContext, mViewPortWidth, mViewPortHeight);
        }
        if (mPointProgram == null) {
            mPointProgram = new PointProgram(mContext, mViewPortWidth, mViewPortHeight);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);

        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        //绘制脸框
        for (BefPetFaceInfo.PetFace face90 : petFaceInfo.getFace90()) {
            BefFaceInfo.FaceRect rect = face90.getRect();
            float left = transformCenterAlign(rect.getLeft(), 1/mResizeRatio);
            float right = transformCenterAlign(rect.getRight(),1/mResizeRatio);
            float top = transformCenterAlign(rect.getTop(),1/mResizeRatio);
            float bottom =  transformCenterAlign(rect.getBottom(),1/mResizeRatio);

            mLineProgram.drawRect(new RectF(left, top, right, bottom), Color.RED, DRAW_POINT_SIZE);

            for (BefFaceInfo.FacePoint point : face90.getPoints_array()) {
                PointF target = point.asPoint();
                target.x  = transformCenterAlign(target.x, 1/mResizeRatio);
                target.y = transformCenterAlign(target.y, 1/mResizeRatio);

                mPointProgram.draw(target, Color.RED, DRAW_POINT_SIZE);
            }
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
}
