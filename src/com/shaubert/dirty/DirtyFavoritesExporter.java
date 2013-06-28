package com.shaubert.dirty;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.preference.PreferenceManager;
import com.shaubert.dirty.client.DirtyBlog;
import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.util.Files;
import com.shaubert.util.Shlog;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import java.io.*;

public class DirtyFavoritesExporter {

    private static final Shlog SHLOG = new Shlog(DirtyFavoritesExporter.class.getSimpleName());
    
    private Activity activity;
    private AsyncTask<Void, Void, File> exportTask;
    private DirtyPreferences dirtyPreferences;

    public DirtyFavoritesExporter(Activity activity) {
        this.activity = activity;
        this.dirtyPreferences = new DirtyPreferences(PreferenceManager.getDefaultSharedPreferences(activity), activity);
    }
    
    public void startExport() {
        if (exportTask == null || exportTask.getStatus() == Status.FINISHED) {
            exportTask = new AsyncTask<Void, Void, File>() {
                
                private Exception ex;
                
                @Override
                protected File doInBackground(Void... params) {
                    try {
                        return saveFavoritesInBackground();
                    } catch (IOException e) {
                        SHLOG.w(e);
                        ex = e;
                        return null;
                    }
                }
                
                @Override
                protected void onPostExecute(File result) {
                    if (result == null) {
                        if (ex != null) {
                            showErrorMessage();
                        } else {
                            showZeroFavoritesMessage();
                        }
                    } else {
                        showTaskFinishedMessage(result);
                    }
                }
            };
            exportTask.execute();
        }
    }
    
    protected void showZeroFavoritesMessage() {
        DirtyMessagesProvider provider = DirtyMessagesProvider.getInstance(activity);
        if (dirtyPreferences.isUseCrouton()) {
            Crouton.clearCroutonsForActivity(activity);
            Crouton.makeText(activity, provider.getZeroFavoritesMessageCompact(), Style.ALERT).show();
        } else {
            DirtyToast.show(activity, provider.getRandomFaceImageId(), provider.getZeroFavoritesMessage());
        }
    }

    protected void showTaskFinishedMessage(File result) {
        DirtyMessagesProvider provider = DirtyMessagesProvider.getInstance(activity);
        CharSequence message = provider.getFavoritesExportFinishedMessage(result.getAbsolutePath());
        if (dirtyPreferences.isUseCrouton()) {
            Crouton.clearCroutonsForActivity(activity);
            Crouton.makeText(activity, message, Style.CONFIRM).show();
        }  else {
            DirtyToast.show(activity, provider.getRandomFaceImageId(), message);
        }
    }

    protected void showErrorMessage() {
        DirtyMessagesProvider provider = DirtyMessagesProvider.getInstance(activity);
        if (dirtyPreferences.isUseCrouton()) {
            Crouton.clearCroutonsForActivity(activity);
            Crouton.makeText(activity, provider.getSimpleErrorMessage(), Style.ALERT).show();
        } else {
            DirtyToast.show(activity, provider.getRandomFaceImageId(), provider.getErrorMessage());
        }
    }

    private File saveFavoritesInBackground() throws IOException {
        final Cursor cursor = activity.getContentResolver().query(DirtyPostEntity.URI,
                null, 
                DirtyPostEntity.FAVORITE + "!= 0", null, 
                DirtyPostEntity.SERVER_ID + " DESC");
        try {
            if (cursor.moveToFirst()) {
                final File result = Files.getFavoritesExportFile();
                new Files.OutputTask<OutputStreamWriter>() {
                    @Override
                    protected OutputStreamWriter openStream() throws FileNotFoundException {
                        return new OutputStreamWriter(new FileOutputStream(result));
                    }

                    @Override
                    protected void doWrite(OutputStreamWriter stream) throws IOException {
                        DirtyBlog blog = DirtyBlog.getInstance();
                        SummaryFormatter formatter = new SummaryFormatter(activity);
                        boolean writeDivider = false;
                        do {
                            ContentValues values = new ContentValues();
                            DatabaseUtils.cursorRowToContentValues(cursor, values);
                            DirtyPost post = new DirtyPost(values);
                            
                            if (writeDivider) {
                                stream.write("\n-------------------------------------------------\n\n");
                            }
                            writeDivider = true;
                            
                            stream.write(blog.getPostLink(post));
                            stream.write("\n\n");
                            stream.write(post.getMessage());
                            stream.write("\n\n");
                            stream.write(formatter.formatSummaryTextForExport(post));
                        } while (cursor.moveToNext());
                    }
                }.performWrite();
                return result;
            }
        } finally {
            cursor.close();
        }
        return null;
    }
}
