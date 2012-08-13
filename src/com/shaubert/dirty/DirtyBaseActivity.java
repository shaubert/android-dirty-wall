package com.shaubert.dirty;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Random;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.shaubert.dirty.net.DataLoadRequest;
import com.shaubert.dirty.net.DirtyPostLoadRequest;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.Request;
import com.shaubert.net.nutshell.RequestStatus;
import com.shaubert.util.AsyncTasks;
import com.shaubert.util.AsyncTasks.Task;
import com.shaubert.util.BitmapDecodeTask;
import com.shaubert.util.Files;
import com.shaubert.util.Versions;

@TargetApi(11)
public class DirtyBaseActivity extends JournalBasedFragmentActivity {

    public static final String DIRTY_POST_LOAD_REQUEST_ID = "dirty-post-load-request-id";
    public static final String GERTRUDA_LOAD_REQUEST_ID = "gertruda-load-request-id";
    private static final String GERTRUDA_PATH = "gertruda-filepath";

    private class DirtyPostLoadRequestListener extends DefaultStatusListener {
        @Override
        public void onFinished(Request request) {
            super.onFinished(request);
            RequestStateBase state = (RequestStateBase) request.getState();
            String gertrudaUrl = state.getString(DirtyPostLoadRequest.GERTRUDA_URL_PARAM);
            if (!TextUtils.isEmpty(gertrudaUrl)) {
                startGertrudaRefresh(gertrudaUrl);
            }
            boolean cancelledByTimeThrottle = state.getBoolean(DirtyPostLoadRequest.CANCELLED_BY_TIME_PARAM, false);
            if (!cancelledByTimeThrottle) {
                int newCount = state.getInt(DirtyPostLoadRequest.NEW_POSTS_NUMBER_PARAM, 0);
                showPostLoadedMessage(newCount);
            } else {
                loadGertrudaFromCacheIfNeeded();
            }
            hideRefreshAnimation();
        }
        
        @Override
        public void onError(Request request) {
            super.onError(request);
            showLoadingPostsError();
            hideRefreshAnimation();
            loadGertrudaFromCacheIfNeeded();
        }
    }
    
    private class GertrudaLoadRequestListener extends DefaultStatusListener {
     
        @Override
        public void onFinished(Request request) {
            super.onFinished(request);
            loadGertrudaFromFile(((RequestStateBase) request.getState()).getString("file"));
        }
        
    }

    protected long dirtyPostLoadRequestId;
    protected long gertrudaLoadRequestId;
    protected DataLoadRequest gertrudaLoadRequest;
    private BitmapDecodeTask bitmapDecodeTask;
    private String gertrudaPath;

    private View petr;
    private AnimationDrawable petrBackgroung;
    private Animation petrMovement;
    
    private View gertrudaBox;
    private ImageView gertruda;
    
    protected View dirtyTv;
    private AnimationDrawable dirtyTvAnimation;
    
    protected DirtyPreferences dirtyPreferences;
    protected DirtyPostLoadRequest dirtyPostLoadRequest;
    
    protected DirtyMessagesProvider dirtyMessagesProvider;
    protected MenuItem refreshMenuItem;
	private MenuItem favoriteMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Versions.isApiLevelAvailable(11)) {
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }
        dirtyPreferences = new DirtyPreferences(
                PreferenceManager.getDefaultSharedPreferences(this), this);
        dirtyMessagesProvider = DirtyMessagesProvider.getInstance(getApplicationContext());
        
        setContentView(R.layout.l_dirty_base_activity);
    }
    
    @Override
    public void onContentChanged() {
    	super.onContentChanged();
        petr = findViewById(R.id.petr);
        petrBackgroung = (AnimationDrawable)petr.getBackground();
        
        gertrudaBox = findViewById(R.id.gertruda_box);
        gertruda = (ImageView)gertrudaBox.findViewById(R.id.gertruda);
        
        dirtyTv = findViewById(R.id.empty_view_tv);
        dirtyTvAnimation = (AnimationDrawable) dirtyTv.getBackground();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            if (dirtyPreferences.isPetrEnabled()) {
                petrBackgroung.start();
                petrBackgroung.stop();
                petrBackgroung.start();
            }
            dirtyTvAnimation.start();
        }
    }
    
    public void startGertrudaRefresh(String gertrudaUrl) {
        if (gertrudaLoadRequest == null || (
                RequestStatus.isWaitingOrProcessing(gertrudaLoadRequest.getState().getStatus()) 
                && !gertrudaUrl.equals(gertrudaLoadRequest.getState().getString("url")))) {
            File file = Files.getGertrudaFile(this, gertrudaUrl);
            gertrudaPath = file.getAbsolutePath();
            if (!file.exists()) {
                gertrudaLoadRequest = new DataLoadRequest(gertrudaUrl, gertrudaPath);
                gertrudaLoadRequest.setFullStateChangeListener(new GertrudaLoadRequestListener());
                startRequest(gertrudaLoadRequest);
            } else {
                loadGertrudaFromFile(gertrudaPath);
            }
        }
    }

    public void loadGertrudaFromFile(String fileName) {
        if (bitmapDecodeTask != null) {
            bitmapDecodeTask.cancel(true);
        }
        bitmapDecodeTask = new BitmapDecodeTask() {
            @Override
            protected void onImageLoaded(Bitmap bitmap) {
                gertruda.setImageBitmap(bitmap);
                if (gertrudaBox.getVisibility() != View.VISIBLE) {
                    gertrudaBox.setVisibility(View.VISIBLE);
                    gertrudaBox.startAnimation(AnimationUtils.makeInAnimation(getApplicationContext(), true));
                }
            }
        };
        bitmapDecodeTask.execute(new File(fileName));
    }

    public void loadGertrudaFromCacheIfNeeded() {
        if (TextUtils.isEmpty(gertrudaPath)) {
            AsyncTasks.executeInBackground(new Task<Activity>(this) {
                String filename;
                @Override
                public void run(Activity context) {
                    File dir = Files.getGertrudaDir(context);
                    String[] images = dir.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            String name = filename.toLowerCase();
                            return name.endsWith("png") || name.endsWith("jpg") || name.endsWith("jpeg") || name.endsWith("gif");
                        }
                    });
                    if (images.length > 0) {
                        filename = dir.getAbsolutePath() + "/" + images[new Random().nextInt(images.length)];
                    }
                }
                
                @Override
                public void onUiThread(Activity context) {
                    if (!TextUtils.isEmpty(filename)) {
                        loadGertrudaFromFile(filename);
                    }
                }
            });
        }
    }

    @Override
    public void onStartInitialRequests() {
        super.onStartInitialRequests();
        
    	dirtyPostLoadRequestId = getIntent().getLongExtra(DIRTY_POST_LOAD_REQUEST_ID, -1);
    	gertrudaLoadRequestId = getIntent().getLongExtra(GERTRUDA_LOAD_REQUEST_ID, -1);
    	restoreRequests();
        startLoadPostsRequestIfNotStarted(false);
    }

    @Override
    public void onRestoreRequestState(Bundle savedInstanceState) {
        super.onRestoreRequestState(savedInstanceState);
        dirtyPostLoadRequestId = savedInstanceState.getLong(DIRTY_POST_LOAD_REQUEST_ID, -1);
        gertrudaLoadRequestId = savedInstanceState.getLong(GERTRUDA_LOAD_REQUEST_ID, -1);
        restoreRequests();
        
        gertrudaPath = savedInstanceState.getString(GERTRUDA_PATH);
        if (!TextUtils.isEmpty(gertrudaPath)) {
        	loadGertrudaFromFile(gertrudaPath);
        } else {
        	loadGertrudaFromCacheIfNeeded();
        }
    }

	private void restoreRequests() {
		if (dirtyPostLoadRequestId > 0) {
        	dirtyPostLoadRequest = restoreAndRegisterIfNotFinished(dirtyPostLoadRequestId);
        	dirtyPostLoadRequest.setFullStateChangeListener(new DirtyPostLoadRequestListener());
        }
        if (gertrudaLoadRequestId > 0) {
            gertrudaLoadRequest = restoreAndRegisterIfNotFinished(gertrudaLoadRequestId);
            GertrudaLoadRequestListener changeListener = new GertrudaLoadRequestListener();
            gertrudaLoadRequest.setFullStateChangeListener(changeListener);
        }
	}
	
	protected Intent attachRequestsIds(Intent intent) {
		intent.putExtra(DIRTY_POST_LOAD_REQUEST_ID, dirtyPostLoadRequestId);
		intent.putExtra(GERTRUDA_LOAD_REQUEST_ID, gertrudaLoadRequestId);
		return intent;
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DIRTY_POST_LOAD_REQUEST_ID, dirtyPostLoadRequestId);
        outState.putLong(GERTRUDA_LOAD_REQUEST_ID, gertrudaLoadRequestId);
        outState.putString(GERTRUDA_PATH, gertrudaPath);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (dirtyPreferences.isPetrEnabled()) {
            if (petr.getAnimation() == null) {
                petrMovement = AnimationUtils.loadAnimation(this, R.anim.petr_movement);
                petr.startAnimation(petrMovement);
            }
            petr.setVisibility(View.VISIBLE);
        } else {
            petr.setVisibility(View.GONE);
            petr.clearAnimation();
        }
        
        refreshFavoriteMenuItem();
    }

    protected void startLoadPostsRequestIfNotStarted(boolean force) {
        if (dirtyPostLoadRequest == null || dirtyPostLoadRequest.isCancelled() 
                || isFinished(dirtyPostLoadRequest)) {
            dirtyPostLoadRequest = new DirtyPostLoadRequest();
            dirtyPostLoadRequest.getState().put(DirtyPostLoadRequest.FORCE_PARAM, force);
            dirtyPostLoadRequest.setFullStateChangeListener(new DirtyPostLoadRequestListener());
            dirtyPostLoadRequestId = startRequest(dirtyPostLoadRequest);
            showRefreshAnimation();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        petrBackgroung.stop();
        dirtyTvAnimation.stop();
        
        if (isFinishing()) {
        	if (dirtyPostLoadRequest != null) {
        		unregisterForUpdates(dirtyPostLoadRequest);
        	}
        	if (gertrudaLoadRequest != null) {
        		unregisterForUpdates(gertrudaLoadRequest);
        	}
        }
    }
    
    public void showRefreshAnimation() {
        if (!Versions.isApiLevelAvailable(11)) {
            setProgressBarIndeterminate(true);
            setProgressBarIndeterminateVisibility(true);
        } else if (refreshMenuItem != null) {
            AnimationDrawable animationDrawable = (AnimationDrawable)getResources().getDrawable(R.drawable.ic_popup_sync);
            refreshMenuItem.setIcon(animationDrawable);
            animationDrawable.start();
            animationDrawable.stop();
            animationDrawable.start();
        }
    }
    
    public void hideRefreshAnimation() {
        if (!Versions.isApiLevelAvailable(11)) {
            setProgressBarIndeterminateVisibility(false);
        } else if (refreshMenuItem != null) {
            refreshMenuItem.setIcon(R.drawable.ic_menu_refresh);
        }
    }
    
    public void showPostLoadedMessage(int newCount) {
        if (newCount > 0) {
            DirtyToast.show(this, dirtyMessagesProvider.getRandomFaceImageId(), 
                    dirtyMessagesProvider.getMessageForNewPosts(newCount));
        }
    }

    public void showLoadingPostsError() {
        DirtyToast.show(this, dirtyMessagesProvider.getRandomFaceImageId(), 
                dirtyMessagesProvider.getErrorMessage());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main_menu, menu);
        favoriteMenuItem = menu.findItem(R.id.favorites_menu_item);
		refreshFavoriteMenuItem();
        this.refreshMenuItem = menu.findItem(R.id.refresh_posts_menu_item);
        showRefreshAnimationIfNeeded();
        return super.onCreateOptionsMenu(menu);
    }

    private void showRefreshAnimationIfNeeded() {
        if (dirtyPostLoadRequest != null 
                && RequestStatus.isWaitingOrProcessing(dirtyPostLoadRequest.getState().getStatus())) {
            showRefreshAnimation();
        }
    }
        
    private void refreshFavoriteMenuItem() {
    	if (favoriteMenuItem != null) { 
	        if (dirtyPreferences.isShowingOnlyFavorites()) {
	            favoriteMenuItem.setTitle(R.string.show_all);
	            favoriteMenuItem.setIcon(R.drawable.ic_menu_show_only_favorites);
	        } else {
	            favoriteMenuItem.setTitle(R.string.only_favorites);
	            favoriteMenuItem.setIcon(R.drawable.ic_menu_show_all);
	        }
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {        
            case R.id.refresh_posts_menu_item:                
                startLoadPostsRequestIfNotStarted(true);
                return true;
                
            case R.id.favorites_menu_item:                
                toggleShowFavorites();
                refreshFavoriteMenuItem();
                return true;
                
            case R.id.settings_menu_item:
                openSettings();
                return true;
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    protected void toggleShowFavorites() {
        boolean show = !dirtyPreferences.isShowingOnlyFavorites();
        dirtyPreferences.setShowOnlyFavorites(show);
    }
    
}