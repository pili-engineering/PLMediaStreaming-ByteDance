package com.bytedance.labcv.demo.opengl;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class LineProgram extends ShaderProgram {

    private static final int COORDS_PER_VERTEX = 2;
    private static final int STRIDE = COORDS_PER_VERTEX * 4;
    private static final int POSITION_COUNT = 2;

    private final int mPositionLocation;
    private final int mColorLocation;

    // 存储矩形顶点数据，从 drawRect() 方法中分离出啦，减少重复的初始化
    private FloatBuffer mRectFb;
    private float[] mRectPoints;

    public LineProgram(Context context, int width, int height) {
        super(context, VERTEX, FRAGMENT, width, height);

        mPositionLocation = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mColorLocation = GLES20.glGetUniformLocation(mProgram, "u_Color");

        mRectFb = ByteBuffer.allocateDirect(STRIDE * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mRectPoints = new float[STRIDE];
    }



    public void drawRect(RectF rect, int color, float lineWidth) {
        useProgram();

        float x1s = transformX(rect.left);
        float y1s = transformY(rect.top);
        float x2s = transformX(rect.right);
        float y2s = transformY(rect.bottom);
        int index = 0;
        mRectPoints[index++] = x1s;
        mRectPoints[index++] = y1s;

        mRectPoints[index++] = x1s;
        mRectPoints[index++] = y2s;

        mRectPoints[index++] = x2s;
        mRectPoints[index++] = y2s;

        mRectPoints[index++] = x2s;
        mRectPoints[index++] = y1s;

        mRectFb.position(0);
        mRectFb.put(mRectPoints);

        mRectFb.position(0);
        GLES20.glVertexAttribPointer(mPositionLocation, POSITION_COUNT, GLES20.GL_FLOAT,
            false, STRIDE, mRectFb);
        GLES20.glEnableVertexAttribArray(mPositionLocation);

        float r = Color.red(color) / 255f;
        float g = Color.green(color) / 255f;
        float b = Color.blue(color) / 255f;
        float a = Color.alpha(color) / 255f;
        GLES20.glUniform4f(mColorLocation, r, g, b, a);

        GLES20.glLineWidth(lineWidth);
        // 将 type 设置为 GL_LINE_LOOP 时只是需要指定四个顶点，系统会自动构成回路
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 4);

        GLES20.glDisableVertexAttribArray(mPositionLocation);
    }

    public void drawLineStrip(PointF points[], int color, float linewidth) {
        drawLine(points, color, linewidth, true);
    }

    public void drawLines(PointF points[], int color, float linewidth) {
        drawLine(points, color, linewidth, false);
    }

    // drawLinesTrip() 和 drawLines() 两个方法仅绘制类型不同，可以将逻辑统一到本方法里
    private void drawLine(PointF[] points, int color, float lineWidth, boolean strip) {
        useProgram();

        float[] pointpPosis = new float[points.length * POSITION_COUNT];
        FloatBuffer pointsBuffer = ByteBuffer
                .allocateDirect(STRIDE * points.length)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        for(int i = 0; i < points.length; i ++)
        {
            pointpPosis[i*2] = transformX(points[i].x);
            pointpPosis[i*2+1] = transformY(points[i].y);
        }

        pointsBuffer.position(0);
        pointsBuffer.put(pointpPosis);

        pointsBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionLocation, POSITION_COUNT, GLES20.GL_FLOAT,
                false, STRIDE, pointsBuffer);
        GLES20.glEnableVertexAttribArray(mPositionLocation);

        float r = Color.red(color) / 255f;
        float g = Color.green(color) / 255f;
        float b = Color.blue(color) / 255f;
        float a = Color.alpha(color) / 255f;
        GLES20.glUniform4f(mColorLocation, r, g, b, a);

        GLES20.glLineWidth(lineWidth);
        GLES20.glDrawArrays(strip ? GLES20.GL_LINE_STRIP : GLES20.GL_LINES, 0, points.length);

        GLES20.glDisableVertexAttribArray(mPositionLocation);
    }

    private static final String VERTEX = "attribute vec4 a_Position;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = a_Position;\n" +
            "}";

    private static final String FRAGMENT = "precision mediump float;\n" +
            "\n" +
            "uniform vec4 u_Color;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = u_Color;\n" +
            "}";

}
