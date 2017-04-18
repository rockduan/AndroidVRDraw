package com.dlodlo.admin.dlodlovrdraw;

import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;

/**
 * Created by admin on 2017/3/31.
 */

public class PlayMovieThread extends Thread{
    private String TAG = MainActivity.TAG;
    private final File mFile;
    private final Surface mSurface;
    private final SpeedControlCallback mCallback;
    private MoviePlayer mMoviePlayer;

    public PlayMovieThread(File mFile, Surface mSurface, SpeedControlCallback mCallback) {
        this.mFile = mFile;
        this.mSurface = mSurface;
        this.mCallback = mCallback;
        start();
    }

    public void requestStop(){
        mMoviePlayer.requestStop();
    }
    @Override
    public void run() {
        try {
            mMoviePlayer = new MoviePlayer(mFile, mSurface, mCallback);
            mMoviePlayer.setLoopMode(true);
            mMoviePlayer.play();
        } catch (IOException ioe) {
            Log.e(TAG, "movie playback failed", ioe);
        } finally {
            mSurface.release();
            Log.d(TAG, "PlayMovieThread stopping");
        }
    }
}
