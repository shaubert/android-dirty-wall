package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.DataLoaderRequest;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest.HttpMethod;
import com.shaubert.blogadapter.client.RequestProvider;
import com.shaubert.blogadapter.domain.Post;
import com.shaubert.dirty.DirtyPreferences;

public class DirtyRequestProvider implements RequestProvider {

    private DirtyPreferences dirtyPreferences;

    public DirtyRequestProvider(DirtyPreferences dirtyPreferences) {
        this.dirtyPreferences = dirtyPreferences;
    }

    @Override
    public DataLoaderRequest createRequestForPosts() {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        if (dirtyPreferences.isShowAllOnMainPage()) {
            request.setUrl("https://www.d3.ru/all/new");
        } else {
            request.setUrl("https://www.d3.ru/new");
        }
        return request;
    }

    public DataLoaderRequest createRequestForPosts(String subBlogUrl) {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        if (!subBlogUrl.contains(".d3.ru")) {
            subBlogUrl = subBlogUrl + ".d3.ru";
        }
        request.setUrl("https://" + subBlogUrl + "/new");
        return request;
    }

    @Override
    public DataLoaderRequest createRequestForComments(Post post) {
        DirtyPost dirtyPost = (DirtyPost)post;
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        String subBlogUrl = getSubBlogUrl(dirtyPost);
        request.setUrl("https://" + subBlogUrl + "/comments/" + dirtyPost.getServerId());
        return request;
    }

    public DataLoaderRequest createRequestForBlogs(int offset) {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setHttpMethod(HttpMethod.POST);
        request.setEntityMimeType("application/x-www-form-urlencoded");
        request.setUrl("https://d3.ru/ajax/blogs/top/");
//        request.addCookie(new BasicClientCookie("domains_sort", "top"));
//        request.addCookie(new BasicClientCookie("main_index_sort_mode", "top"));
        String params = "offset=" + offset;
        request.setEntity(params.getBytes());
        return request;
    }
    
	private String getSubBlogUrl(DirtyPost dirtyPost) {
		return dirtyPost.getSubBlogHost();
	}

    public DataLoaderRequest createRequestForComment(DirtyPost post, long commentServerId) {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setUrl("https://" + getSubBlogUrl(post) + "/comments/" + post.getServerId() + "/#" + commentServerId);
        return request;
    }

}