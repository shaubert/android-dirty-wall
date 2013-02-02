package com.shaubert.dirty;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.shaubert.dirty.db.BlogsCursor;
import com.shaubert.dirty.db.DirtyContract.DirtyBlogEntity;
import com.shaubert.util.AsyncTasks;
import com.shaubert.util.AsyncTasks.Task;
import com.shaubert.util.SelectableLinkMovementMethod;

public class DirtyBlogView extends FrameLayout implements Checkable {

	private long blogId;
	private boolean isFavorite;
    
    private View blogView;
    private TextView title;
    private TextView description;
    private View frameBody;
    private TextView summary;
    private ImageView favoriteButton;
    
    private DirtyPreferences dirtyPreferences;
    private SummaryFormatter summaryFormatter;
    
    public DirtyBlogView(Context context) {
        super(context);
        
        this.dirtyPreferences = new DirtyPreferences(
                PreferenceManager.getDefaultSharedPreferences(context), context);
        this.summaryFormatter = new SummaryFormatter(getContext());
        
        LayoutInflater inflater = LayoutInflater.from(context);
        blogView = inflater.inflate(R.layout.l_dirty_blog_info, this, true);
        title = (TextView) blogView.findViewById(R.id.title);
        frameBody = blogView.findViewById(R.id.frame_body);
        description = (TextView)blogView.findViewById(R.id.description);
        description.setMovementMethod(SelectableLinkMovementMethod.getInstance());
        description.setClickable(false);
        description.setLongClickable(false);
        summary = (TextView)blogView.findViewById(R.id.summary);
        summary.setMovementMethod(SelectableLinkMovementMethod.getInstance());
        summary.setClickable(false);
        summary.setLongClickable(false);
        favoriteButton = (ImageView)blogView.findViewById(R.id.favorite_button);
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
    
    public void swapData(BlogsCursor blog) {
    	blogId = blog.getId();
    	isFavorite = blog.isFavorite();

        description.setVisibility(VISIBLE);
        summary.setVisibility(VISIBLE);
        favoriteButton.setVisibility(VISIBLE);
    	
    	title.setTextSize(dirtyPreferences.getFontSize());
    	title.setTypeface(null, Typeface.BOLD);
    	title.setText(blog.getTitle());
    	
    	description.setTextSize(dirtyPreferences.getFontSize());
    	SpannableStringBuilder descriptionBuilder = new SpannableStringBuilder(blog.getName());
    	if (!TextUtils.isEmpty(blog.getDescription())) {
	    	descriptionBuilder.append('\n');
	    	descriptionBuilder.append(Html.fromHtml(blog.getDescription()));
    	}
    	description.setText(descriptionBuilder);
		
    	summary.setTextSize(dirtyPreferences.getSummarySize());
        summary.setText(summaryFormatter.formatSummaryText(blog));
        
        refreshFavoriteButton();
    }

    public void setMainPage() {
        blogId = DirtyBlogsAdapter.MAIN_PAGE_ID;
        isFavorite = false;
        description.setVisibility(GONE);
        summary.setVisibility(GONE);
        favoriteButton.setVisibility(GONE);

        title.setTextSize(dirtyPreferences.getFontSize());
        title.setTypeface(null, Typeface.BOLD);
        title.setText(R.string.sub_blog_main);
    }

    private void refreshFavoriteButton() {
        favoriteButton.setImageResource(isFavorite ? R.drawable.star_filled : R.drawable.star_empty);
    }
    
    protected void toggleFavorite() {
        final ContentValues values = new ContentValues();
        //invert value
        values.put(DirtyBlogEntity.FAVORITE, isFavorite ? 0 : 1);
        AsyncTasks.executeInBackground(new Task<Context>(getContext()) {
            @Override
            public void run(Context context) {
                context.getContentResolver().update(ContentUris.withAppendedId(DirtyBlogEntity.URI, blogId), 
                        values, null, null);
            }
        });
        isFavorite = !isFavorite;
        refreshFavoriteButton();
    }
}