package com.shaubert.dirty;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.shaubert.dirty.JournalBasedFragmentActivity.DefaultStatusListener;
import com.shaubert.dirty.net.DataLoadRequest;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.Request;
import com.shaubert.net.nutshell.RequestStatus;
import com.shaubert.util.AsyncTasks;
import com.shaubert.util.AsyncTasks.Task;
import com.shaubert.util.BitmapDecodeTask;
import com.shaubert.util.Files;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Random;

public class Gertruda {

	public static final String GERTRUDA_LOAD_REQUEST_ID = "gertruda-load-request-id";
	private static final String GERTRUDA_PATH = "gertruda-filepath";
	
    private class GertrudaLoadRequestListener extends DefaultStatusListener {
        public GertrudaLoadRequestListener(JournalBasedFragmentActivity activity) {
			super(activity);
		}

		@Override
        public void onFinished(Request request) {
            super.onFinished(request);
            loadGertrudaFromFile(((RequestStateBase) request.getState()).getString("file"));
        }
        
    }
	
	protected long gertrudaLoadRequestId;
    protected DataLoadRequest gertrudaLoadRequest;
    private BitmapDecodeTask bitmapDecodeTask;
    private String gertrudaPath;
	
    private View gertrudaBox;
    private ImageView gertruda;

    private String subBlog;
    
    private JournalBasedFragmentActivity activity;
    
    public Gertruda(JournalBasedFragmentActivity activity) {
		this.activity = activity;
        findUiElements();
	}

    public String getSubBlog() {
        return subBlog;
    }

    public void setSubBlog(String subBlog) {
        if (!TextUtils.equals(subBlog, this.subBlog)) {
            this.subBlog = subBlog;
            this.gertrudaPath = null;

            if (gertrudaLoadRequest != null) {
                gertrudaLoadRequest.cancel();
            }
        }
    }

    private void findUiElements() {
		gertrudaBox = activity.findViewById(R.id.gertruda_box);
		if (gertrudaBox != null) {
			gertruda = (ImageView)gertrudaBox.findViewById(R.id.gertruda);
		}
	}

	public void startGertrudaRefresh(String gertrudaUrl) {
        if (gertrudaLoadRequest == null || (
                RequestStatus.isWaitingOrProcessing(gertrudaLoadRequest.getState().getStatus()) 
                && !gertrudaUrl.equals(gertrudaLoadRequest.getState().getString("url")))) {
            File file = Files.getGertrudaFile(activity, subBlog, gertrudaUrl);
            gertrudaPath = file.getAbsolutePath();
            if (!file.exists()) {
                gertrudaLoadRequest = new DataLoadRequest(gertrudaUrl, gertrudaPath);
                gertrudaLoadRequest.setFullStateChangeListener(new GertrudaLoadRequestListener(activity));
                activity.startRequest(gertrudaLoadRequest);
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
            	if (gertruda == null || gertrudaBox == null) {
            		findUiElements();
            	}
                gertruda.setImageBitmap(bitmap);
                if (gertrudaBox.getVisibility() != View.VISIBLE) {
                    gertrudaBox.setVisibility(View.VISIBLE);
                    gertrudaBox.startAnimation(AnimationUtils.makeInAnimation(
                    		activity, true));
                }
            }
        };
        bitmapDecodeTask.execute(new File(fileName));
    }

    public void loadGertrudaFromCacheIfNeeded() {
        if (TextUtils.isEmpty(gertrudaPath)) {
            AsyncTasks.executeInBackground(new Task<Activity>(activity) {
                String filename;
                @Override
                public void run(Activity context) {
                    File dir = Files.getGertrudaDir(context, subBlog);
                    String[] images = dir.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            String name = filename.toLowerCase();
                            return name.endsWith("png") || name.endsWith("jpg") || name.endsWith("jpeg") || name.endsWith("gif");
                        }
                    });
                    if (images != null && images.length > 0) {
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
	
    public void onStartInitialRequests() {
    	gertrudaLoadRequestId = activity.getIntent().getLongExtra(GERTRUDA_LOAD_REQUEST_ID, -1);
    	restoreRequests();
    }

    public void onRestoreRequestState(Bundle savedInstanceState) {
        gertrudaLoadRequestId = savedInstanceState.getLong(GERTRUDA_LOAD_REQUEST_ID, -1);
        restoreRequests();
        
        gertrudaPath = savedInstanceState.getString(GERTRUDA_PATH);
        if (!TextUtils.isEmpty(gertrudaPath)) {
        	loadGertrudaFromFile(gertrudaPath);
        } else {
        	loadGertrudaFromCacheIfNeeded();
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(GERTRUDA_LOAD_REQUEST_ID, gertrudaLoadRequestId);
        outState.putString(GERTRUDA_PATH, gertrudaPath);
    }
    
	private void restoreRequests() {
        if (gertrudaLoadRequestId > 0) {
            gertrudaLoadRequest = activity.restoreAndRegisterIfNotFinished(gertrudaLoadRequestId);
            GertrudaLoadRequestListener changeListener = new GertrudaLoadRequestListener(activity);
            gertrudaLoadRequest.setFullStateChangeListener(changeListener);
        }
	}
	
	public void onPause() {
		if (activity.isFinishing()) {
			if (gertrudaLoadRequest != null) {
        		activity.unregisterForUpdates(gertrudaLoadRequest);
        	}
		}
	}
	
	public long getGertrudaLoadRequestId() {
		return gertrudaLoadRequestId;
	}
}
