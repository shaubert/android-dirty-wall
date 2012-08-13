
package com.shaubert.util;

import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.dirty.net.RequestContract.Request;
import com.shaubert.net.nutshell.RequestStatus;

import android.content.Context;

public class DatabaseCleaner {

    private static final Shlog SHLOG = new Shlog(DatabaseCleaner.class.getSimpleName());

    public static void startCleanUpIfNeeded(final Context context) {
        new Thread() {
            public void run() {
                cleanUpRequests(context);
                cleanUpPosts(context);
            }
        }.start();
    }

    protected static void cleanUpPosts(Context context) {
        try {
            long twoWeaksAgo = System.currentTimeMillis() - 14 * 24 * 3600 * 1000;
            context.getContentResolver().delete(
                    DirtyPostEntity.URI,
                    "IFNULL(" + DirtyPostEntity.FAVORITE + ",0) = 0 AND " + DirtyPostEntity.INSERT_TIME + "<"
                            + twoWeaksAgo, null);
        } catch (Exception ex) {
            SHLOG.w(ex);
        }
    }

    protected static void cleanUpRequests(Context context) {
        try {
            long weakAgo = System.currentTimeMillis() - 7 * 24 * 3600 * 1000;
            context.getContentResolver().delete(
                    Request.URI,
                    "(" + Request.STATUS + "= ? OR " + Request.STATUS + "= ?) AND (" + Request.CREATION_TIME + "<"
                            + weakAgo + ")", new String[] {
                            RequestStatus.FINISHED.toString(), RequestStatus.FINISHED_WITH_ERRORS.toString()
                    });
        } catch (Exception ex) {
            SHLOG.w(ex);
        }
    }

}
