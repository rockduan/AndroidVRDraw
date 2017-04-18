package com.dlodlo.admin.dlodlovrdraw;

import android.os.Handler;
import android.os.Message;
import android.view.InflateException;

import java.io.IOException;

/**
 * Created by admin on 2017/3/31.
 */

public class PlayTask implements Runnable{
    private static final int MSG_PLAY_STOPPED = 0;

    private MoviePlayer mPlayer;
    private MoviePlayer.PlayerFeedback mFeedback;
    private boolean mDoLoop;
    private Thread mThread;
    private LocalHandler mLocalHandler;
    private final Object mStopLock = new Object();
    private boolean mStopped = false;

    public PlayTask(MoviePlayer mPlayer, MoviePlayer.PlayerFeedback mFeedback) {
        this.mPlayer = mPlayer;
        this.mFeedback = mFeedback;
        mLocalHandler = new LocalHandler();
    }
    public void setLoopMode(boolean loopMode){
        mDoLoop = loopMode;
    }

    public void execute(){
        mPlayer.setLoopMode(mDoLoop);
        mThread = new Thread(this,"Movie Player");
        mThread.start();
    }
    public void requestStop(){
        mPlayer.requestStop();
    }

    public void waitForStop(){
        synchronized (mStopLock){
            while(!mStopped){
                try{
                    mStopLock.wait();
                }catch(InterruptedException ie){

                }
            }
        }
    }
    @Override
    public void run() {
        try{
            mPlayer.play();
        }catch(IOException ioe){
            throw new RuntimeException(ioe);
        }finally {
            synchronized (mStopLock){
                mStopped = true;
                mStopLock.notifyAll();
            }
            mLocalHandler.sendMessage(mLocalHandler.obtainMessage(MSG_PLAY_STOPPED, mFeedback));
        }
    }

    public class LocalHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what){
                case MSG_PLAY_STOPPED:
                    MoviePlayer.PlayerFeedback fb= (MoviePlayer.PlayerFeedback)msg.obj;
                    fb.playbackStopped();
                    break;
                default:
                    throw new RuntimeException("Unkown msg"+what);
            }
        }
    }
}
