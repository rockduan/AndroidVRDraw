package com.dlodlo.admin.dlodlovrdraw;

import android.graphics.SurfaceTexture;
import android.graphics.drawable.ScaleDrawable;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.hardware.Camera;
import com.dlodlo.admin.dlodlovrdraw.gles.EglCore;
import com.dlodlo.admin.dlodlovrdraw.gles.GlUtil;
import com.dlodlo.admin.dlodlovrdraw.gles.Sprite2d;
import com.dlodlo.admin.dlodlovrdraw.gles.Texture2dProgram;
import com.dlodlo.admin.dlodlovrdraw.gles.WindowSurface;

import java.io.IOException;

import static com.dlodlo.admin.dlodlovrdraw.MainActivity.TAG;

/**
 * Created by duanliang on 4/7/2017.
 */

public class RenderThread extends Thread implements SurfaceTexture.OnFrameAvailableListener{
    private static final int DEFAULT_ZOOM_PERCENT = 0;      // 0-100
    private static final int DEFAULT_SIZE_PERCENT = 50;     // 0-100
    private static final int DEFAULT_ROTATE_PERCENT = 0;    // 0-100
    // Requested values; actual may differ.
    private static final int REQ_CAMERA_WIDTH = 1280;
    private static final int REQ_CAMERA_HEIGHT = 720;
    private static final int REQ_CAMERA_FPS = 30;

    private volatile  RenderHandler mHandler;
    private Object mStartLock = new Object();
    private boolean mReady = false;
    private MainHandler mMainHandler;
    private Camera mCamera;
    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;
    private EglCore mEglCore;
    private WindowSurface mWindowSurface;
    private int mWindowSurfaceWidth;
    private int mWindowSurfaceHeight;

    private SurfaceTexture mCameraTexture;
    private SurfaceTexture mDrawTexture_dlodlo;
    private float[] mDisplayProjectionMatrix = new float[16];

    private Texture2dProgram mTexProgram;
    private final ScaledDrawable2d mRectDrawable =
            new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
    private final Sprite2d mRect = new Sprite2d(mRectDrawable);

    private int mZoomPercent = DEFAULT_ZOOM_PERCENT;
    private int mSizePercent = DEFAULT_SIZE_PERCENT;
    private int mRotatePercent = DEFAULT_ROTATE_PERCENT;
    private float mPosX, mPosY;

    private int mTextureId;
    public RenderThread(MainHandler handler){mMainHandler = handler;};

    @Override
    public void run() {
        super.run();
        Log.d(TAG, "run begin");
        Looper.prepare();
        Log.d(TAG, "Looper.prepare");
        mHandler = new RenderHandler(this);
        synchronized (mStartLock){
            mReady = true;
            mStartLock.notify();
        }
        mEglCore = new EglCore(null,0);
        openCamera(REQ_CAMERA_WIDTH, REQ_CAMERA_HEIGHT, REQ_CAMERA_FPS);
        Log.d(TAG, "before loop");
        Looper.loop();

        Log.d(TAG, "looper quit");
        releaseCamera();
        releaseGl();
        mEglCore.release();

        synchronized (mStartLock) {
            mReady = false;
        }
    }

    public void waitUntilReady(){
        synchronized (mStartLock){
            while (!mReady){
                try{
                    mStartLock.wait();
                }catch (InterruptedException ie){

                }
            }
        }
    }

    /**
     * Returns the render thread's Handler.  This may be called from any thread.
     */
    public RenderHandler getHandler() {
        return mHandler;
    }

    public void shutdown(){
        Log.d(TAG,"RenderThread.shutdown");
        Looper.myLooper().quit();
    }

    public void surfaceAvailable(SurfaceHolder holder, boolean newSurface){
        Log.d(TAG,"RenderThread.surfaceAvaliable(holder="+holder+"newSurface="+newSurface);
        Surface surface = holder.getSurface();
        mWindowSurface = new WindowSurface(mEglCore,surface,false);
        mWindowSurface.makeCurrent();

        mTexProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
        int textureId = mTexProgram.createTextureObject();

        //int textureId_dlodlo = mTexProgram.createTextureObject();
        mCameraTexture = new SurfaceTexture(textureId);
        Log.d(TAG,"RenderThread.surfaceAvaliable,textureId="+textureId+"mTextureId="+mTextureId);
        //mDrawTexture_dlodlo = new SurfaceTexture(mTextureId);
        mRect.setTexture(textureId);
        //mRect.setTexture(mTextureId);
        if(!newSurface){
            mWindowSurfaceWidth = mWindowSurface.getWidth();
            mWindowSurfaceHeight = mWindowSurface.getHeight();
            finishSurfaceSetup();
        }

        mCameraTexture.setOnFrameAvailableListener(this);
    }

    public void releaseGl(){
        Log.d(TAG,"RenderThread.releaseGl");
        GlUtil.checkGlError("releaseGl start");
        if(mWindowSurface != null){
            mWindowSurface.release();
            mWindowSurface = null;
        }
        if(mTexProgram != null){
            mTexProgram.release();
            mTexProgram = null;
        }
        GlUtil.checkGlError("releaseGl done");
        mEglCore.makeNothingCurrent();
    }

    public void surfaceChanged(int width,int height){
        Log.d(TAG, "RenderThread.surfaceChanged " + width + "x" + height);
        mWindowSurfaceWidth = width;
        mWindowSurfaceHeight = height;
        finishSurfaceSetup();
    }

    private void finishSurfaceSetup() {
        Log.d(TAG,"RenderThread.finishSurfaceSetup");
        int width = mWindowSurfaceWidth;
        int height = mWindowSurfaceHeight;
        Log.d(TAG, "finishSurfaceSetup size=" + width + "x" + height +
                " camera=" + mCameraPreviewWidth + "x" + mCameraPreviewHeight);

        // Use full window.
        GLES20.glViewport(200,200, width, height);

        // Simple orthographic projection, with (0,0) in lower-left corner.
        Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, width, 0, height, -1, 1);

        // Default position is center of screen.
        mPosX = width / 2.0f;
        mPosY = height / 2.0f;

        updateGeometry();

        // Ready to go, start the camera.
        Log.d(TAG, "starting camera preview");
        try {
            mCamera.setPreviewTexture(mCameraTexture);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

    private void updateGeometry() {
        Log.d(TAG,"RenderThread.updateGeometry");
        int width = mWindowSurfaceWidth;
        int height = mWindowSurfaceHeight;

        int smallDim = Math.min(width, height);
        // Max scale is a bit larger than the screen, so we can show over-size.
        float scaled = smallDim * (mSizePercent / 100.0f) * 1.25f;
        float cameraAspect = (float) mCameraPreviewWidth / mCameraPreviewHeight;
        int newWidth = Math.round(scaled * cameraAspect);
        int newHeight = Math.round(scaled);

        float zoomFactor = 1.0f - (mZoomPercent / 100.0f);
        int rotAngle = Math.round(360 * (mRotatePercent / 100.0f));

        mRect.setScale(newWidth, newHeight);
        mRect.setPosition(mPosX, mPosY);
        mRect.setRotation(rotAngle);
        mRectDrawable.setScale(zoomFactor);

        mMainHandler.sendRectSize(newWidth, newHeight);
        mMainHandler.sendZoomArea(Math.round(mCameraPreviewWidth * zoomFactor),
                Math.round(mCameraPreviewHeight * zoomFactor));
        mMainHandler.sendRotateDeg(rotAngle);
    }

    public void surfaceDestroyed(){
        Log.d(TAG, "RenderThread.surfaceDestroyed");
        releaseGl();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mHandler.sendFrameAvailable();
    }

    public void frameAvailable() {
        //Log.d(TAG,System.currentTimeMillis()+",RenderThread.frameAvailable begin");
        mCameraTexture.updateTexImage();
        draw();
    }

    public void draw() {
        GlUtil.checkGlError("draw start");

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mRect.draw(mTexProgram, mDisplayProjectionMatrix);
        mWindowSurface.swapBuffers();

        GlUtil.checkGlError("draw done");
    }

    public void setZoom(int percent) {
        mZoomPercent = percent;
        updateGeometry();
    }

    public void setSize(int percent) {
        mSizePercent = percent;
        updateGeometry();
    }

    public void setRotate(int percent) {
        mRotatePercent = percent;
        updateGeometry();
    }

    public void setPosition(int x, int y) {
        mPosX = x;
        mPosY = mWindowSurfaceHeight - y;   // GLES is upside-down
        updateGeometry();
    }
    public void setTextureId(int textureId){
        mTextureId = textureId;
        Log.d(TAG,"RenderThread.setTextureId = "+mTextureId);
    }
    public void openCamera(int desiredWidth, int desiredHeight, int desiredFps) {
        Log.d(TAG,"RenderThread.openCamera");
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = android.hardware.Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = android.hardware.Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

        // Try to set the frame rate to a constant value.
        int thousandFps = CameraUtils.chooseFixedPreviewFps(parms, desiredFps * 1000);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);

        mCamera.setParameters(parms);

        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);
        String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
        if (fpsRange[0] == fpsRange[1]) {
            previewFacts += " @" + (fpsRange[0] / 1000.0) + "fps";
        } else {
            previewFacts += " @[" + (fpsRange[0] / 1000.0) +
                    " - " + (fpsRange[1] / 1000.0) + "] fps";
        }
        Log.i(TAG, "Camera config: " + previewFacts);

        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;
        mMainHandler.sendCameraParams(mCameraPreviewWidth, mCameraPreviewHeight,
                thousandFps / 1000.0f);
    }
    public void releaseCamera() {
        Log.d(TAG,"RenderThread.releaseCamera");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }
}
