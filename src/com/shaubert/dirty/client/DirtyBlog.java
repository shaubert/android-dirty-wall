package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.Blog;
import com.shaubert.blogadapter.client.DataLoader;
import com.shaubert.blogadapter.client.HttpClientGateway;
import com.shaubert.blogadapter.client.HttpDataLoader;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest;
import com.shaubert.blogadapter.client.ParserFactory;

public class DirtyBlog extends Blog {

    //http://img.dirty.ru/d3/gertruda/40.jpg
    
    private static DirtyBlog instance;
    
    public static DirtyBlog getInstance() {
        if (instance == null) {
            DataLoader dataLoader = new HttpDataLoader(new HttpClientGateway());
            DirtyRequestProvider requestProvider = new DirtyRequestProvider();
            instance = new DirtyBlog(dataLoader, new DirtyParserFactory(), requestProvider);
        }
        return instance;
    }
    
    private final DirtyRequestProvider requestProvider;
    
    private DirtyBlog(DataLoader dataLoader, ParserFactory parserFactory,
            DirtyRequestProvider requestProvider) {
        super(dataLoader, parserFactory, requestProvider);
        this.requestProvider = requestProvider;
    }
    
    public String getPostLink(DirtyPost post) {
        return ((HttpDataLoaderRequest) requestProvider.createRequestForComments(post)).getUrl();
    }
    
    public String getCommentLink(DirtyPost post, long commentServerId) {
        return ((HttpDataLoaderRequest) requestProvider.createRequestForComment(post, commentServerId)).getUrl();
    }

}
