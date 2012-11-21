package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.DataLoaderRequest;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest;
import com.shaubert.blogadapter.client.RequestProvider;
import com.shaubert.blogadapter.domain.Post;

public class DirtyRequestProvider implements RequestProvider {

    @Override
    public DataLoaderRequest createRequestForPosts() {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setUrl("http://www.d3.ru");
        return request;
    }

    @Override
    public DataLoaderRequest createRequestForComments(Post post) {
        DirtyPost dirtyPost = (DirtyPost)post;
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setUrl("http://www.d3.ru/comments/" + dirtyPost.getServerId());
        return request;
    }

    public DataLoaderRequest createRequestForComment(DirtyPost post, long commentServerId) {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setUrl("http://www.dirty.ru/comments/" + post.getServerId() + "/#" + commentServerId);
        return request;
    }

}
