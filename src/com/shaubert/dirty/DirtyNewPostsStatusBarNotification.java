package com.shaubert.dirty;

import com.shaubert.util.Bitmaps;
import com.shaubert.util.Versions;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class DirtyNewPostsStatusBarNotification {

	public static final int PENDING_INTENT_REQUEST_CODE = 119922;
	public static final int NEW_POSTS_NOTIFICATION_ID = 4310;
	
	private NotificationManager notificationManager;
	private final Context context;
	private DirtyMessagesProvider dirtyMessagesProvider;
	private DirtyPreferences dirtyPreferences;
	private int totalNewPostsCount;
	
	public DirtyNewPostsStatusBarNotification(Context context, int newPostsCount) {
		this.context = context;
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		this.dirtyMessagesProvider = DirtyMessagesProvider.getInstance(context);
		this.dirtyPreferences = new DirtyPreferences(
				PreferenceManager.getDefaultSharedPreferences(context), context);
		
		this.totalNewPostsCount = dirtyPreferences.getNewPostsCount() + newPostsCount;
		dirtyPreferences.setNewPostsCount(totalNewPostsCount);
	}
	
	public void post() {
		long when = System.currentTimeMillis();

		Intent notificationIntent = new Intent(context, PostsListActivity.class);
		notificationIntent.putExtra(PostsListActivity.EXTRA_FROM_NOTIFICATION, true);
		PendingIntent contentIntent = PendingIntent.getActivity(context, PENDING_INTENT_REQUEST_CODE, 
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		builder
			.setAutoCancel(true)
			.setSmallIcon(R.drawable.ic_stat_dirty_icon)
			.setContentTitle(dirtyMessagesProvider.getNewPostsString(totalNewPostsCount))
			.setContentText(dirtyMessagesProvider.getMessageForNewPosts(totalNewPostsCount))
			.setContentIntent(contentIntent)
			.setDefaults(Notification.DEFAULT_SOUND)
			.setNumber(totalNewPostsCount)
			.setOnlyAlertOnce(true)
			.setWhen(when);
		
		if (Versions.isApiLevelAvailable(11)) {
			int maxWidht = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
			int maxHeight = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
			Bitmap largeIcon = Bitmaps.resizeBitmap(
					BitmapFactory.decodeResource(context.getResources(),
							dirtyMessagesProvider.getRandomFaceImageId()), maxWidht, maxHeight);
			builder.setLargeIcon(largeIcon);
		}
		
		Notification notification = builder.getNotification();
		notificationManager.notify(NEW_POSTS_NOTIFICATION_ID, notification);
	}
	
}
