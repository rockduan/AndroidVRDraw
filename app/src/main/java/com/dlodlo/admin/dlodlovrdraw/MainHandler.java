package com.dlodlo.admin.dlodlovrdraw;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by duanliang on 4/7/2017.
 */

public class MainHandler extends Handler{
    private static final String TAG = MainActivity.TAG;
    private static final int MSG_SEND_CAMERA_PARAMS0 = 0;
    private static final int MSG_SEND_CAMERA_PARAMS1 = 1;
    private static final int MSG_SEND_RECT_SIZE = 2;
    private static final int MSG_SEND_ZOOM_AREA = 3;
    private static final int MSG_SEND_ROTATE_DEG = 4;

    private WeakReference<TextureFromCameraActivity> mWeakActivity;

    public MainHandler(TextureFromCameraActivity activity){
        mWeakActivity = new WeakReference<TextureFromCameraActivity>(activity);

    }
    /**
     * Sends the updated camera parameters to the main thread.
     * <p>
     * Call from render thread.
     */
    public void sendCameraParams(int width, int height, float fps) {
        // The right way to do this is to bundle them up into an object.  The lazy
        // way is to send two messages.
        sendMessage(obtainMessage(MSG_SEND_CAMERA_PARAMS0, width, height));
        sendMessage(obtainMessage(MSG_SEND_CAMERA_PARAMS1, (int) (fps * 1000), 0));
    }

    /**
     * Sends the updated rect size to the main thread.
     * <p>
     * Call from render thread.
     */
    public void sendRectSize(int width, int height) {
        sendMessage(obtainMessage(MSG_SEND_RECT_SIZE, width, height));
    }

    /**
     * Sends the updated zoom area to the main thread.
     * <p>
     * Call from render thread.
     */
    public void sendZoomArea(int width, int height) {
        sendMessage(obtainMessage(MSG_SEND_ZOOM_AREA, width, height));
    }

    /**
     * Sends the updated zoom area to the main thread.
     * <p>
     * Call from render thread.
     */
    public void sendRotateDeg(int rot) {
        sendMessage(obtainMessage(MSG_SEND_ROTATE_DEG, rot, 0));
    }

    @Override
    public void handleMessage(Message msg) {
        TextureFromCameraActivity activity = mWeakActivity.get();
        if (activity == null) {
            Log.d(TAG, "Got message for dead activity");
            return;
        }

        switch (msg.what) {
            case MSG_SEND_CAMERA_PARAMS0: {
                activity.mCameraPreviewWidth = msg.arg1;
                activity.mCameraPreviewHeight = msg.arg2;
                break;
            }
            case MSG_SEND_CAMERA_PARAMS1: {
                activity.mCameraPreviewFps = msg.arg1 / 1000.0f;
                activity.updateControls();
                break;
            }
            case MSG_SEND_RECT_SIZE: {
                activity.mRectWidth = msg.arg1;
                activity.mRectHeight = msg.arg2;
                activity.updateControls();
                break;
            }
            case MSG_SEND_ZOOM_AREA: {
                activity.mZoomWidth = msg.arg1;
                activity.mZoomHeight = msg.arg2;
                activity.updateControls();
                break;
            }
            case MSG_SEND_ROTATE_DEG: {
                activity.mRotateDeg = msg.arg1;
                activity.updateControls();
                break;
            }
            default:
                throw new RuntimeException("Unknown message " + msg.what);
        }
    }
}
