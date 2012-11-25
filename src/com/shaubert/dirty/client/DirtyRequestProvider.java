package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.DataLoaderRequest;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest;
import com.shaubert.blogadapter.client.RequestProvider;
import com.shaubert.blogadapter.domain.Post;

public class DirtyRequestProvider implements RequestProvider {

    @Override
    public DataLoaderRequest createRequestForPosts() {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setUrl("http://www.d3.ru/new");
        return request;
    }

    @Override
    public DataLoaderRequest createRequestForComments(Post post) {
        DirtyPost dirtyPost = (DirtyPost)post;
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        String subBlogUrl = getSubBlogUrl(dirtyPost);
        request.setUrl("http://" + subBlogUrl + "/comments/" + dirtyPost.getServerId());
        return request;
    }

	private String getSubBlogUrl(DirtyPost dirtyPost) {
		String subBlogUrl = dirtyPost.getSubBlogName();
        if (subBlogUrl == null) {
        	subBlogUrl = "d3.ru";
        }
		return subBlogUrl;
	}

    public DataLoaderRequest createRequestForComment(DirtyPost post, long commentServerId) {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setUrl("http://" + getSubBlogUrl(post) + "/comments/" + post.getServerId() + "/#" + commentServerId);
        return request;
    }

}