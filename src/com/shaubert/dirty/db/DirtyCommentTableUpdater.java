package com.shaubert.dirty.db;

import org.ecype.diego.Entity;
import org.ecype.diego.UpdateCallback;

import android.database.sqlite.SQLiteDatabase;

public class DirtyCommentTableUpdater implements UpdateCallback {
	
	@Override
	public void onUpdate(Entity entity, SQLiteDatabase db, int oldVersion, int newVersion) {
	}

}
