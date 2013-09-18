package com.shaubert.dirty.client;

import com.shaubert.blogadapter.domain.Post;
import com.shaubert.dirty.db.ContentValuesState;
import com.shaubert.dirty.db.DirtyContract.DirtyPostEntity;

import android.content.ContentValues;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import java.io.File;
import java.util.Date;

public class DirtyRecord extends Post {

    private static final String IMAGES_DIVIDER = "@=-=-=@";
    private static final String IMAGE_PROP_DIVIDER = "@x-x-x@";

    public static class Image {
        public final String src;
        public final int widht;
        public final int height;

        public Image(String src, int widht, int height) {
            this.src = src;
            this.widht = widht;
            this.height = height;
        }

        @Override
        public String toString() {
            return src + " (" + widht + "x" + height + ")";
        }
    }

    private Spanned spannedText;
    protected ContentValuesState values;
    private File imagePath;

    public DirtyRecord() {
        this(new ContentValues());
    }

    public DirtyRecord(ContentValues values) {
        this.values = new ContentValuesState(values);
    }

    public ContentValues getValues() {
        return values.asContentValues();
    }

    @Override
    public String getAuthor() {
        return values.getAsString(DirtyPostEntity.AUTHOR);
    }

    @Override
    public void setAuthor(String author) {
        values.put(DirtyPostEntity.AUTHOR, author);
    }

    public long getCreationDateAsMillis() {
        return values.getAsLong(DirtyPostEntity.CREATION_DATE, -1L);
    }

    @Override
    public String getCreationDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCreationDate(String creationDate) {
        throw new UnsupportedOperationException();
    }

    public void setCreationDate(Date creationDate) {
        values.put(DirtyPostEntity.CREATION_DATE, creationDate.getTime());
    }

    @Override
    public String[] getImageUrls() {
        throw new UnsupportedOperationException();
    }

    public Image[] getImages() {
        return parseImages(values.getAsString(DirtyPostEntity.IMAGE_URLS, ""));
    }

    @Override
    public void setImageUrls(String[] imageUrls) {
        throw new UnsupportedOperationException();
    }

    public void setImages(Image[] images) {
        values.put(DirtyPostEntity.IMAGE_URLS, imagesToString(images));
    }

    public File getCachedImagePath() {
        return imagePath;
    }

    public void setCachedImagePath(File imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public String getMessage() {
        return values.getAsString(DirtyPostEntity.MESSAGE);
    }

    @Override
    public void setMessage(String message) {
        values.put(DirtyPostEntity.MESSAGE, message);
    }

    @Override
    public String getTitle() {
        return values.getAsString(DirtyPostEntity.TITLE);
    }

    @Override
    public void setTitle(String title) {
        values.put(DirtyPostEntity.TITLE, title);
    }

    public Long getId() {
        return values.getAsLong(DirtyPostEntity.ID);
    }

    public void setId(Long id) {
        if (id != null) {
            values.put(DirtyPostEntity.ID, id);
        } else {
            values.remove(DirtyPostEntity.ID);
        }
    }

    public void setServerId(long id) {
        values.put(DirtyPostEntity.SERVER_ID, id);
    }

    public long getServerId() {
        return values.getAsLong(DirtyPostEntity.SERVER_ID, -1L);
    }

    public int getVotesCount() {
        return values.getAsInteger(DirtyPostEntity.VOTES_COUNT, 0);
    }

    public void setVotesCount(int votesCount) {
        this.values.put(DirtyPostEntity.VOTES_COUNT, votesCount);
    }

    public String getAuthorLink() {
        return values.getAsString(DirtyPostEntity.AUTHOR_LINK);
    }

    public void setAuthorLink(String authorLink) {
        this.values.put(DirtyPostEntity.AUTHOR_LINK, authorLink);
    }

    public String getFormattedText() {
        return values.getAsString(DirtyPostEntity.FORMATTED_MESSAGE);
    }

    public void setFormattedText(String formattedText) {
        this.values.put(DirtyPostEntity.FORMATTED_MESSAGE, formattedText);
    }

    public Spanned getSpannedText() {
        if (spannedText == null) {
            if (getFormattedText() != null) {
                spannedText = Html.fromHtml(getFormattedText());
            } else {
                spannedText = new SpannableStringBuilder(getMessage());
            }
        }
        return spannedText;
    }

    public void setSpannedText(Spanned formattedText) {
        this.spannedText = formattedText;
    }

    public long getInsertTime() {
        return values.getAsLong(DirtyPostEntity.INSERT_TIME, 0L);
    }

    public void setInsertTime(long insertTime) {
        values.put(DirtyPostEntity.INSERT_TIME, insertTime);
    }

    public static Image[] parseImages(String urls) {
        if (!TextUtils.isEmpty(urls)) {
            String[] images = urls.split(IMAGES_DIVIDER);
            if (images != null && images.length > 0) {
                if (TextUtils.isEmpty(images[0])) {
                    return new Image[0];
                } else {
                    Image[] res = new Image[images.length];
                    for (int i = 0; i < images.length; i++) {
                        res[i] = parseImage(images[i]);
                    }
                    return res;
                }
            }
        }
        return new Image[0];
    }

    public static Image parseImage(String image) {
        String[] srcAndSizes = image.split(IMAGE_PROP_DIVIDER);
        return new Image(srcAndSizes[0], Integer.parseInt(srcAndSizes[1]), Integer.parseInt(srcAndSizes[2]));
    }

    public static String imagesToString(Image[] images) {
        if (images.length == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < images.length - 1; i++) {
            result.append(imageToString(images[i])).append(IMAGES_DIVIDER);
        }
        result.append(imageToString(images[images.length - 1]));
        return result.toString();
    }

    public static String imageToString(Image image) {
        return image.src + IMAGE_PROP_DIVIDER + image.widht + IMAGE_PROP_DIVIDER + image.height;
    }

    public void setVideoUrl(String url) {
        this.values.put(DirtyPostEntity.VIDEO_URL, url);
    }

    public String getVideoUrl() {
        return this.values.getAsString(DirtyPostEntity.VIDEO_URL, null);
    }

} 