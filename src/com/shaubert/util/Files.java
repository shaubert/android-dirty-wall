package com.shaubert.util;

import com.shaubert.dirty.client.DirtyPost;

import android.content.Context;
import android.os.Environment;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Files {
    
    private static final Shlog SHLOG = new Shlog(Files.class.getSimpleName());
    
    public static final long PREFFERED_MAX_CACHE_SIZE = 50 * 1024 * 1024;
        
    public static abstract class OutputTask<T extends Closeable> {
        public void performWrite() throws IOException {
            T stream = null;
            try {
                stream = openStream();
                doWrite(stream);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        
        protected abstract T openStream() throws IOException;
        
        protected abstract void doWrite(T stream) throws IOException;
    }
    
    public static File getGertrudaFile(Context context, String url) {
        return new File(getGertrudaDir(context), url.substring(url.lastIndexOf('/') + 1));
    }
    
    public static File getGertrudaDir(Context context) {
        File dir = new File(getHomeDir(context), "/gertruda");
        dir.mkdirs();
        return dir;
    }

    public static File getPostImageCache(Context context, DirtyPost post, String url) {
        File dir = new File(getHomeDir(context), "/posts/" + post.getServerId() + "/");
        dir.mkdirs();
        return new File(dir, "" + url.hashCode() + ".png");
    }
    
    public static File getCommentImageCache(Context context, long commentServerId, String url) {
        File dir = new File(getHomeDir(context), "/comments/");
        dir.mkdirs();
        return new File(dir, "" + commentServerId + url.hashCode() + ".png");
    }
    
    public static File getHomeDir(Context context) {
        File dir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + context.getPackageName());
        dir.mkdirs();
        return dir;
    }
    
    public static File getFavoritesExportFile() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Dirty");
        dir.mkdirs();
        return new File(dir, "favorites.txt");
    }
 
    public static void startCleanUpCacheIfNeeded(final Context context, final long maxCacheSize) {
        new Thread() {
            @Override
            public void run() {
                List<File> files = searchFiles(getHomeDir(context), null);
                long size = getFilesSize(files);
                SHLOG.d("Current cache size = " + size / (1024 * 1024f) + " MiB");
                if (size > maxCacheSize) {
                    SHLOG.d("Cleaning up cache...");
                    Collections.sort(files, new Comparator<File>() {
                        @Override
                        public int compare(File lhs, File rhs) {
                            return ((Long) lhs.lastModified()).compareTo(rhs.lastModified());
                        }
                    });
                    long newSize = (long)(maxCacheSize * 0.6f);
                    for (File file : files) {
                        long fSize = file.length();
                        if (file.delete()) {
                            SHLOG.d("Cleaning up cache: removed file " + file.getAbsolutePath());
                            size -= fSize;
                        }
                        if (size <= newSize) {
                            break;
                        }
                    }
                    SHLOG.d("Cache size cleaned up to " + size / (1024 * 1024f) + " MiB");
                } else {
                    SHLOG.d("Cache size is in bounds");
                }
            }
        }.start();
    }
    
    public static List<File> searchFiles(File where, FilenameFilter filter) {
        List<File> result = new ArrayList<File>();
        String[] files = where.list();
        if (files != null) {
            for (String name : files) {
                if (".".equals(name) || "..".equals(name)) {
                    continue;
                }

                File file = new File(where, name);
                if (file.isDirectory()) {
                    result.addAll(searchFiles(file, filter));
                } else {
                    if (filter == null || (filter != null && filter.accept(where, name))) {
                        result.add(file);
                    }
                }
            }
        }
        return result;
    }
    
    public static long getFilesSize(List<File> files) {
        long res = 0;
        for (File file : files) {
            res += file.length();
        }
        return res;
    }
}