package com.shaubert.dirty.client;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.URLSpan;
import com.shaubert.util.HtmlHelper;
import com.shaubert.util.Shlog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirtyRecordParser {

    private static final Shlog SHLOG = new Shlog(DirtyRecordParser.class.getSimpleName());

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));
    private final SimpleDateFormat dateWithTimeFormat1 = new SimpleDateFormat("dd MMMM yyyy HH.mm", new Locale("ru"));
    private final SimpleDateFormat dateWithTimeFormat2 = new SimpleDateFormat("dd.MM.yyyy HH.mm", new Locale("ru"));

    private static final String YOUTUBE_VIDEO_PATH = "http://img.youtube.com/vi/";
    private static final Pattern YOUTUBE_ID_PATTERN = Pattern.compile("(?<=watch\\?v=|/videos/|embed/)[^#&\\?]*");;

    public DirtyRecordParser() {
    }

    public Date parseEpochDate(String date) {
        try {
            long seconds = Long.parseLong(date);
            return new Date(seconds * 1000);
        } catch (NumberFormatException ex) {
            SHLOG.w("error parsing epoch date", ex);
        }
        return new Date(0);
    }

    public Date parseDate(String recordDate) {
        try {
            String dateTime[] = recordDate.split(" в ");
            String date = dateTime[0].trim();
            String time = dateTime[1].trim();
            if (time.contains(" ")) {
                time = time.split(" ")[0];
            }
            final String dateWithTime;
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            if ("сегодня".equalsIgnoreCase(date)) {
                dateWithTime = dateFormat.format(calendar.getTime()) + " " + time;
            } else if ("вчера".equalsIgnoreCase(date)) {
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                dateWithTime = dateFormat.format(calendar.getTime()) + " " + time;
            } else {
				dateWithTime = date + " " + time;
            }
            Date res = parseDateStr(dateWithTime, dateWithTimeFormat2);
            if (res == null) {
            	res = parseDateStr(dateWithTime, dateWithTimeFormat1);
            }
            
            if (res != null) {
            	return res;
            }
        } catch (Exception ex) {
            SHLOG.w(ex);
        }
        return new Date(0);
    }
    
    private Date parseDateStr(String str, DateFormat format) {
    	try {
    		return format.parse(str);
    	} catch (Exception ex) {
    		return null;
    	}
    }
    
    public String tryToConvertToVideoUrl(String imageUrl) {
        if (imageUrl.startsWith(YOUTUBE_VIDEO_PATH)) {
            try {
                String videoName = imageUrl.substring(YOUTUBE_VIDEO_PATH.length());
                return "http://www.youtube.com/watch?v=" + videoName.split("/")[0];
            } catch (Exception ex) {
                SHLOG.w(ex);
            }
        }
        return null;
    }

    public String getYoutubeVideoThumbnail(String youtubeUrl) {
        Matcher matcher = YOUTUBE_ID_PATTERN.matcher(youtubeUrl);
        if(matcher.find()){
            return YOUTUBE_VIDEO_PATH + matcher.group() + "/0.jpg";
        } else {
            return null;
        }
    }

    public void parseBody(HtmlTagFinder.TagNode body, final DirtyRecord dirtyRecord) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        final List<DirtyRecord.Image> images = new ArrayList<DirtyRecord.Image>();
        HtmlHelper.convertToSpannable(body, builder, null, new HtmlHelper.ImageHandler() {
                    @Override
                    public void onImgTag(String src, int widht, int height) {
                        images.add(new DirtyRecord.Image(src, widht, height));

                        String videoUrl = tryToConvertToVideoUrl(src);
                        if (!TextUtils.isEmpty(videoUrl)) {
                            dirtyRecord.setVideoUrl(videoUrl);
                        }
                    }
                }, new HtmlHelper.UnknownTagHandler() {
                    @Override
                    public void onUnknownTag(HtmlTagFinder.TagNode tag) {
                        if (tag.getName().equalsIgnoreCase("iframe")) {
                            String src = tag.getAttributes().getValue("src");
                            if (src != null && src.contains("youtube")) {
                                dirtyRecord.setVideoUrl(src);
                                images.add(0, new DirtyRecord.Image(getYoutubeVideoThumbnail(src), -1, -1));
                            }
                        }
                    }
                }
        );

        if (dirtyRecord.getVideoUrl() == null) {
            URLSpan[] urlSpans = builder.getSpans(0, 1, URLSpan.class);
            for (URLSpan urlSpan : urlSpans) {
                if (urlSpan.getURL().contains("youtube")) {
                    dirtyRecord.setVideoUrl(urlSpan.getURL());
                    images.add(0, new DirtyRecord.Image(getYoutubeVideoThumbnail(urlSpan.getURL()), -1, -1));
                    break;
                }
            }
        }

        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '\n') {
            builder.delete(builder.length() - 1, builder.length());
        }

        for (Iterator<DirtyRecord.Image> iterator = images.iterator(); iterator.hasNext();) {
            DirtyRecord.Image image = iterator.next();
            if (TextUtils.isEmpty(image.src)) {
                iterator.remove();
            }
        }

        dirtyRecord.setSpannedText(builder);
        dirtyRecord.setFormattedText(HtmlHelper.toHtml(builder));
        dirtyRecord.setMessage(builder.toString());
        dirtyRecord.setImages(images.toArray(new DirtyRecord.Image[images.size()]));
    }
}
