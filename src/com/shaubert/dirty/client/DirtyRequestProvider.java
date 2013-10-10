package com.shaubert.dirty.client;

import android.text.TextUtils;
import com.shaubert.blogadapter.client.DataLoaderRequest;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest.HttpMethod;
import com.shaubert.blogadapter.client.RequestProvider;
import com.shaubert.blogadapter.domain.Post;
import com.shaubert.dirty.DirtyPreferences;
import com.shaubert.util.Shlog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DirtyRequestProvider implements RequestProvider {

    private static final Shlog SHLOG = new Shlog(DirtyRequestProvider.class.getSimpleName());

    private DirtyPreferences dirtyPreferences;

    public DirtyRequestProvider(DirtyPreferences dirtyPreferences) {
        this.dirtyPreferences = dirtyPreferences;
    }

    @Override
    public DataLoaderRequest createRequestForPosts() {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        if (dirtyPreferences.isShowAllOnMainPage()) {
            request.setUrl("http://www.d3.ru/all/new");
        } else {
            request.setUrl("http://www.d3.ru/new");
        }
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
        request.setEntityMimeType("application/x-www-form-urlencoded");
        request.setUrl("http://d3.ru/ajax/blogs/top/");
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

    public DataLoaderRequest createLoginRequest(String login, String password, String captchaImageUrl, String captchaWords) {
        HttpDataLoaderRequest request = new HttpDataLoaderRequest();
        request.setUrl("https://d3.ru/login");
        request.setHttpMethod(HttpMethod.POST);
        request.setEntityMimeType("application/x-www-form-urlencoded");
        try {
            StringBuilder form = new StringBuilder();
            form.append("username=").append(URLEncoder.encode(login, "UTF-8"));
            form.append("&").append("password=").append(URLEncoder.encode(password, "UTF-8"));
            if (!TextUtils.isEmpty(captchaImageUrl)) {
                form.append("&").append("recaptcha_challenge_field=").append(captchaImageUrl.split("c=")[1]);
                form.append("&").append("recaptcha_response_field=").append(URLEncoder.encode(captchaWords, "UTF-8"));
            }

            request.setEntity(form.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            SHLOG.e("unable to encode", e);
        }
        return request;
    }

}