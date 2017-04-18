package com.dlodlo.admin.dlodlovrdraw;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by admin on 2017/3/31.
 */

public class GenerateTask  extends AsyncTask<Void,Integer,Integer> implements ContentManager.ProgressUpdater{
    private String TAG = MainActivity.TAG;
    // ----- accessed from UI thread -----
    private final Context mContext;
    private final AlertDialog mPrepDialog;
    private final ProgressBar mProgressBar;

    // ----- accessed from async thread -----
    private int mCurrentIndex;

    // ----- accessed from both -----
    private final int[] mTags;
    private volatile RuntimeException mFailure;


    public GenerateTask(Context context, AlertDialog dialog, int[] tags) {
        mContext = context;
        mPrepDialog = dialog;
        mTags = tags;
        mProgressBar = (ProgressBar) mPrepDialog.findViewById(R.id.work_progress);
        mProgressBar.setMax(tags.length * 100);
    }

    @Override // async task thread
    protected Integer doInBackground(Void... params) {
        ContentManager contentManager = ContentManager.getInstance();

        Log.d(TAG, "doInBackground...");
        for (int i = 0; i < mTags.length; i++) {
            mCurrentIndex = i;
            updateProgress(0);
            try{
                contentManager.prepare(this, mTags[i]);
            } catch (RuntimeException re) {
                mFailure = re;
                break;
            }
            updateProgress(100);
        }

        if (mFailure != null) {
            Log.w(TAG, "Failed while generating content", mFailure);
        } else {
            Log.d(TAG, "generation complete");
        }
        return 0;
    }

    @Override // async task thread
    public void updateProgress(int percent) {
        publishProgress(mCurrentIndex, percent);
    }

    @Override // UI thread
    protected void onProgressUpdate(Integer... progressArray) {
        int index = progressArray[0];
        int percent = progressArray[1];
        //Log.d(TAG, "progress " + index + "/" + percent + " of " + mTags.length * 100);
        if (percent == 0) {
            TextView name = (TextView) mPrepDialog.findViewById(R.id.workJobName_text);
            name.setText(ContentManager.getInstance().getFileName(mTags[index]));
        }
        mProgressBar.setProgress(index * 100 + percent);
    }

    @Override // UI thread
    protected void onPostExecute(Integer result) {
        Log.d(TAG, "onPostExecute -- dismss");
        mPrepDialog.dismiss();

        if (mFailure != null) {
            showFailureDialog(mContext, mFailure);
        }
    }

    /**
     * Posts an error dialog, including the message from the failure exception.
     */
    private void showFailureDialog(Context context, RuntimeException failure) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.contentGenerationFailedTitle);
        String msg = context.getString(R.string.contentGenerationFailedMsg,
                failure.getMessage());
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
