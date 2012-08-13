package com.shaubert.dirty;

import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.dirty.net.DataLoadRequest;
import com.shaubert.net.core.RequestBase;
import com.shaubert.net.core.RequestStatusListener;
import com.shaubert.net.nutshell.Request;
import com.shaubert.net.nutshell.RequestStatus;
import com.shaubert.util.AsyncTasks;
import com.shaubert.util.AsyncTasks.Task;
import com.shaubert.util.BitmapDecodeTask;
import com.shaubert.util.Networks;
import com.shaubert.util.SelectableLinkMovementMethod;
import com.shaubert.util.Sizes;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;

public class DirtyPostView extends FrameLayout implements Checkable {

    public interface OnCommentLoadClickListener {
        void onCommentLoadClicked(DirtyPostView dirtyPostPresenter);
    }
    
    private static class ImageLoaderRequestListener extends RequestStatusListener {
        private JournalBasedFragmentActivity activity;
        
        public ImageLoaderRequestListener(JournalBasedFragmentActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onFinished(Request request) {
            activity.unregisterForUpdates((RequestBase)request);
            final long rPostId = ((RequestBase) request).getState().getLong("postId", -1);
            activity.getContentResolver().notifyChange(ContentUris.withAppendedId(DirtyPostEntity.URI, rPostId), null);
        }
        
        @Override
        public void onError(Request request) {
            activity.unregisterForUpdates((RequestBase)request);
        }
    }
    
    private static final String IMAGE_LOADER_REQUEST_ID = "image_loader_request_id";
    
    private DirtyPost dirtyPost;
    private final long postId;
    
    private View postView;
    private View frameBody;
    private TextView message;
    private TextView summary;
    private View loadOrRefreshComments;
    private ImageButton favoriteButton;
    
    private OnCommentLoadClickListener commentLoadClickListener;
    
    private DataLoadRequest imageLoaderRequest;
    private long imageLoaderRequestId;
    private boolean requestRestoreFinished;
    private String currentImagePath;
    private BitmapDecodeTask bitmapDecodeTask;
    private Bitmap bitmap;
    
    private final JournalBasedFragmentActivity activity;
    private boolean released;
    
    private SummaryFormatter summaryFormatter;

    private DirtyPreferences dirtyPreferences;
    
    public DirtyPostView(JournalBasedFragmentActivity activity, long postId) {
        super(activity);
        
        this.activity = activity;
        this.postId = postId;
        
        this.summaryFormatter = new SummaryFormatter(activity);
        this.dirtyPreferences = new DirtyPreferences(
                PreferenceManager.getDefaultSharedPreferences(activity), activity);
        
        LayoutInflater inflater = LayoutInflater.from(activity);
        postView = inflater.inflate(R.layout.l_dirty_post, this, true);
        frameBody = postView.findViewById(R.id.frame_body);
        message = (TextView)postView.findViewById(R.id.message);
        message.setMovementMethod(SelectableLinkMovementMethod.getInstance());
        message.setClickable(false);
        message.setLongClickable(false);
        summary = (TextView)postView.findViewById(R.id.summary);
        summary.setMovementMethod(SelectableLinkMovementMethod.getInstance());
        summary.setClickable(false);
        summary.setLongClickable(false);
        loadOrRefreshComments = postView.findViewById(R.id.load_refresh_comments);
        loadOrRefreshComments.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (commentLoadClickListener != null) {
                    commentLoadClickListener.onCommentLoadClicked(DirtyPostView.this);
                }
            }
        });
        favoriteButton = (ImageButton)postView.findViewById(R.id.favorite_button);
        favoriteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavorite();
            }
        });        
        
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }
    
    @Override
    public void setChecked(boolean checked) {
        setSelected(checked);
    }
    
    @Override
    public boolean isChecked() {
        return isSelected();
    }
    
    @Override
    public void toggle() {
        if (isChecked()) {
            setChecked(true);
        } else {
            setChecked(false);
        }
    }
    
    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        StateListDrawable stateListDrawable = (StateListDrawable)frameBody.getBackground();
        if (stateListDrawable.getCurrent() instanceof TransitionDrawable) {
            TransitionDrawable transition = (TransitionDrawable)stateListDrawable.getCurrent();
            if (pressed) {
                transition.startTransition(ViewConfiguration.getLongPressTimeout());
            } else {
                transition.resetTransition();
            }
        }
    }
    
    public void setCommentLoadClickListener(OnCommentLoadClickListener commentLoadClickListener) {
        this.commentLoadClickListener = commentLoadClickListener;
    }
    
    public void setLoadOrRefreshButtonEnabled(boolean enabled) {
        loadOrRefreshComments.setEnabled(enabled);
    }
    
    public void restoreRequestState(Bundle savedInstanceState) {
        imageLoaderRequestId = savedInstanceState.getLong(IMAGE_LOADER_REQUEST_ID, -1);
    }
    
    public void resume() {
        refreshContent();
        AsyncTasks.executeInBackground(new Task<JournalBasedFragmentActivity>(activity) {
            public void run(JournalBasedFragmentActivity activity) {
                if (imageLoaderRequestId > 0) {
                    if (imageLoaderRequest == null) {
                        imageLoaderRequest = activity.restore(imageLoaderRequestId);
                        long rPostId = imageLoaderRequest.getState().getLong("postId", -1);
                        if (rPostId == postId) {
                            ImageLoaderRequestListener changeListener = new ImageLoaderRequestListener(activity);
                            imageLoaderRequest.setFullStateChangeListener(changeListener);
                        } else {
                            imageLoaderRequestId = 0;
                            imageLoaderRequest = null;
                        }
                    }
                }
                requestRestoreFinished = true;
            }
            
            @Override
            public void onUiThread(JournalBasedFragmentActivity context) {
                if (!released) {
                    if (imageLoaderRequest != null) {
                        context.registerForUpdates(imageLoaderRequest);
                    }
                    if (dirtyPost != null) {
                        processPostImages();
                    }
                }
            }
        });
    }

    public void pause() {
        if (imageLoaderRequest != null) {
            activity.unregisterForUpdates(imageLoaderRequest);
        }
    }
    public void saveInstanceState(Bundle outState) {
        outState.putLong(IMAGE_LOADER_REQUEST_ID, imageLoaderRequestId);
    }
    
    public void release() {
        if (bitmapDecodeTask != null) {
            bitmapDecodeTask.cancel(true);
        }
        if (bitmap != null) {
            bitmap.recycle();
        }
    }

    public void setDirtyPost(DirtyPost dirtyPost) {
        this.dirtyPost = dirtyPost;
        refreshContent();
    }

    private void refreshContent() {
        if (dirtyPost != null) {
            message.setText(dirtyPost.getSpannedText());
            message.setTextSize(dirtyPreferences.getFontSize());
            summary.setText(summaryFormatter.formatSummaryText(dirtyPost));
            summary.setTextSize(dirtyPreferences.getSummarySize());
            refreshFavoriteButton();
            processPostImages();
        }
    }

    private void refreshFavoriteButton() {
        favoriteButton.setImageResource(dirtyPost.isFavorite() ? R.drawable.star_filled : R.drawable.star_empty);
    }
    
    private void processPostImages() {
        File imagePath = dirtyPost.getCachedImagePath();
        if (imagePath != null && requestRestoreFinished) {
            if (!imagePath.exists() && (imageLoaderRequest == null || imageLoaderRequest.isCancelled())) {
            	if (dirtyPreferences.shouldLoadImagesOnlyWithWiFi()) {
            		if (!Networks.hasWiFiConnection(activity)) {
            			return;
            		}
            	}
                imageLoaderRequest = new DataLoadRequest(dirtyPost.getImages()[0].src, dirtyPost.getCachedImagePath().getAbsolutePath());
                imageLoaderRequest.getState().put("postId", postId);
                imageLoaderRequest.setFullStateChangeListener(new ImageLoaderRequestListener(activity));
                imageLoaderRequestId = activity.startRequest(imageLoaderRequest);
            } else {
                String path = imagePath.getAbsolutePath();
                if (imageLoaderRequest != null && imageLoaderRequest.getState().getStatus() == RequestStatus.FINISHED_WITH_ERRORS) {
                    showImageLoadingError();
                } else if (!path.equals(currentImagePath)) {
                    loadImage(path);
                } else if (bitmap != null) {
                    setImage(bitmap);
                }
            }
        }
    }

    protected void toggleFavorite() {
        final ContentValues values = new ContentValues();
        //invert value
        values.put(DirtyPostEntity.FAVORITE, dirtyPost.isFavorite() ? 0 : 1);
        AsyncTasks.executeInBackground(new Task<Context>(getContext()) {
            @Override
            public void run(Context context) {
                context.getContentResolver().update(ContentUris.withAppendedId(DirtyPostEntity.URI, postId), 
                        values, null, null);
            }
        });
        dirtyPost.setFavorite(!dirtyPost.isFavorite());
        refreshFavoriteButton();
    }
    
    private void showImageLoadingError() {
    }
    
    private void showImageDecodingError() {
        if (!released) {
            
        }
    }

    private void loadImage(String imagePath) {
        if (bitmapDecodeTask != null) {
            bitmapDecodeTask.cancel(true);
        }
        currentImagePath = imagePath;
        bitmapDecodeTask = new BitmapDecodeTask(Sizes.dpToPx(256, message.getContext()), Sizes.dpToPx(256, message.getContext())) {
            @Override
            protected void onImageLoaded(Bitmap bitmap) {
                setImage(bitmap);
            }
            
            @Override
            protected void onLoadError() {
                super.onLoadError();
                currentImagePath = null;
                showImageDecodingError();
            }
        };
        bitmapDecodeTask.execute(new File(imagePath));        
    }

    
    protected void setImage(Bitmap bitmap) {
        if (!released) {
            Spanned text = (Spanned)message.getText();
            if (text.getSpans(0, text.length(), ImageSpan.class).length == 0) {
                this.bitmap = bitmap;
                SpannableStringBuilder builder = new SpannableStringBuilder(text);
                
                URLSpan linkSpan = null;
                if (!TextUtils.isEmpty(dirtyPost.getVideoUrl())) {
                    linkSpan = new URLSpan(dirtyPost.getVideoUrl());
                } else {
                    linkSpan = new URLSpan(dirtyPost.getImages()[0].src);
                }
                        
                RelativeSizeSpan[] spans = builder.getSpans(0, 1, RelativeSizeSpan.class);
                if (spans != null && spans.length > 0) {
                    int end = builder.getSpanEnd(spans[0]);
                    builder.insert(end + 1, "\n \n");
                    ImageSpan imageSpan = new ImageSpan(activity, bitmap, ImageSpan.ALIGN_BOTTOM);
                    builder.setSpan(imageSpan, end + 2, end + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.setSpan(linkSpan, end + 2, end + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    builder.insert(0, " \n");
                    ImageSpan imageSpan = new ImageSpan(activity, bitmap, ImageSpan.ALIGN_BOTTOM);
                    builder.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.setSpan(linkSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                message.setText(builder);
            }
        } else {
            bitmap.recycle();
        }
    }
}
