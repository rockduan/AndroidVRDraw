package com.dlodlo.admin.dlodlovrdraw;
import android.hardware.Camera;
import android.util.Log;

import java.util.List;

/**
 * Created by duanliang on 4/11/2017.
 */

public class CameraUtils {

    private static final String TAG = MainActivity.TAG;
    public static void choosePreviewSize(Camera.Parameters parms,int width,int height){
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if(ppsfv != null)
        {
            Log.d(TAG,"Camera preferred preview size for video is"+ppsfv.width + "x" + ppsfv.height);
        }
        for(Camera.Size size : parms.getSupportedPreviewSizes()){
            if(size.width == width && size.height == height){
                parms.setPreviewSize(width,height);
                return ;
            }
        }

        Log.w(TAG,"Unable to set preview size to"+width+"x"+height);

        if(ppsfv != null){
            parms.setPreviewSize(ppsfv.width,ppsfv.height);
        }
    }

    /**
     * Attempts to find a fixed preview frame rate that matches the desired frame rate.
     * <p>
     * It doesn't seem like there's a great deal of flexibility here.
     * <p>
     * TODO: follow the recipe from http://stackoverflow.com/questions/22639336/#22645327
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    public static int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;     // shrug
        }

        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }
}
