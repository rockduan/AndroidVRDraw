package com.dlodlo.admin.dlodlovrdraw.glUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.dlodlo.admin.dlodlovrdraw.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.dlodlo.admin.dlodlovrdraw.MainActivity.TAG;

/**
 * Created by duanliang on 4/12/2017.
 */

public class MyRenderer implements GLSurfaceView.Renderer {
    private Bitmap bmp;
    private int mProgram;
    private int mTexSamplerHandle;
    private int mTexCoordHandle;
    private int mPosCoordHandle;
    private FloatBuffer mTexVertices;
    private FloatBuffer mPosVertices;
    private int[] mTextures = new int[2];
    private int[] mTextures2 = new int[2];
    //着色器程序
    private String VERTEX_SHADER_CODE =
            "attribute vec4 a_position;\n" +
                    "attribute vec2 a_texcoord;\n" +
                    "varying vec2 v_texcoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = a_position;\n" +
                    "  v_texcoord = a_texcoord;\n" +
                    "}\n";
    private String FRAGMENT_SHADER_CODE =
            "precision mediump float;\n" +
                    "uniform sampler2D tex_sampler;\n" +
                    "varying vec2 v_texcoord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(tex_sampler, v_texcoord);\n" +
                    "}\n";
    public int[] getmTextures() {
        return mTextures;
    }

    public void setmTextures(int[] mTextures) {
        this.mTextures = mTextures;
    }

    public MyRenderer(Context context) {
        bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.desktop);
    }

    @Override
    public void onDrawFrame(GL10 arg0) {
        Log.d(TAG,"onDrawFrame begin");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        GLShaderToolbox.checkGlError("glUseProgram");

        //将纹理坐标传递给着色器程序并使能属性数组
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexVertices);
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLShaderToolbox.checkGlError("vertex attribute setup");

        //将顶点坐标传递给着色器程序并使能属性数组
        GLES20.glVertexAttribPointer(mPosCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mPosVertices);
        GLES20.glEnableVertexAttribArray(mPosCoordHandle);
        GLShaderToolbox.checkGlError("vertex attribute setup");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLShaderToolbox.checkGlError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLShaderToolbox.checkGlError("glBindTexture");
        GLES20.glUniform1i(mTexSamplerHandle, 0);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

    }

    @Override
    public void onSurfaceChanged(GL10 arg0, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        GLShaderToolbox.checkGlError("glViewport");
        //调整AspectRatio 保证landscape和portrait的时候显示比例相同，图片不会被拉伸
        if (mPosVertices != null) {
            float imgAspectRatio = bmp.getWidth() / (float)bmp.getHeight();
            float viewAspectRatio = w / (float)h;
            float relativeAspectRatio = viewAspectRatio / imgAspectRatio;
            float x0, y0, x1, y1;
            if (relativeAspectRatio > 1.0f) {
                x0 = -1.0f / relativeAspectRatio;
                y0 = -1.0f;
                x1 = 1.0f / relativeAspectRatio;
                y1 = 1.0f;
            } else {
                x0 = -1.0f;
                y0 = -relativeAspectRatio;
                x1 = 1.0f;
                y1 = relativeAspectRatio;
            }
            float[] coords = new float[] { x0, y0, x1, y0, x0, y1, x1, y1 };
            mPosVertices.put(coords).position(0);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 g, EGLConfig eglConfig) {
        //纹理坐标屏幕右上角为原点(左下，右下，左上，右上)
        float[] TEX_VERTICES = { 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f };
        mTexVertices = ByteBuffer.allocateDirect(TEX_VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexVertices.put(TEX_VERTICES).position(0);
        //顶点坐标屏幕中心点为原点(左下，右下，左上，右上)
        float[] POS_VERTICES = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f };
        mPosVertices = ByteBuffer.allocateDirect(POS_VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mPosVertices.put(POS_VERTICES).position(0);
        mProgram = GLShaderToolbox.createProgram(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE);
        mTexSamplerHandle = GLES20.glGetUniformLocation(mProgram,"tex_sampler");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texcoord");
        mPosCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_position");
        //创建纹理并将图片贴入纹理
        GLES20.glGenTextures(2, mTextures , 0);
        GLES20.glGenTextures(2, mTextures2 , 0);
        Log.d(TAG,"MyRender.TextureId="+mTextures[0]+"mTextures2="+mTextures2[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        GLShaderToolbox.initTextureNeedParams();
    }

}
