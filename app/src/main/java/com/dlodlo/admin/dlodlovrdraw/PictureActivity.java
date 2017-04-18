package com.dlodlo.admin.dlodlovrdraw;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.dlodlo.admin.dlodlovrdraw.glUtils.MyRenderer;

/**
 * Created by duanliang on 4/17/2017.
 */

public class PictureActivity extends Activity{
    private GLSurfaceView mGLView;

    private void prepareTexture(){
        mGLView = new GLSurfaceView(this);
        mGLView.setEGLContextClientVersion(2);
        myRenderer = new MyRenderer(this);
        mGLView.setRenderer(myRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    }


    private MyRenderer myRenderer;


    public MyRenderer getMyRenderer() {
        return myRenderer;
    }

    public void setMyRenderer(MyRenderer myRenderer) {
        this.myRenderer = myRenderer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //show mGlView
        prepareTexture();
        setContentView(mGLView);
    }
}
