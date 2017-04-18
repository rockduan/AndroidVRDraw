package com.dlodlo.admin.dlodlovrdraw;

import android.util.Log;

/**
 * Created by admin on 2017/3/31.
 */

public class SpeedControlCallback implements MoviePlayer.FrameCallback{

    private static final String TAG = MainActivity.TAG;
    private static final boolean CHECK_SLEEP_TIME = false;
    private static final long ONE_MILLION = 1000000L;

    private long mPrevPresentUsec;
    private long mPrevMonoUsec;
    private long mFixedFrameDurationUsec;
    private boolean mLoopReset;

    /**
     * Sets a fixed playback rate.  If set, this will ignore the presentation time stamp
     * in the video file.  Must be called before playback thread starts.
     */
    public void setFixedPlaybackRate(int fps) {
        mFixedFrameDurationUsec = ONE_MILLION / fps;
    }


    @Override
    public void preRender(long presentationTimeUsec) {
        if(mPrevMonoUsec == 0){
            mPrevMonoUsec = System.nanoTime() /1000;
            mPrevPresentUsec = presentationTimeUsec;
        }else{
            long frameDelta;
            if(mLoopReset){
                mPrevPresentUsec = presentationTimeUsec - ONE_MILLION / 30;
                mLoopReset = false;
            }

            if(mFixedFrameDurationUsec != 0)
            {
                frameDelta = mFixedFrameDurationUsec;
            }else{
                frameDelta = presentationTimeUsec - mPrevPresentUsec;
            }
            if (frameDelta < 0) {
                Log.w(TAG, "Weird, video times went backward");
                frameDelta = 0;
            } else if (frameDelta == 0) {
                // This suggests a possible bug in movie generation.
                Log.i(TAG, "Warning: current frame and previous frame had same timestamp");
            } else if (frameDelta > 10 * ONE_MILLION) {
                // Inter-frame times could be arbitrarily long.  For this player, we want
                // to alert the developer that their movie might have issues (maybe they
                // accidentally output timestamps in nsec rather than usec).
                Log.i(TAG, "Inter-frame pause was " + (frameDelta / ONE_MILLION) +
                        "sec, capping at 5 sec");
                frameDelta = 5 * ONE_MILLION;
            }
            long desiredUsec = mPrevMonoUsec + frameDelta;  // when we want to wake up
            long nowUsec = System.nanoTime() / 1000;
            while (nowUsec < (desiredUsec - 100) /*&& mState == RUNNING*/) {
                // Sleep until it's time to wake up.  To be responsive to "stop" commands
                // we're going to wake up every half a second even if the sleep is supposed
                // to be longer (which should be rare).  The alternative would be
                // to interrupt the thread, but that requires more work.
                //
                // The precision of the sleep call varies widely from one device to another;
                // we may wake early or late.  Different devices will have a minimum possible
                // sleep time. If we're within 100us of the target time, we'll probably
                // overshoot if we try to sleep, so just go ahead and continue on.
                long sleepTimeUsec = desiredUsec - nowUsec;
                if (sleepTimeUsec > 500000) {
                    sleepTimeUsec = 500000;
                }
                try {
                    if (CHECK_SLEEP_TIME) {
                        long startNsec = System.nanoTime();
                        Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
                        long actualSleepNsec = System.nanoTime() - startNsec;
                        Log.d(TAG, "sleep=" + sleepTimeUsec + " actual=" + (actualSleepNsec/1000) +
                                " diff=" + (Math.abs(actualSleepNsec / 1000 - sleepTimeUsec)) +
                                " (usec)");
                    } else {
                        Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
                    }
                } catch (InterruptedException ie) {}
                nowUsec = System.nanoTime() / 1000;
            }

            // Advance times using calculated time values, not the post-sleep monotonic
            // clock time, to avoid drifting.
            mPrevMonoUsec += frameDelta;
            mPrevPresentUsec += frameDelta;
        }
    }

    @Override
    public void postRender() {

    }

    @Override
    public void loopReset() {
        mLoopReset = true;
    }
}
