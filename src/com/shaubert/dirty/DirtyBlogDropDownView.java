package com.shaubert.dirty;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.shaubert.dirty.db.BlogsCursor;

public class DirtyBlogDropDownView extends FrameLayout {

	private long blogId;

    private View blogView;
    private TextView title;
    private TextView summary;

    private DirtyPreferences dirtyPreferences;
    private SummaryFormatter summaryFormatter;

    public DirtyBlogDropDownView(Context context) {
        super(context);
        
        this.dirtyPreferences = new DirtyPreferences(
                PreferenceManager.getDefaultSharedPreferences(context), context);
        this.summaryFormatter = new SummaryFormatter(getContext());
        
        LayoutInflater inflater = LayoutInflater.from(context);
        blogView = inflater.inflate(R.layout.l_dirty_blog_dropdown, this, true);
        title = (TextView) blogView.findViewById(R.id.title);
        summary = (TextView)blogView.findViewById(R.id.summary);
    }

    public void swapData(BlogsCursor blog) {
    	blogId = blog.getId();

    	title.setText(blog.getTitle());
        summary.setText(summaryFormatter.formatSummaryText(blog));
        summary.setVisibility(VISIBLE);
    }

    public void setMainPage() {
        blogId = FavoriteBlogsDropDownAdapter.MAIN_PAGE_ID;

        title.setText(R.string.sub_blog_main);
        summary.setVisibility(GONE);
    }

    public void hideSummary() {
        summary.setVisibility(GONE);
    }
}