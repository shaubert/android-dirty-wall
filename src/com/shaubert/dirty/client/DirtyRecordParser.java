package com.shaubert.dirty.client;

import com.shaubert.util.Dates;
import com.shaubert.util.Shlog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DirtyRecordParser {
    
    private static final Shlog SHLOG = new Shlog(DirtyRecordParser.class.getSimpleName());
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat dateWithTimeFormat1 = new SimpleDateFormat("dd MMMM yyyy hh.mm");
    private final SimpleDateFormat dateWithTimeFormat2 = new SimpleDateFormat("dd.MM.yyyy hh.mm");
    
    private static final String YOUTUBE_VIDEO_PATH = "http://img.youtube.com/vi/";
    
    public DirtyRecordParser() {
        dateFormat.setTimeZone(Dates.GMT);
        dateWithTimeFormat1.setTimeZone(Dates.GMT);
        dateWithTimeFormat2.setTimeZone(Dates.GMT);
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
            Date res = parseDateStr(dateWithTime, dateWithTimeFormat1);
            if (res == null) {
            	res = parseDateStr(dateWithTime, dateWithTimeFormat2);
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
}
