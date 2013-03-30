package com.shaubert.dirty.db;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.persistence.*;
import org.ecype.diego.Update;
import org.ecype.diego.Uris;

import static androidx.persistence.CascadeType.REMOVE;

@Contract(authority = DirtyContract.AUTHORITY, dbFileName = "dirty.db", version = 21)
public class DirtyContract {
    
    public static final String AUTHORITY = "com.shaubert.dirty";

    public static class DirtyRecordColums {

        @Id
        @GeneratedValue
        @Column(type = ColumnType.INT)
        public static final String ID = BaseColumns._ID;

        @Column(type = ColumnType.STRING)
        public static final String TITLE = "title";
        
        @Column(type = ColumnType.STRING)
        public static final String MESSAGE = "message";
        
        @Column(type = ColumnType.STRING)
        public static final String IMAGE_URLS = "image_urls";

        @Column(type = ColumnType.STRING)
        public static final String VIDEO_URL = "video_url";
        
        @Column(type = ColumnType.LONG, nullable = false)
        public static final String CREATION_DATE = "creation_date";
        
        @Column(type = ColumnType.STRING)
        public static final String AUTHOR = "author";
        
        @Column(type = ColumnType.STRING)
        public static final String AUTHOR_LINK = "author_link";
        
        @Column(type = ColumnType.LONG, nullable = false)
        public static final String SERVER_ID = "server_id";
                
        @Column(type = ColumnType.INT, nullable = false)
        public static final String VOTES_COUNT = "votes";
                
        @Column(type = ColumnType.STRING)
        public static final String FORMATTED_MESSAGE = "formatted_message";
        
        @Column(type = ColumnType.LONG)
        public static final String INSERT_TIME = "insert_time";        
    }
    
    @Entity
    @Update(callback = DirtyPostTableUpdater.class)
    public static class DirtyPostEntity extends DirtyRecordColums {
        
        @OneToMany(targetEntity = DirtyCommentEntity.class)
        public static final String COMMENTS = "comments";
        
        @Column(type = ColumnType.INT, nullable = false)
        public static final String COMMENTS_COUNT = "comments_count";
        
        @Column(type = ColumnType.INT, nullable = false)
        public static final String GOLDEN = "golden";
        
        @Column(type = ColumnType.INT)
        public static final String FAVORITE = "favorite";
        
        @Column(type = ColumnType.INT)
        public static final String UNREAD = "unread";
                
        @Column(type = ColumnType.STRING)
        public static final String SUB_BLOG_NAME = "sub_blog_name";
        
        public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/dirtypostentity");
        
        public static Uri getCommentsUri(long postId) {
            return Uris.append(
                    ContentUris.withAppendedId(DirtyPostEntity.URI, postId), 
                    DirtyPostEntity.COMMENTS);
        }
    }
    
    @Entity
    @Update(callback = DirtyCommentTableUpdater.class)
    public static class DirtyCommentEntity extends DirtyRecordColums {
       
        @ManyToOne(targetEntity = DirtyPostEntity.class, cascade = REMOVE)
        public static final String POST = "post";
        
        @Column(type = ColumnType.INT)
        public static final String INDENT_LEVEL = "indent_level";
        
        @Column(type = ColumnType.INT)
        public static final String COMMENTS_ORDER = "comments_order";
        
        public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/dirtycommententity");
        
    }
    
    @Entity
    @Update(callback = DirtyBlogTableUpdater.class)
    public static class DirtyBlogEntity {
       
        @Id
        @GeneratedValue
        @Column(type = ColumnType.INT)
        public static final String ID = BaseColumns._ID;

        @Column(type = ColumnType.LONG)
        public static final String BLOG_ID = "blog_id";
        
        @Column(type = ColumnType.STRING)
        public static final String TITLE = "title";

        @Column(type = ColumnType.STRING)
        public static final String TITLE_LOWER = "title_lower";

        @Column(type = ColumnType.STRING)
        public static final String NAME = "name";
        
        @Column(type = ColumnType.STRING)
        public static final String NAME_LOWER = "name_lower";

        @Column(type = ColumnType.STRING)
        public static final String DESCRIPTION = "description";

        @Column(type = ColumnType.STRING)
        public static final String DESCRIPTION_LOWER = "description_lower";

        @Column(type = ColumnType.STRING)
        public static final String URL = "url";

        @Column(type = ColumnType.STRING)
        public static final String AUTHOR = "author";
        
        @Column(type = ColumnType.LONG)
        public static final String AUTHOR_ID = "author_id";
        
        @Column(type = ColumnType.INT)
        public static final String READERS_COUNT = "readers_count";
        
        @Column(type = ColumnType.INT)
        public static final String FAVORITE = "favorite";
        
        public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/dirtyblogentity");
        
    }
    
}