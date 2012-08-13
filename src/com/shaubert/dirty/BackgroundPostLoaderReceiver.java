package com.shaubert.dirty;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.shaubert.dirty.net.DirtyPostLoadRequest;
import com.shaubert.dirty.net.RequestContract;
import com.shaubert.dirty.net.RequestService;
import com.shaubert.net.core.DefaultExecutorBridge;
import com.shaubert.net.core.DefaultJournal;
import com.shaubert.net.core.DefaultRequestRecreator;
import com.shaubert.net.core.RequestRepositoryOnContentResolver;
import com.shaubert.net.nutshell.Journal;
import com.shaubert.util.Networks;
import com.shaubert.util.Shlog;

public class BackgroundPostLoaderReceiver extends BroadcastReceiver {

	private static Shlog SHLOG = new Shlog(BackgroundPostLoaderReceiver.class.getSimpleName());
	
	public static final String SYNC_ACTION = "com.shaubert.dirty.BackgroundPostLoaderReceiver.SYNC_ACTION";
	private static final int SYNC_REQUEST_CODE = 2031;
	
	private Journal journal;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		SHLOG.d("intent received: " + intent);
		if (SYNC_ACTION.equals(intent.getAction())) {
			if (Networks.hasInternet(context)) {
				SHLOG.d("starting request to load posts...");
				setupJournal(context);
				launchSync();
			} else {
				SHLOG.w("not internet connection");
			}
		}
	}

	protected void setupJournal(Context context) {
		RequestRepositoryOnContentResolver repository = new RequestRepositoryOnContentResolver(
				context.getApplicationContext(), 
				new DefaultRequestRecreator(context.getApplicationContext()), RequestContract.Request.URI);
		DefaultExecutorBridge executorBridge = new DefaultExecutorBridge(context.getApplicationContext(), 
				RequestService.class);
		journal = new DefaultJournal(repository, executorBridge, null);
	}	

	private void launchSync() {
		DirtyPostLoadRequest postLoadRequest = new DirtyPostLoadRequest();
		postLoadRequest.getState().put("background-notification", true);
		journal.register(postLoadRequest);
	}
	
	public static void scheduleSync(Context context, long interval) {
		PendingIntent pendingIntent = createIntent(context);
		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
				SystemClock.elapsedRealtime() + interval, interval, pendingIntent);
	}
	
	public static void unscheduleSync(Context context) {
		PendingIntent pendingIntent = createIntent(context);
		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		manager.cancel(pendingIntent);
	}
	
	private static PendingIntent createIntent(Context context) {
		Intent intent = new Intent(SYNC_ACTION);
		return PendingIntent.getBroadcast(context, SYNC_REQUEST_CODE, 
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
}