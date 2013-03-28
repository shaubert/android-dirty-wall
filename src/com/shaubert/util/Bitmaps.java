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
        InputStream in = null;
        try {
            int inWidth = 0;
            int inHeight = 0;

            in = new FileInputStream(file);

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
            options.inPurgeable = true;
            if (inWidth > maxWidht || inHeight > maxHeight) {
                // calc rought re-size (this is no exact resize)
                float scale = Math.max((float)inWidth/maxWidht, (float)inHeight/maxHeight);
                int pow = log2((int) Math.ceil(scale));
                options.inSampleSize = (int)Math.pow(2, pow);
            }
            // decode full image
            return BitmapFactory.decodeStream(in, null, options);
        } catch (IOException e) {
            SHLOG.w(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    public static Size getScaledSize(int maxWidht, int maxHeight, int width, int height) {
        if (width > maxWidht || height > maxHeight) {
            float scale = Math.max((float) width/maxWidht, (float) height/maxHeight);
            int pow = log2((int) Math.ceil(scale));
            return new Size(width / pow, height / pow);
        } else {
            return new Size(width, height);
        }
    }

    private static int log2(int x) {
        int pow = 0;
        if(x % 2 == 1) { pow += 1;}
        if(x >= (1 << 16)) { x >>= 16; pow +=  16;}
        if(x >= (1 << 8 )) { x >>=  8; pow +=   8;}
        if(x >= (1 << 4 )) { x >>=  4; pow +=   4;}
        if(x >= (1 << 2 )) { x >>=  2; pow +=   2;}
        if(x >= (1 << 1 )) { x >>=  1; pow +=   1;}
        return pow;
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidht, int maxHeight) {
    	double aspect = Math.min((double) maxWidht / bitmap.getWidth(),
    			(double) maxHeight / bitmap.getHeight());
    	int widht = (int) (bitmap.getWidth() * aspect);
    	int height = (int) (bitmap.getHeight() * aspect);
    	return Bitmap.createScaledBitmap(bitmap, widht, height, false);
    }
}
