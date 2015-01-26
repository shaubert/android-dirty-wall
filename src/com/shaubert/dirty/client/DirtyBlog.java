package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.*;
import com.shaubert.dirty.DirtyPreferences;

public class DirtyBlog extends Blog {

    //http://img.dirty.ru/d3/gertruda/40.jpg
    
    private static DirtyBlog instance;

    public static void init(DirtyPreferences dirtyPreferences) {
        if (instance == null) {
            DataLoader dataLoader = new HttpDataLoader(new HttpClientGateway(null));
            DirtyRequestProvider requestProvider = new DirtyRequestProvider(dirtyPreferences);
            instance = new DirtyBlog(dataLoader, new DirtyParserFactory(), requestProvider);
        }
    }

    public static DirtyBlog getInstance() {
        return instance;
    }
    
    private final DirtyRequestProvider requestProvider;
    
    private DirtyBlog(DataLoader dataLoader, DirtyParserFactory parserFactory,
            DirtyRequestProvider requestProvider) {
        super(dataLoader, parserFactory, requestProvider);
        this.requestProvider = requestProvider;
    }

    public Pager<DirtyPost> createPager(String subBlogUrl) {
        return new Pager<DirtyPost>(dataLoader, parserFactory.createPostParser(),
                requestProvider.createRequestForPosts(subBlogUrl));
    }

	public Pager<DirtySubBlog> createBlogsPager(int offset) {
        return new Pager<DirtySubBlog>(dataLoader, ((DirtyParserFactory) parserFactory).createBlogsParser(),
        		requestProvider.createRequestForBlogs(offset));
    }
    
    public String getPostLink(DirtyPost post) {
        return ((HttpDataLoaderRequest) requestProvider.createRequestForComments(post)).getUrl();
    }
    
    public String getCommentLink(DirtyPost post, long commentServerId) {
        return ((HttpDataLoaderRequest) requestProvider.createRequestForComment(post, commentServerId)).getUrl();
    }

    public String getAuthorLink(String authorName) {
        return "http://d3.ru/user/" + authorName;
    }

    public DirtyRequestProvider getRequestProvider() {
        return requestProvider;
    }
}
