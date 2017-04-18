package com.dlodlo.admin.dlodlovrdraw;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dlodlo.admin.dlodlovrdraw.glUtils.MyRenderer;

/**
 * Created by duanliang on 4/7/2017.
 */

public class TextureFromCameraActivity extends Activity implements SurfaceHolder.Callback, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = MainActivity.TAG;
    private static final int DEFAULT_ZOOM_PERCENT = 0;
    private static final int DEFAULT_SIZE_PERCENT = 0;
    private static final int DEFAULT_ROTATE_PERCENT = 0;

    // Requested values; actual may differ.
    private static final int REQ_CAMERA_WIDTH = 1280;
    private static final int REQ_CAMERA_HEIGHT = 720;
    private static final int REQ_CAMERA_FPS = 30;

    private static SurfaceHolder sSurfaceHolder;
    private RenderThread mRenderThread;
    private MainHandler mHandler;
    // User controls.
    private SeekBar mZoomBar;
    private SeekBar mSizeBar;
    private SeekBar mRotateBar;
    // These values are passed to us by the camera/render thread, and displayed in the UI.
    // We could also just peek at the values in the RenderThread object, but we'd need to
    // synchronize access carefully.
    public int mCameraPreviewWidth, mCameraPreviewHeight;
    public float mCameraPreviewFps;
    public int mRectWidth, mRectHeight;
    public int mZoomWidth, mZoomHeight;
    public int mRotateDeg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"TextureFromCameraActivity.onCreate begin");

       // setContentView(R.layout.activity_texture_from_camera);
        mHandler = new MainHandler(this);

        SurfaceView sv = (SurfaceView)findViewById(R.id.cameraOnTexture_surfaceView);
        SurfaceHolder sh = sv.getHolder();
        sh.addCallback(this);

        mZoomBar = (SeekBar) findViewById(R.id.tfcZoom_seekbar);
        mSizeBar = (SeekBar) findViewById(R.id.tfcSize_seekbar);
        mRotateBar = (SeekBar) findViewById(R.id.tfcRotate_seekbar);
        mZoomBar.setProgress(DEFAULT_ZOOM_PERCENT);
        mSizeBar.setProgress(DEFAULT_SIZE_PERCENT);
        mRotateBar.setProgress(DEFAULT_ROTATE_PERCENT);
        mZoomBar.setOnSeekBarChangeListener(this);
        mSizeBar.setOnSeekBarChangeListener(this);
        mRotateBar.setOnSeekBarChangeListener(this);

        updateControls();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"Activity.onResume");
        mRenderThread = new RenderThread(mHandler);
        mRenderThread.setName("TexFromCam Render");
        mRenderThread.start();
        mRenderThread.waitUntilReady();

        RenderHandler rh = mRenderThread.getHandler();
        rh.sendZoomValue(mZoomBar.getProgress());
        rh.sendSizeValue(mSizeBar.getProgress());
        rh.sendRotateValue(mRotateBar.getProgress());

        if (sSurfaceHolder != null) {
            Log.d(TAG, "Sending previous surface");
            rh.sendSurfaceAvailable(sSurfaceHolder, false);
        } else {
            Log.d(TAG, "No previous surface");
        }
        Log.d(TAG, "onResume END");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"Activity.onPause");
        RenderHandler rh = mRenderThread.getHandler();
        rh.sendShutdown();

        try {
            mRenderThread.join();
        } catch (InterruptedException ie) {
            // not expected
            throw new RuntimeException("join was interrupted", ie);
        }
        mRenderThread = null;
        Log.d(TAG, "onPause END");
    }

    public void updateControls(){
        String str = getString(R.string.tfcCameraParams, mCameraPreviewWidth,
                mCameraPreviewHeight, mCameraPreviewFps);
        TextView tv = (TextView)findViewById(R.id.tfcCameraParams_text);
        tv.setText(str);

        str = getString(R.string.tfcRectSize, mRectWidth, mRectHeight);
        tv = (TextView) findViewById(R.id.tfcRectSize_text);
        tv.setText(str);

        str = getString(R.string.tfcZoomArea, mZoomWidth, mZoomHeight);
        tv = (TextView) findViewById(R.id.tfcZoomArea_text);
        tv.setText(str);
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Activity.surfaceCreated holder=" + holder + " (static=" + sSurfaceHolder + ")");
        if (sSurfaceHolder != null) {
            throw new RuntimeException("sSurfaceHolder is already set");
        }

        sSurfaceHolder = holder;

        if (mRenderThread != null) {
            // Normal case -- render thread is running, tell it about the new surface.
            RenderHandler rh = mRenderThread.getHandler();
            rh.sendSurfaceAvailable(holder, true);
        } else {
            // Sometimes see this on 4.4.x N5: power off, power on, unlock, with device in
            // landscape and a lock screen that requires portrait.  The surface-created
            // message is showing up after onPause().
            //
            // Chances are good that the surface will be destroyed before the activity is
            // unpaused, but we track it anyway.  If the activity is un-paused and we start
            // the RenderThread, the SurfaceHolder will be passed in right after the thread
            // is created.
            Log.d(TAG, "render thread not running");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Activity.surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                " holder=" + holder);

        if (mRenderThread != null) {
            RenderHandler rh = mRenderThread.getHandler();
            rh.sendSurfaceChanged(format, width, height);
        } else {
            Log.d(TAG, "Ignoring surfaceChanged");
            return;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // In theory we should tell the RenderThread that the surface has been destroyed.
        if (mRenderThread != null) {
            RenderHandler rh = mRenderThread.getHandler();
            rh.sendSurfaceDestroyed();
        }
        Log.d(TAG, "surfaceDestroyed holder=" + holder);
        sSurfaceHolder = null;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mRenderThread == null) {
            // Could happen if we programmatically update the values after setting a listener
            // but before starting the thread.  Also, easy to cause this by scrubbing the seek
            // bar with one finger then tapping "recents" with another.
            Log.w(TAG, "Ignoring onProgressChanged received w/o RT running");
            return;
        }
        RenderHandler rh = mRenderThread.getHandler();

        // "progress" ranges from 0 to 100
        if (seekBar == mZoomBar) {
            //Log.v(TAG, "zoom: " + progress);
            rh.sendZoomValue(progress);
        } else if (seekBar == mSizeBar) {
            //Log.v(TAG, "size: " + progress);
            rh.sendSizeValue(progress);
        } else if (seekBar == mRotateBar) {
            //Log.v(TAG, "rotate: " + progress);
            rh.sendRotateValue(progress);
        } else {
            throw new RuntimeException("unknown seek bar");
        }

        // If we're getting preview frames quickly enough we don't really need this, but
        // we don't want to have chunky-looking resize movement if the camera is slow.
        // OTOH, if we get the updates too quickly (60fps camera?), this could jam us
        // up and cause us to run behind.  So use with caution.
        rh.sendRedraw();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
                //Log.v(TAG, "onTouchEvent act=" + e.getAction() + " x=" + x + " y=" + y);
                if (mRenderThread != null) {
                    RenderHandler rh = mRenderThread.getHandler();
                    rh.sendPosition((int) x, (int) y);

                    // Forcing a redraw can cause sluggish-looking behavior if the touch
                    // events arrive quickly.
                    //rh.sendRedraw();
                }
                break;
            default:
                break;
        }
        return true;
    }
}
