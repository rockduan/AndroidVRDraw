package com.dlodlo.admin.dlodlovrdraw;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.logging.LogRecord;

import static com.dlodlo.admin.dlodlovrdraw.MainActivity.TAG;

/**
 * Created by duanliang on 4/7/2017.
 */

public class RenderHandler extends Handler {
    private static final int MSG_SURFACE_AVAILABLE = 0;
    private static final int MSG_SURFACE_CHANGED = 1;
    private static final int MSG_SURFACE_DESTROYED = 2;
    private static final int MSG_SHUTDOWN = 3;
    private static final int MSG_FRAME_AVAILABLE = 4;
    private static final int MSG_ZOOM_VALUE = 5;
    private static final int MSG_SIZE_VALUE = 6;
    private static final int MSG_ROTATE_VALUE = 7;
    private static final int MSG_POSITION = 8;
    private static final int MSG_REDRAW = 9;
    private static final int MSG_TEXTURE_ID = 10;
    private WeakReference<RenderThread> mWeakRenderThread;

    public RenderHandler(RenderThread rt){
        mWeakRenderThread = new WeakReference<RenderThread>(rt);
    }
    public void sendSurfaceAvailable(SurfaceHolder holder,boolean newSurface){
        sendMessage(obtainMessage(MSG_SURFACE_AVAILABLE,newSurface?1:0,0,holder));

    }

    public void sendSurfaceChanged(int format,int width,int height){
        sendMessage(obtainMessage(MSG_SURFACE_CHANGED,width,height));
    }

    public void sendSurfaceDestroyed(){
        sendMessage(obtainMessage(MSG_SURFACE_DESTROYED));
    }
    public void sendShutdown(){
        sendMessage(obtainMessage(MSG_SHUTDOWN));
    }
    public void sendFrameAvailable(){
        sendMessage(obtainMessage(MSG_FRAME_AVAILABLE));
    }

    public void sendZoomValue(int progress){
        sendMessage(obtainMessage(MSG_ZOOM_VALUE,progress,0));
    }
    public void sendSizeValue(int progress){
        sendMessage(obtainMessage(MSG_SIZE_VALUE,progress,0));
    }

    public void sendRotateValue(int progress){
        sendMessage(obtainMessage(MSG_ROTATE_VALUE,progress));
    }

    public void sendPosition(int x,int y){
        sendMessage(obtainMessage(MSG_POSITION,x,y));
    }
    public void sendRedraw(){
        sendMessage(obtainMessage(MSG_REDRAW));
    }
    public void sendTexturId(int textureid){
        sendMessage(obtainMessage(MSG_TEXTURE_ID,textureid));
    }
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        int what = msg.what;

        RenderThread renderThread = mWeakRenderThread.get();
        if(renderThread == null){
            Log.w(TAG,"RenderHandler.handleMessage: weak ref is null");
            return ;
        }

        switch (what){
            case MSG_SURFACE_AVAILABLE:
                renderThread.surfaceAvailable((SurfaceHolder) msg.obj, msg.arg1 != 0);
                break;
            case MSG_SURFACE_CHANGED:
                renderThread.surfaceChanged(msg.arg1, msg.arg2);
                break;
            case MSG_SURFACE_DESTROYED:
                renderThread.surfaceDestroyed();
                break;
            case MSG_SHUTDOWN:
                renderThread.shutdown();
                break;
            case MSG_FRAME_AVAILABLE:
                renderThread.frameAvailable();
                break;
            case MSG_ZOOM_VALUE:
                renderThread.setZoom(msg.arg1);
                break;
            case MSG_SIZE_VALUE:
                renderThread.setSize(msg.arg1);
                break;
            case MSG_ROTATE_VALUE:
                renderThread.setRotate(msg.arg1);
                break;
            case MSG_POSITION:
                renderThread.setPosition(msg.arg1, msg.arg2);
                break;
            case MSG_REDRAW:
                renderThread.draw();
                break;
            case MSG_TEXTURE_ID:
                renderThread.setTextureId(msg.arg1);
                break;
            default:
                throw new RuntimeException("unknown message " + what);
        }
    }
}
