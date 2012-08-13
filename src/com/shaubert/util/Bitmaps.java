package com.shaubert.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Bitmaps {

    public static class Size {
        public final int width;
        public final int height;
        
        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
    
    private static final Shlog SHLOG = new Shlog(Bitmaps.class.getSimpleName());
    
    public static Bitmap loadScaledImage(File file, int maxWidht, int maxHeight) {
        try {
            int inWidth = 0;
            int inHeight = 0;

            InputStream in = new FileInputStream(file);

            // decode image size (decode metadata only, not the whole image)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            in.close();
            in = null;

            // save width and height
            inWidth = options.outWidth;
            inHeight = options.outHeight;

            // decode full image pre-resized
            in = new FileInputStream(file);
            options = new BitmapFactory.Options();
            
            if (inWidth > maxWidht || inHeight > maxHeight) {
                // calc rought re-size (this is no exact resize)
                float scale = Math.max(inWidth/maxWidht, inHeight/maxHeight);
                options.inSampleSize = (int)Math.pow(2, Math.ceil(scale));
            }
            // decode full image
            return BitmapFactory.decodeStream(in, null, options);
        } catch (IOException e) {
            SHLOG.w(e);
        }
        return null;
    }
    
    public static Size getScaledSize(int maxWidht, int maxHeight, int width, int height) {
        if (width > maxWidht || height > maxHeight) {
            float scale = Math.max(width/maxWidht, height/maxHeight);
            float mul = (int)Math.pow(2, Math.ceil(scale));
            return new Size((int)(width / mul), (int)(height / mul));
        } else {
            return new Size(width, height);
        }
    }
    
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidht, int maxHeight) {
    	double aspect = Math.min((double) maxWidht / bitmap.getWidth(),
    			(double) maxHeight / bitmap.getHeight());
    	int widht = (int) (bitmap.getWidth() * aspect);
    	int height = (int) (bitmap.getHeight() * aspect);
    	return Bitmap.createScaledBitmap(bitmap, widht, height, false);
    }
}
