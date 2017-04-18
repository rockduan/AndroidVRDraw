package com.dlodlo.admin.dlodlovrdraw;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Trace;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.dlodlo.admin.dlodlovrdraw.gles.EglCore;
import com.dlodlo.admin.dlodlovrdraw.gles.WindowSurface;

/**
 * Created by admin on 2017/4/1.
 */

public class MultiSurfaceActivity extends Activity implements SurfaceHolder.Callback{
    private static final String TAG = MainActivity.TAG;
    private static final int BOUNCE_STEPS = 30;
    private SurfaceView mSurfaceView1;
    private SurfaceView mSurfaceView2;
    private SurfaceView mSurfaceView3;

    private volatile boolean mBouncing;

    private Thread mBounceThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_surface_test);
        mSurfaceView1 = (SurfaceView) findViewById(R.id.multiSurfaceView1);
        mSurfaceView1.getHolder().addCallback(this);
        mSurfaceView1.setSecure(true);
        mSurfaceView2 = (SurfaceView) findViewById(R.id.multiSurfaceView2);
        mSurfaceView2.getHolder().addCallback(this);
        mSurfaceView2.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceView2.setZOrderMediaOverlay(true);
        mSurfaceView3 = (SurfaceView)findViewById(R.id.multiSurfaceView3);
        mSurfaceView3.getHolder().addCallback(this);
        mSurfaceView3.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceView3.setZOrderOnTop(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mBounceThread != null){
            stopBouncing();
        }
    }
    public void clickBounce(){
        Log.d(TAG,"clickBounce bouncing="+mBouncing);
        if(mBounceThread != null)
        {
            stopBouncing();
        }else{
            startBouncing();
        }
    }
    private void startBouncing(){
        final Surface surface = mSurfaceView2.getHolder().getSurface();
        if(surface == null || !surface.isValid())
        {
            Log.w(TAG,"mSurfaceView2 is not ready");
            return ;
        }
        mBounceThread = new Thread(){
            @Override
            public void run() {
                super.run();
                while(true){
                    long startWhen = System.nanoTime();
                    for (int i = 0; i < BOUNCE_STEPS; i++) {
                        if (!mBouncing) return;
                        drawBouncingCircle(surface, i);
                    }
                    for (int i = BOUNCE_STEPS; i > 0; i--) {
                        if (!mBouncing) return;
                        drawBouncingCircle(surface, i);
                    }
                    long duration  = System.nanoTime() - startWhen;
                    double framePerSec = 1000000000.0 /(duration / (BOUNCE_STEPS*2.0));
                    Log.d(TAG,"Bouncing at"+framePerSec+"fps");
                }
            }
        };
        mBouncing = true;
        mBounceThread.setName("Bouncer");
        mBounceThread.start();
    }
    /**
     * Signals the bounce-thread to stop, and waits for it to do so.
     */
    private void stopBouncing() {
        Log.d(TAG, "Stopping bounce thread");
        mBouncing = false;      // tell thread to stop
        try {
            mBounceThread.join();
        } catch (InterruptedException ignored) {}
        mBounceThread = null;
    }

    /**
     * Returns an ordinal value for the SurfaceHolder, or -1 for an invalid surface.
     */
    private int getSurfaceId(SurfaceHolder holder) {
        if (holder.equals(mSurfaceView1.getHolder())) {
            return 1;
        } else if (holder.equals(mSurfaceView2.getHolder())) {
            return 2;
        } else if (holder.equals(mSurfaceView3.getHolder())) {
            return 3;
        } else {
            return -1;
        }
    }
    /**
     * Clears the surface, then draws a filled circle with a shadow.
     * <p>
     * The Canvas drawing we're doing may not be fully implemented for hardware-accelerated
     * renderers (shadow layers only supported for text).  However, Surface#lockCanvas()
     * currently only returns an unaccelerated Canvas, so it all comes out looking fine.
     */
    private void drawCircleSurface(Surface surface, int x, int y, int radius) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(radius / 4 + 1, 0, 0, Color.RED);

        Canvas canvas = surface.lockCanvas(null);
        try {
            Log.v(TAG, "drawCircleSurface: isHwAcc=" + canvas.isHardwareAccelerated());
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //canvas.drawCircle(x, y, radius, paint);
            Rect r = new Rect(600,800,1800,1800);
            canvas.drawRect(r, paint);
        } finally {
            surface.unlockCanvasAndPost(canvas);
        }
    }
    /**
     * Clears the surface, then draws some alpha-blended rectangles with GL.
     * <p>
     * Creates a temporary EGL context just for the duration of the call.
     */
    private void drawRectSurface(Surface surface, int left, int top, int width, int height) {
        EglCore eglCore = new EglCore();
        WindowSurface win = new WindowSurface(eglCore, surface, false);
        win.makeCurrent();
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        for (int i = 0; i < 4; i++) {
            int x, y, w, h;
            if (width < height) {
                // vertical
                w = width / 4;
                h = height;
                x = left + w * i;
                y = top;
            } else {
                // horizontal
                w = width;
                h = height / 4;
                x = left;
                y = top + h * i;
            }
            Log.d(TAG,"x="+x+"y="+y+"w="+w+"h="+h);
            GLES20.glScissor(x, y, w, h);
            switch (i) {
                case 0:     // 50% blue at 25% alpha, pre-multiplied
                    GLES20.glClearColor(1.0f, 1.0f, 0.125f, 1.0f);
                    break;
                case 1:     // 100% blue at 25% alpha, pre-multiplied
                    GLES20.glClearColor(0.0f, 1.0f, 1.0f, 1.0f);
                    break;
                case 2:     // 200% blue at 25% alpha, pre-multiplied (should get clipped)
                    GLES20.glClearColor(0.0f, 0.0f, 0.5f, 1.0f);
                    break;
                case 3:     // 100% white at 25% alpha, pre-multiplied
                    GLES20.glClearColor(0.25f, 0.25f, 0.25f, 1.0f);
                    break;
            }
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

        win.swapBuffers();
        win.release();
        eglCore.release();
    }

    /**
     * Clears the surface, then draws a filled circle with a shadow.
     * <p>
     * Similar to drawCircleSurface(), but the position changes based on the value of "i".
     */
    private void drawBouncingCircle(Surface surface, int i) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        Canvas canvas = surface.lockCanvas(null);
        try {
            Trace.beginSection("drawBouncingCircle");
            Trace.beginSection("drawColor");
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            Trace.endSection(); // drawColor

            int width = canvas.getWidth();
            int height = canvas.getHeight();
            int radius, x, y;
            if (width < height) {
                // portrait
                radius = width / 4;
                x = width / 4 + ((width / 2 * i) / BOUNCE_STEPS);
                y = height * 3 / 4;
            } else {
                // landscape
                radius = height / 4;
                x = width * 3 / 4;
                y = height / 4 + ((height / 2 * i) / BOUNCE_STEPS);
            }

            paint.setShadowLayer(radius / 4 + 1, 0, 0, Color.RED);

            canvas.drawCircle(x, y, radius, paint);
            Trace.endSection(); // drawBouncingCircle
        } finally {
            surface.unlockCanvasAndPost(canvas);
        }
    }
    /**
     * onClick handler for "bounce" button.
     */
    public void clickBounce(@SuppressWarnings("unused") View unused) {
        Log.d(TAG, "clickBounce bouncing=" + mBouncing);
        if (mBounceThread != null) {
            stopBouncing();
        } else {
            startBouncing();
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
   int id = getSurfaceId(holder);
        if (id < 0) {
            Log.w(TAG, "surfaceCreated UNKNOWN holder=" + holder);
        } else {
            Log.d(TAG, "surfaceCreated #" + id + " holder=" + holder);

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                " holder=" + holder);

        int id = getSurfaceId(holder);
        boolean portrait = height > width;
        Surface surface = holder.getSurface();

        switch (id) {
            case 1:
                // default layer: circle on left / top
                if (portrait) {
                    drawCircleSurface(surface, width / 2, height / 4, width / 4);
                } else {
                    drawCircleSurface(surface, width / 4, height / 2, height / 4);
                }
                break;
            case 2:
                // media overlay layer: circle on right / bottom
                if (portrait) {
                    drawCircleSurface(surface, width / 2, height * 3 / 4, width / 4);
                } else {
                    drawCircleSurface(surface, width * 3 / 4, height / 2, height / 4);
                }
                break;
            case 3:
                // top layer: alpha stripes
                if (portrait) {
                    int halfLine = width / 8 + 1;
                    drawRectSurface(surface, width/2 - halfLine, 0, halfLine*2, height);
                } else {
                    int halfLine = height / 8 + 1;
                    drawRectSurface(surface, 0, height/2 - halfLine, width, halfLine*2);
                }
                break;
            default:
                throw new RuntimeException("wha?");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface destroyed holder=" + holder);
    }
}
