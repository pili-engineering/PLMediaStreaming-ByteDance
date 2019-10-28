// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.bytedance.labcv.demo.opengl;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class PointProgram extends ShaderProgram {

    private static final int POSITION_COUNT = 2;
    private static final int STRIDE = POSITION_COUNT * 4;

    private final int mPositionLocation;
    private final int mColorLocation;
    private final int mPointSizeLocation;

    private final FloatBuffer mFloatBuffer;

    private final float[] mPositions = new float[2];

    public PointProgram(Context context, int width, int height) {
        super(context, VERTEX, FRAGMENT, width, height);

        mPositionLocation = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mColorLocation = GLES20.glGetUniformLocation(mProgram, "u_Color");
        mPointSizeLocation = GLES20.glGetUniformLocation(mProgram, "uPointSize");

        mFloatBuffer = ByteBuffer
                .allocateDirect(STRIDE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    public void draw(PointF point, int color, float pointSize) {
        useProgram();

        mPositions[0] = transformX(point.x);
        mPositions[1] = transformY(point.y);

        mFloatBuffer.position(0);
        mFloatBuffer.put(mPositions);

        float r = Color.red(color) / 255f;
        float g = Color.green(color) / 255f;
        float b = Color.blue(color) / 255f;
        float a = Color.alpha(color) / 255f;
        GLES20.glUniform4f(mColorLocation, r, g, b, a);

        mFloatBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionLocation, 2, GLES20.GL_FLOAT, false, 0, mFloatBuffer);
        GLES20.glEnableVertexAttribArray(mPositionLocation);

        GLES20.glUniform1f(mPointSizeLocation, pointSize);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }

    private static final String VERTEX = "attribute vec4 a_Position;\n" +
            "uniform float uPointSize;\n" +
            "void main() {\n" +
            "    gl_Position = a_Position;\n" +
            "    gl_PointSize = uPointSize;\n" +
            "}";

    private static final String FRAGMENT = "precision mediump float;\n" +
            "\n" +
            "uniform vec4 u_Color;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = u_Color;\n" +
            "}";
}
