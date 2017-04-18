package com.dlodlo.admin.dlodlovrdraw;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.InflateException;
import android.view.View;

/**
 * Created by admin on 2017/3/31.
 */

public class WorkDialog {
    private static final String TAG = MainActivity.TAG;

    private WorkDialog() {}     // nah

    /**
     * Prepares an alert dialog builder, using the work_dialog view.
     * <p>
     * The caller should finish populating the builder, then call AlertDialog.Builder#show().
     */
    public static AlertDialog.Builder create(Activity activity, int titleId) {
        View view;
        try {
            view = activity.getLayoutInflater().inflate(R.layout.work_dialog, null);
        } catch (InflateException ie) {
            Log.e(TAG, "Exception while inflating work dialog layout: " + ie.getMessage());
            throw ie;
        }

        String title = activity.getString(titleId);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setView(view);
        return builder;
    }
}
