package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.DataLoaderRequest;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest.HttpMethod;
import com.shaubert.blogadapter.client.RequestProvider;
import com.shaubert.blogadapter.domain.Post;
import org.apache.http.impl.cookie.BasicClientCookie;

public class DirtyRequestProvider implements RequestProvider {

    @Override
    public DataLoaderRequest createRequestForPosts() {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setUrl("http://www.d3.ru/new");
        return request;
    }

    public DataLoaderRequest createRequestForPosts(String subBlogUrl) {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setUrl("http://" + subBlogUrl + "/new");
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

    public DataLoaderRequest createRequestForBlogs(int offset) {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setHttpMethod(HttpMethod.POST);
        request.setEntityMineType("application/x-www-form-urlencoded");
        request.setUrl("http://d3.ru/ajax/domains/");
        request.addCookie(new BasicClientCookie("domains_sort", "top"));
        request.addCookie(new BasicClientCookie("main_index_sort_mode", "top"));
        String params = "offset=" + offset;
        request.setEntity(params.getBytes());
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