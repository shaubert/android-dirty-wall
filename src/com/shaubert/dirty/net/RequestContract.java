package com.shaubert.dirty.net;

import com.shaubert.net.core.RequestRepositoryOnContentResolver;
import com.shaubert.net.core.RequestStateBase;

import android.net.Uri;
import androidx.persistence.Column;
import androidx.persistence.ColumnType;
import androidx.persistence.Contract;
import androidx.persistence.Entity;
import androidx.persistence.GeneratedValue;
import androidx.persistence.Id;

@Contract(authority = RequestContract.AUTHORITY, dbFileName = "requests.db")
public class RequestContract {
    
static final String AUTHORITY = "com.shaubert.dirty.net";

    @Entity
    public static class Request {
        @Id
        @GeneratedValue
        @Column(type = ColumnType.INT)
        public static final String ID = RequestStateBase.ID_KEY;

        @Column(type = ColumnType.STRING, nullable = false)
        public static final String STATUS = RequestStateBase.STATUS_KEY;
        
        @Column(type = ColumnType.FLOAT, nullable = false)
        public static final String PROGRESS = RequestStateBase.PROGRESS_KEY;
        
        @Column(type = ColumnType.INT, nullable = false)
        public static final String CANCELLED = RequestStateBase.CANCELLED_KEY;
        
        @Column(type = ColumnType.STRING)
        public static final String EXTRAS = RequestStateBase.EXTRAS_KEY;
        
        @Column(type = ColumnType.STRING, nullable = false)
        public static final String CLASS_NAME = RequestRepositoryOnContentResolver.CLASS_NAME_KEY;
        
        @Column(type = ColumnType.LONG)
        public static final String CREATION_TIME = RequestRepositoryOnContentResolver.CREATION_TIME_KEY;
        
        @Column(type = ColumnType.LONG)
        public static final String UPDATE_TIME = RequestRepositoryOnContentResolver.UPDATE_TIME_KEY;
        
        public static final Uri URI = Uri.parse("content://com.shaubert.dirty.net/request");
    }
    
}
