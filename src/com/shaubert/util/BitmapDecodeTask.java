package com.shaubert.util;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.io.File;

public abstract class BitmapDecodeTask extends AsyncTask<File, Void, Bitmap> {

    private static final Shlog SHLOG = new Shlog(BitmapDecodeTask.class.getSimpleName());
    
    private final int maxWidht;
    private final int maxHeight;
    
    public BitmapDecodeTask() {
        this(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    
    public BitmapDecodeTask(int maxWidht, int maxHeight) {
        this.maxWidht = maxWidht;
        this.maxHeight = maxHeight;
    }
    
    @Override
    protected Bitmap doInBackground(File... params) {
        File path = params[0];
        try {
            SHLOG.d("decoding image " + path); 
            return Bitmaps.loadScaledImage(path, maxWidht, maxHeight);
        } catch (Exception ex) {
            SHLOG.w(ex);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null) {
            onImageLoaded(result);
        } else {
            onLoadError();
        }
    }
    
    protected abstract void onImageLoaded(Bitmap bitmap);
    
    protected void onLoadError() {
    }
}
