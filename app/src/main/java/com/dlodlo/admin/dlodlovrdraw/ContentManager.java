package com.dlodlo.admin.dlodlovrdraw;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by admin on 2017/3/31.
 */

public class ContentManager {
    private static final String TAG = MainActivity.TAG;
    public static final int MOVIE_EIGHT_RECTS = 0;
    public static final int MOVIE_SLIDERS = 1;
    private static final int[] ALL_TAGS = new int[]{
            MOVIE_EIGHT_RECTS,
            MOVIE_SLIDERS
    };

    private static final Object sLock = new Object();
    private static ContentManager sInstance = null;
    private boolean mInitialized = false;
    private File mFilesDir;
    private ArrayList<Content> mContent;

    public static ContentManager getInstance(){
        synchronized (sLock){
            if(sInstance == null){
                sInstance = new ContentManager();
            }
            return sInstance;
        }
    }
    private ContentManager(){

    }
    /**
     * Creates all content, overwriting any existing entries.
     * <p>
     * Call from main UI thread.
     */
    public void createAll(Activity caller) {
        prepareContent(caller, ALL_TAGS);
    }
    public interface ProgressUpdater{
        void updateProgress(int percent);
    }
    /**
     * Prepares the specified content.  For example, if the caller requires a movie that doesn't
     * exist, this will post a progress dialog and generate the movie.
     * <p>
     * Call from main UI thread.  This returns immediately.  Content generation continues
     * on a background thread.
     */
    public void prepareContent(Activity caller, int[] tags) {
        // Put up the progress dialog.
        Log.d(TAG,"prepareContent");
        AlertDialog.Builder builder = WorkDialog.create(caller, R.string.preparing_content);
        builder.setCancelable(false);
        AlertDialog dialog = builder.show();
        // Generate content in async task.
        GenerateTask genTask = new GenerateTask(caller, dialog, tags);
        genTask.execute();
    }

    /**
     * Prepares the specified item.
     * <p>
     * This may be called from the async task thread.
     */
    public void prepare(ProgressUpdater prog, int tag) {
        Log.d(TAG,"ContentManager.prepare+"+tag);
        GeneratedMovie movie;
        switch (tag) {
            case MOVIE_EIGHT_RECTS:
                movie = new MovieEightRects();
                movie.create(getPath(tag), prog);
                synchronized (mContent) {
                    mContent.add(tag, movie);
                }
                break;
            case MOVIE_SLIDERS:
                movie = new MovieSliders();
                movie.create(getPath(tag), prog);
                synchronized (mContent) {
                    mContent.add(tag, movie);
                }
                break;
            default:
                throw new RuntimeException("Unknown tag " + tag);
        }
    }
    public static void initialize(Context context){
        ContentManager mgr = getInstance();
        synchronized (sLock){
            if(!mgr.mInitialized){
                mgr.mFilesDir = context.getFilesDir();
                mgr.mContent = new ArrayList<Content>();
                mgr.mInitialized = true;
            }
        }
    }

    public boolean isContentCreated(Context unused){
        for(int i = 0 ; i < ALL_TAGS.length;i++)
        {
            File file = getPath(i);
            Log.d(TAG,"check if file can read,file.path="+file.getPath().toString());
            if(!file.exists())
            {
                Log.d(TAG,"file not exist return false;");
                return false;
            }else{
                Log.d(TAG,"file exist!");
            }
            if(!mFilesDir.canRead()) {
                Log.d(TAG, "Can't find readable" + file);
                return false;
            }else{
                Log.d(TAG,"file can read !");
            }
        }
        return true;
    }
    /**
     * Returns the storage location for the specified item.
     */
    public File getPath(int tag) {
        return new File(mFilesDir, getFileName(tag));
    }
    /**
     * Returns the filename for the tag.
     */
    public String getFileName(int tag) {
        switch (tag) {
            case MOVIE_EIGHT_RECTS:
                return "gen-eight-rects.mp4";
            case MOVIE_SLIDERS:
                return "gen-sliders.mp4";
            default:
                throw new RuntimeException("Unknown tag " + tag);
        }
    }
}
