package com.dlodlo.admin.dlodlovrdraw;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.File;

/**
 * Created by admin on 2017/3/31.
 */

public class VideoBlob implements TextureView.SurfaceTextureListener{
    private final String LTAG;
    private TextureView mTextureView;
    private int mMovieTag;

    private SurfaceTexture mSavedSurfaceTexture;
    private PlayMovieThread mPlayThread;
    private SpeedControlCallback mCallback;
    public VideoBlob(TextureView mTextureView, int mMovieTag,int oridnal) {
        this.LTAG = MainActivity.TAG +oridnal;
        this.mTextureView = mTextureView;
        this.mMovieTag = mMovieTag;
        mCallback = new SpeedControlCallback();
        recreateView(mTextureView);
    }
    /**
     * Performs partial construction.  The VideoBlob is already created, but the Activity
     * was recreated, so we need to update our view.
     */
    public void recreateView(TextureView view) {
        Log.d(LTAG, "recreateView: " + view);
        mTextureView = view;
        mTextureView.setSurfaceTextureListener(this);
        if (mSavedSurfaceTexture != null) {
            Log.d(LTAG, "using saved st=" + mSavedSurfaceTexture);
            view.setSurfaceTexture(mSavedSurfaceTexture);
        }
    }
    /**
     * Stop playback and shut everything down.
     */
    public void stopPlayback() {
        Log.d(LTAG, "stopPlayback");
        mPlayThread.requestStop();
        // TODO: wait for the playback thread to stop so we don't kill the Surface
        //       before the video stops

        // We don't need this any more, so null it out.  This also serves as a signal
        // to let onSurfaceTextureDestroyed() know that it can tell TextureView to
        // free the SurfaceTexture.
        mSavedSurfaceTexture = null;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(LTAG, "onSurfaceTextureAvailable size=" + width + "x" + height + ", st=" + surface);
        if (mSavedSurfaceTexture == null) {
            mSavedSurfaceTexture = surface;

            File sliders = ContentManager.getInstance().getPath(mMovieTag);
            mPlayThread = new PlayMovieThread(sliders, new Surface(surface), mCallback);
        } else {
            // Can't do it here in Android <= 4.4.  The TextureView doesn't add a
            // listener on the new SurfaceTexture, so it never sees any updates.
            // Needs to happen from activity onCreate() -- see recreateView().
            //Log.d(LTAG, "using saved st=" + mSavedSurfaceTexture);
            //mTextureView.setSurfaceTexture(mSavedSurfaceTexture);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

}
