package com.dlodlo.admin.dlodlovrdraw;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.SimpleAdapter;

/**
 * Created by admin on 2017/3/31.
 */

public class DoubleDecodeActivity extends Activity {
    private static final String TAG = MainActivity.TAG;
    private static final int VIDEO_COUNT = 2;
    private static boolean sVideoRunning = false;
    private static VideoBlob[] sBlob = new VideoBlob[VIDEO_COUNT];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_double_decode);
        Log.d(TAG,"DoubleDecodeActivity.onCreate");








        if(!sVideoRunning){
            sBlob[0] = new VideoBlob((TextureView)findViewById(R.id.double1_texture_view),
                    ContentManager.MOVIE_EIGHT_RECTS,0);
            sBlob[1] = new VideoBlob((TextureView)findViewById(R.id.double2_texture_view),
                    ContentManager.MOVIE_SLIDERS,0);
            sVideoRunning = true;
        }else{
            sBlob[0].recreateView((TextureView) findViewById(R.id.double1_texture_view));
            sBlob[1].recreateView((TextureView) findViewById(R.id.double2_texture_view));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        boolean finishing = isFinishing();
        Log.d(TAG,"isFinishing"+finishing);
        for(int i = 0;i<VIDEO_COUNT;i++)
        {
            if(finishing){
                sBlob[i].stopPlayback();
                sBlob[i] = null;
            }
        }
        sVideoRunning = !finishing;
        Log.d(TAG,"onPause complete");
    }
}
