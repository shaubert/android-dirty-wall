package com.shaubert.dirty;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;
import com.shaubert.dirty.db.PostsCursor;
import com.shaubert.util.AsyncTasks;
import com.shaubert.util.AsyncTasks.Task;
import com.shaubert.util.SelectableLinkMovementMethod;

public class DirtyPostCompactView extends FrameLayout implements Checkable {

    private long postId;
    private boolean isFavorite;
    
    private View postView;
    private TextView header;
    private View frameBody;
    private TextView message;
    private TextView summary;
    private ImageView favoriteButton;
    private View unreadMark;
    
    private SummaryFormatter summaryFormatter;
    private DirtyPreferences dirtyPreferences;
    
    public DirtyPostCompactView(Context context) {
        super(context);
        
        
        this.summaryFormatter = new SummaryFormatter(context);
        this.dirtyPreferences = new DirtyPreferences(
                PreferenceManager.getDefaultSharedPreferences(context), context);
        
        LayoutInflater inflater = LayoutInflater.from(context);
        postView = inflater.inflate(R.layout.l_dirty_compact_post, this, true);
        header = (TextView) postView.findViewById(R.id.header_text);
        frameBody = postView.findViewById(R.id.frame_body);
        message = (TextView)postView.findViewById(R.id.message);
        message.setMovementMethod(SelectableLinkMovementMethod.getInstance());
        message.setClickable(false);
        message.setLongClickable(false);
        summary = (TextView)postView.findViewById(R.id.summary);
        summary.setMovementMethod(SelectableLinkMovementMethod.getInstance());
        summary.setClickable(false);
        summary.setLongClickable(false);
        favoriteButton = (ImageView)postView.findViewById(R.id.favorite_button);
        favoriteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavorite();
            }
        });
        unreadMark = postView.findViewById(R.id.unread_mark);
        
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
	
    public void swapData(PostsCursor dirtyPost) {
    	postId = dirtyPost.getId();
    	isFavorite = dirtyPost.isFavorite();
    	boolean unread = dirtyPost.isUnread();

        unreadMark.setVisibility(unread ? VISIBLE : GONE);
    	message.setTextSize(dirtyPreferences.getFontSize());
        message.setText(dirtyPost.getMessage());
		summary.setTextSize(dirtyPreferences.getSummarySize());
        summary.setText(summaryFormatter.formatCompactSummaryText(dirtyPost));
        refreshFavoriteButton();
    }

    public void setHeader(CharSequence text) {
    	header.setText(text);
    }
    
    public void setHeaderVisible(boolean visible) {
    	header.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    
    private void refreshFavoriteButton() {
        favoriteButton.setImageResource(isFavorite ? R.drawable.star_filled : R.drawable.star_empty);
    }
    
    protected void toggleFavorite() {
        final ContentValues values = new ContentValues();
        //invert value
        values.put(DirtyPostEntity.FAVORITE, isFavorite ? 0 : 1);
        AsyncTasks.executeInBackground(new Task<Context>(getContext()) {
            @Override
            public void run(Context context) {
                context.getContentResolver().update(ContentUris.withAppendedId(DirtyPostEntity.URI, postId), 
                        values, null, null);
            }
        });
        isFavorite = !isFavorite;
        refreshFavoriteButton();
    }
}
