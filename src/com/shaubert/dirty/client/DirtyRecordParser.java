package com.shaubert.dirty.client;

import com.shaubert.util.Dates;
import com.shaubert.util.Shlog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DirtyRecordParser {
    
    private static final Shlog SHLOG = new Shlog(DirtyRecordParser.class.getSimpleName());
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat dateWithTimeFormat = new SimpleDateFormat("dd.MM.yyyy hh.mm");
    
    private static final String YOUTUBE_VIDEO_PATH = "http://img.youtube.com/vi/";
    
    public DirtyRecordParser() {
        dateFormat.setTimeZone(Dates.GMT);
        dateWithTimeFormat.setTimeZone(Dates.GMT);
    }

    public Date parseDate(String recordDate) {
        try {
            String dateTime[] = recordDate.split(" в ");
            String date = dateTime[0];
            final String dateWithTime;
            Calendar calendar = Calendar.getInstance(Dates.GMT);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            if ("сегодня".equalsIgnoreCase(date)) {
                dateWithTime = dateFormat.format(calendar.getTime()) + " " + dateTime[1];
            } else if ("вчера".equalsIgnoreCase(date)) {
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                dateWithTime = dateFormat.format(calendar.getTime()) + " " + dateTime[1];
            } else {
                dateWithTime = date + " " + dateTime[1];
            }
            return dateWithTimeFormat.parse(dateWithTime);
        } catch (Exception ex) {
            SHLOG.w(ex);
        }
        return new Date(0);
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
}
