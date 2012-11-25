package com.shaubert.dirty.db;

import org.ecype.diego.Entity;
import org.ecype.diego.UpdateCallback;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DirtyPostTableUpdater implements UpdateCallback {

	//version 17, added "unread INTEGER" column
	//version 18, added "sub_blog_name TEXT" column
	
	@Override
	public void onUpdate(Entity entity, SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 17) {
			updateToVersion17(entity, db);
		}
		if (oldVersion < 18) {
			updateToVersion18(entity, db);
		}
	}

	private void updateToVersion18(Entity entity, SQLiteDatabase db) {
		String sql = "ALTER TABLE " + entity.getName()
				+ " ADD COLUMN sub_blog_name TEXT";
		Log.d("SQL", sql);
		db.execSQL(sql);
	}

	private void updateToVersion17(Entity entity, SQLiteDatabase db) {
		String sql = "ALTER TABLE " + entity.getName()
				+ " ADD COLUMN unread INTEGER";
		Log.d("SQL", sql);
		db.execSQL(sql);
		
		sql = "UPDATE " + entity.getName() + " SET unread = 0";
		Log.d("SQL", sql);
		db.execSQL(sql);
	}

}
