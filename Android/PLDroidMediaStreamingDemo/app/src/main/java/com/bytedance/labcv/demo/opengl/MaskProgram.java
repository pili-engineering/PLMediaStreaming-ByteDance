package com.bytedance.labcv.demo.opengl;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * draw color outer mask
 */
public class MaskProgram extends ShaderProgram {


    private  int mPositionLocation;
    private  int mCoordinatLocation;
    private  int mColorLocation;
    private  int mMaskTextureLocaltion;

    private int[] mCachedTextureid=  new int[]{ ShaderHelper.NO_TEXTURE };

    // rgba
    public MaskProgram(Context context, int width, int height,String fragmentShader) {
        super(context, CAMERA_INPUT_VERTEX_SHADER,fragmentShader, width , height);

        mPositionLocation = GLES20.glGetAttribLocation(mProgram, "position");
        mCoordinatLocation = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        mMaskTextureLocaltion = GLES20.glGetUniformLocation(mProgram, "inputMaskTexture");
        mColorLocation = GLES20.glGetUniformLocation(mProgram, "maskColor");
    }


    public void drawMask(byte[] bytes, int width, int height,
                         FloatBuffer vertexBuffer, FloatBuffer textureBuffer, int frameBuffer, int origintextureid, float[] mMaskColor) {
        useProgram();
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionLocation);

        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mCoordinatLocation, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mCoordinatLocation);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc( GLES20.GL_SRC_ALPHA , GLES20.GL_ONE_MINUS_SRC_ALPHA);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.position(0);
        if (mCachedTextureid[0] == ShaderHelper.NO_TEXTURE) {
            GLES20.glGenTextures(1, mCachedTextureid, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mCachedTextureid[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA, width, height, 0,
                GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
        }
        else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mCachedTextureid[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA, width, height, 0,
                GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
//            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width, height,
//                GLES20.GL_SRC_ALPHA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mCachedTextureid[0]);
        GLES20.glUniform1i(mMaskTextureLocaltion, 0);
        GLES20.glUniform4f(mColorLocation, mMaskColor[0], mMaskColor[1], mMaskColor[2], mMaskColor[3]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, origintextureid, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);


      GLES20.glDisableVertexAttribArray(mPositionLocation);
        GLES20.glDisableVertexAttribArray(mCoordinatLocation);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
    }


    public void release() {
        super.release();
        if (mCachedTextureid[0] != ShaderHelper.NO_TEXTURE)
        {
            GLES20.glDeleteTextures(1, mCachedTextureid, 0);
        }
    }

    private static final String CAMERA_INPUT_VERTEX_SHADER = "" +
        "attribute vec4 position;\n" +
        "attribute vec4 inputTextureCoordinate;\n" +
        "\n" +
        "varying vec2 textureCoordinate;\n" +
        "\n" +
        "void main()\n" +
        "{\n" +
        "	textureCoordinate = vec2(inputTextureCoordinate.x, 1.0 -inputTextureCoordinate.y);\n" +
        "	gl_Position = position;\n" +
        "}";


    protected static final String FRAGMENT_PORTRAIT =
         "precision mediump float;\n" +
         "varying highp vec2 textureCoordinate;\n" +
         " \n" +
         "uniform sampler2D inputMaskTexture;\n" +
         "uniform vec4 maskColor;\n" +
         " \n" +
         "void main()\n" +
         "{\n" +
         "     float maska = texture2D(inputMaskTexture, textureCoordinate).a;\n" +
         "     gl_FragColor = vec4(maskColor.rgb , 1.0-maska);\n" +
         "}";
    protected static final String FRAGMENT_HAIR =
         "precision mediump float;\n" +
         "varying highp vec2 textureCoordinate;\n" +
         " \n" +
         "uniform sampler2D inputMaskTexture;\n" +
         "uniform vec4 maskColor;\n" +
         " \n" +
         "void main()\n" +
         "{\n" +
         "     float maska = texture2D(inputMaskTexture, textureCoordinate).a;\n" +
         "     gl_FragColor = vec4(maskColor.rgb ,  maska * maskColor.a);\n" +
         "}";
}
