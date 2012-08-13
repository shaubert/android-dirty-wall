package com.shaubert.dirty;

import com.shaubert.net.core.DefaultLoaderIdMapper;

public class Loaders {

    public static final int DIRTY_POST_IDS_LOADER = 1;
    public static final int DIRTY_POSTS_LOADER = 2;
 
    public static final DefaultLoaderIdMapper POST_LOADER_MAPPER = new DefaultLoaderIdMapper(1000, 1000);
    
    public static final DefaultLoaderIdMapper REQUEST_LOADER_MAPPER = new DefaultLoaderIdMapper(2001, 1000);
    
    public static final DefaultLoaderIdMapper COMMENTS_LOADER_MAPPER = new DefaultLoaderIdMapper(3002, 1000);
    
}
