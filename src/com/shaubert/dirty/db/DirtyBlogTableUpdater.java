package com.shaubert.dirty.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.shaubert.dirty.client.DirtySubBlog;
import org.ecype.diego.Entity;
import org.ecype.diego.UpdateCallback;

public class DirtyBlogTableUpdater implements UpdateCallback {

    @Override
    public void onUpdate(Entity entity, SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 21) {
            updateTo22Version(db, entity);
        }
    }

    private void updateTo22Version(SQLiteDatabase db, Entity entity) {
        db.beginTransaction();
        try {
            db.execSQL("ALTER TABLE " + entity.getName()
                    + " ADD COLUMN title_lower TEXT");
            db.execSQL("ALTER TABLE " + entity.getName()
                    + " ADD COLUMN name_lower TEXT");
            db.execSQL("ALTER TABLE " + entity.getName()
                    + " ADD COLUMN description_lower TEXT");

            BlogsCursor cursor = new BlogsCursor(db.query(entity.getName(), null, null, null, null, null, null));
            if (cursor.moveToFirst()) {
                DirtySubBlog blog = new DirtySubBlog();
                do {
                    blog.getValues().clear();
                    blog.setNameLower(cursor.getName() == null ? null : cursor.getName().toLowerCase());
                    blog.setTitleLower(cursor.getTitle() == null ? null : cursor.getTitle().toLowerCase());
                    blog.setDescriptionLower(cursor.getDescription() == null ? null : cursor.getDescription().toLowerCase());
                    db.update(entity.getName(), blog.getValues(),
                            DirtyContract.DirtyBlogEntity.ID + "=" + cursor.getId(), null);
                } while (cursor.moveToNext());
            }
            cursor.close();

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

}
