package com.shaubert.dirty;

import com.shaubert.dirty.client.DirtyBlog;
import com.shaubert.dirty.client.DirtyPost;
import com.shaubert.dirty.db.BlogsCursor;
import com.shaubert.dirty.db.CommentsCursor;
import com.shaubert.dirty.db.PostsCursor;
import com.shaubert.util.Dates;
import com.shaubert.util.PluralHelper;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SummaryFormatter {

    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private final Context context;

    
    public SummaryFormatter(Context context) {
        this.context = context;
        
        dateFormat = new SimpleDateFormat(context.getString(R.string.post_date_format));
        dateFormat.setTimeZone(Dates.GMT);
        timeFormat = new SimpleDateFormat(context.getString(R.string.post_time_format));
        timeFormat.setTimeZone(Dates.GMT);
    }
    
    public String formatCreationDate(Date creationDate, int formatId) {
        return formatCreationDate(creationDate, formatId, false);
    }
    
    public String formatCreationDate(Date creationDate, int formatId, boolean fullDate) {
        final String dateStr;
        final String timeStr;
        if (creationDate.getTime() > 0) {
            if (!fullDate && Dates.isToday(creationDate, Dates.GMT)) {
                dateStr = context.getString(R.string.today).toLowerCase();
            } else if (!fullDate && Dates.isYesterday(creationDate, Dates.GMT)) {
                dateStr = context.getString(R.string.yesterday).toLowerCase();
            } else {
                dateStr = dateFormat.format(creationDate);
            }
            timeStr = timeFormat.format(creationDate);
        } else {
            dateStr = "?";
            timeStr = "?";
        }
        return String.format(context.getString(formatId), dateStr, timeStr);
    }
    
    public String formatComments(int count) {
        int commentsResFormat = R.string.many_comments;
        switch (PluralHelper.getForm(count)) {
            case ONE:
                commentsResFormat = R.string.one_comment;
                break;
            case FEW:
                commentsResFormat = R.string.few_comments;
                break;
        }
        return String.format(context.getResources().getString(commentsResFormat), count);
    }
    
    public String formatVotes(int count) {
        int votesResFormat = R.string.many_votes;
        switch (PluralHelper.getForm(count)) {
            case ONE:
                votesResFormat = R.string.one_vote;
                break;
            case FEW:
                votesResFormat = R.string.few_votes;
                break;
        }
        return String.format(context.getResources().getString(votesResFormat), count);
    }
    
    public CharSequence formatSummaryText(DirtyPost dirtyPost) {
        String summaryFormat = context.getString(R.string.post_summary_format);
        String author = dirtyPost.getAuthor();
        String subBlogUrl = TextUtils.isEmpty(dirtyPost.getSubBlogName()) ? "d3.ru" : dirtyPost.getSubBlogName();
        String text = String.format(summaryFormat, author,
        		subBlogUrl,
                formatCreationDate(new Date(dirtyPost.getCreationDateAsMillis()), R.string.creation_date_format), 
                formatComments(dirtyPost.getCommentsCount()),
                formatVotes(dirtyPost.getVotesCount()));
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int authorIndex = text.indexOf(author);
        builder.setSpan(new URLSpan(dirtyPost.getAuthorLink()), authorIndex, authorIndex + author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!TextUtils.isEmpty(dirtyPost.getSubBlogName())) {
        	int subBlogIndex = text.indexOf(subBlogUrl);
        	builder.setSpan(new URLSpan("http://" + subBlogUrl), subBlogIndex, subBlogIndex + subBlogUrl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (dirtyPost.isGolden()) {
            builder.append("\u00A0\u00A0");
            builder.setSpan(new ImageSpan(context, R.drawable.stars, DynamicDrawableSpan.ALIGN_BASELINE), 
                    builder.length() - 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    public CharSequence formatCompactSummaryText(PostsCursor dirtyPost) {
        String summaryFormat = context.getString(R.string.post_summary_format);
        String author = dirtyPost.getAuthor();
        String subBlogUrl = TextUtils.isEmpty(dirtyPost.getSubBlogName()) ? "d3.ru" : dirtyPost.getSubBlogName();
        String text = String.format(summaryFormat, author, subBlogUrl,
                formatCreationDate(dirtyPost.getCreationDate(), R.string.creation_date_format), 
                formatComments(dirtyPost.getCommentsCount()),
                formatVotes(dirtyPost.getVotesCount()));
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int authorIndex = text.indexOf(author);
        builder.setSpan(new URLSpan(dirtyPost.getAuthorLink()), authorIndex, authorIndex + author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!TextUtils.isEmpty(dirtyPost.getSubBlogName())) {
        	int subBlogIndex = text.indexOf(subBlogUrl);
        	builder.setSpan(new URLSpan("http://" + subBlogUrl), subBlogIndex, subBlogIndex + subBlogUrl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (dirtyPost.isGolden()) {
            builder.append("\u00A0\u00A0");
            builder.setSpan(new ImageSpan(context, R.drawable.stars, DynamicDrawableSpan.ALIGN_BASELINE), 
                    builder.length() - 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }
    
    public String formatSummaryTextForExport(DirtyPost dirtyPost) {
        String summaryFormat = context.getString(R.string.post_summary_format);
        String author = dirtyPost.getAuthor();
        String subBlogUrl = TextUtils.isEmpty(dirtyPost.getSubBlogName()) ? "d3.ru" : dirtyPost.getSubBlogName();
        String text = String.format(summaryFormat, author,
                subBlogUrl,
                formatCreationDate(new Date(dirtyPost.getCreationDateAsMillis()), R.string.creation_date_format),
                formatComments(dirtyPost.getCommentsCount()),
                formatVotes(dirtyPost.getVotesCount()));
        if (dirtyPost.isGolden()) {
            text += "\u00A0\u00A0\u2605\u2605\u2605\u2605\u2605";//five black stars
        }
        return text;
    }
    
    public CharSequence formatSummaryText(CommentsCursor comment) {
        String summaryFormat = context.getString(R.string.comment_summary_format);
        String author = comment.getAuthor();
        String text = String.format(summaryFormat, author,
                formatCreationDate(comment.getCreationDate(), R.string.creation_date_format), 
                formatVotes(comment.getVotesCount()));
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int authorIndex = text.indexOf(author);
        builder.setSpan(new URLSpan(comment.getAuthorLink()), authorIndex, authorIndex + author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
 
    public String formatSubscribers(int count) {
        int subscrResFormat = R.string.many_subscribers;
        switch (PluralHelper.getForm(count)) {
            case ONE:
                subscrResFormat = R.string.one_subscriber;
                break;
            case FEW:
                subscrResFormat = R.string.few_subscribers;
                break;
        }
        return String.format(context.getResources().getString(subscrResFormat), count);
    }
    
    public CharSequence formatSummaryText(BlogsCursor blog) {
    	String summaryFormat = context.getString(R.string.blog_summary_format);
        String blogUrl = blog.getUrl();
        String author = blog.getAuthor();
        String text = String.format(summaryFormat, blogUrl, author,
                formatSubscribers(blog.getReadersCount()));
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int blogUrlIndex = text.indexOf(blogUrl);
        builder.setSpan(new URLSpan("http://" + blogUrl), blogUrlIndex, blogUrlIndex + blogUrl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int authorIndex = text.indexOf(author);
        builder.setSpan(new URLSpan(DirtyBlog.getInstance().getAuthorLink(author)), authorIndex, authorIndex + author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
    
}
