package com.bytedance.labcv.demo.utils;

import android.content.Context;
import android.graphics.Point;

import com.bytedance.labcv.demo.EffectRenderHelper;

/**
 * 不同算法模块组合的输入大小控制
 */
public class InputSizeManager {

    // 算法模块推荐输入大小
    public static final Point Face106Input = new Point(128, 224);
    public static final Point Face280Input = new Point(360, 640);
    public static final Point FaceAttriInput = new Point(360, 640);
    public static final Point HandInput = new Point(360, 640);
    public static final Point SkeletonInput = new Point(128, 224);
    public static final Point HairCutInput = new Point(128, 224);
    public static final Point MattingInput = new Point(128, 224);
    public static final Point QrdecodeInput = new Point(480, 480);
    public static final Point FaceVerifyInput = new Point(128, 224);
    public static final Point PetFaceInput = new Point(360, 640);

    /**
     * 根据开启的算法模块 计算最合适的输入大小 按照最大原则
     *
     * @param inputWidth  输入图像的宽度
     * @param inputHeight 输入图像的高度
     * @return
     */
    public static float getPreferSampleSize(Context context, EffectRenderHelper effectRenderHelper, int inputWidth, int inputHeight) {
        if (null == effectRenderHelper) return 0.5f;
        // 如果是电视场景 为了提升检测距离 不在opengl中降采样 在SDK中进行降采样
        if (AppUtils.isTv(context))return 1f;
        float xRatio = Face106Input.x * 1.0f / Math.min(inputWidth, inputHeight);
        float yRatio = Face106Input.y * 1.0f / Math.max(inputWidth, inputHeight);
        float preferRatio = Math.max(xRatio, yRatio);

        if (effectRenderHelper.isDetect106Face || effectRenderHelper.isDetectDistance) {
            xRatio = Face106Input.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = Face106Input.y * 1.0f / Math.max(inputWidth, inputHeight);


            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);

        }
        if (effectRenderHelper.isDetectFaceExtra) {
            xRatio = Face280Input.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = Face280Input.y * 1.0f / Math.max(inputWidth, inputHeight);
            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);

        }
        if (effectRenderHelper.isDetectFaceAttr) {
            xRatio = FaceAttriInput.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = FaceAttriInput.y * 1.0f / Math.max(inputWidth, inputHeight);
            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);

        }
        if (effectRenderHelper.isDetectHand) {
            xRatio = HandInput.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = HandInput.y * 1.0f / Math.max(inputWidth, inputHeight);
            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);

        }
        if (effectRenderHelper.isDetectSkeleton) {
            xRatio = SkeletonInput.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = SkeletonInput.y * 1.0f / Math.max(inputWidth, inputHeight);


            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);

        }
        if (effectRenderHelper.isMattingPortrait) {
            xRatio = MattingInput.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = MattingInput.y * 1.0f / Math.max(inputWidth, inputHeight);
            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);

        }

        if (effectRenderHelper.isParsingHair) {
            xRatio = HairCutInput.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = HairCutInput.y * 1.0f / Math.max(inputWidth, inputHeight);
            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);

        }
        if (effectRenderHelper.isFaceVerify) {
            xRatio = FaceVerifyInput.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = FaceVerifyInput.y * 1.0f / Math.max(inputWidth, inputHeight);
            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);

        }
        if (effectRenderHelper.isQrDecoding) {
            xRatio = QrdecodeInput.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = QrdecodeInput.y * 1.0f / Math.max(inputWidth, inputHeight);
            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);

        }
        if (effectRenderHelper.isDetectPetFace) {
            xRatio = PetFaceInput.x * 1.0f / Math.min(inputWidth, inputHeight);
            yRatio = PetFaceInput.y * 1.0f / Math.max(inputWidth, inputHeight);
            float max = Math.max(xRatio, yRatio);
            preferRatio = Math.max(preferRatio, max);
        }


        return preferRatio > 1 ? 1 : preferRatio;

    }


    public static float getMaxPossibleRatio(Context context, int inputWidth, int inputHeight) {
        if (AppUtils.isTv(context))return 1f;
        float xRatio = Face106Input.x * 1.0f / Math.min(inputWidth, inputHeight);
        float yRatio = Face106Input.y * 1.0f / Math.max(inputWidth, inputHeight);
        float maxPossibleRatio = Math.max(xRatio, yRatio);


        float max = Math.max(xRatio, yRatio);
        maxPossibleRatio = Math.max(maxPossibleRatio, max);

        xRatio = Face280Input.x * 1.0f / Math.min(inputWidth, inputHeight);
        yRatio = Face280Input.y * 1.0f / Math.max(inputWidth, inputHeight);
        max = Math.max(xRatio, yRatio);
        maxPossibleRatio = Math.max(maxPossibleRatio, max);


        xRatio = FaceAttriInput.x * 1.0f / Math.min(inputWidth, inputHeight);
        yRatio = FaceAttriInput.y * 1.0f / Math.max(inputWidth, inputHeight);
        max = Math.max(xRatio, yRatio);
        maxPossibleRatio = Math.max(maxPossibleRatio, max);

        xRatio = HandInput.x * 1.0f / Math.min(inputWidth, inputHeight);
        yRatio = HandInput.y * 1.0f / Math.max(inputWidth, inputHeight);
        max = Math.max(xRatio, yRatio);
        maxPossibleRatio = Math.max(maxPossibleRatio, max);

        xRatio = SkeletonInput.x * 1.0f / Math.min(inputWidth, inputHeight);
        yRatio = SkeletonInput.y * 1.0f / Math.max(inputWidth, inputHeight);


        max = Math.max(xRatio, yRatio);
        maxPossibleRatio = Math.max(maxPossibleRatio, max);


        xRatio = MattingInput.x * 1.0f / Math.min(inputWidth, inputHeight);
        yRatio = MattingInput.y * 1.0f / Math.max(inputWidth, inputHeight);
        max = Math.max(xRatio, yRatio);
        maxPossibleRatio = Math.max(maxPossibleRatio, max);


        xRatio = HairCutInput.x * 1.0f / Math.min(inputWidth, inputHeight);
        yRatio = HairCutInput.y * 1.0f / Math.max(inputWidth, inputHeight);
        max = Math.max(xRatio, yRatio);
        maxPossibleRatio = Math.max(maxPossibleRatio, max);


        xRatio = FaceVerifyInput.x * 1.0f / Math.min(inputWidth, inputHeight);
        yRatio = FaceVerifyInput.y * 1.0f / Math.max(inputWidth, inputHeight);
        max = Math.max(xRatio, yRatio);
        maxPossibleRatio = Math.max(maxPossibleRatio, max);


        xRatio = QrdecodeInput.x * 1.0f / Math.min(inputWidth, inputHeight);
        yRatio = QrdecodeInput.y * 1.0f / Math.max(inputWidth, inputHeight);
        max = Math.max(xRatio, yRatio);
        maxPossibleRatio = Math.max(maxPossibleRatio, max);
        return maxPossibleRatio > 1 ? 1 : maxPossibleRatio;

    }
}

