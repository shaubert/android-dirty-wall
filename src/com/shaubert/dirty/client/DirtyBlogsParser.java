package com.shaubert.dirty.client;

import android.annotation.SuppressLint;
import com.shaubert.blogadapter.client.DataLoaderRequest;
import com.shaubert.blogadapter.client.PagerDataParserResult;
import com.shaubert.blogadapter.client.Parser;
import com.shaubert.blogadapter.client.ParserResultList;
import com.shaubert.util.Shlog;
import com.shaubert.util.Versions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DirtyBlogsParser implements Parser {

	public static final Shlog SHLOG = new Shlog(DirtyBlogsParser.class.getSimpleName());
	
	private DirtyRequestProvider requestProvider = DirtyBlog.getInstance().getRequestProvider();
	private PagerDataParserResult<DirtySubBlog> result;

	private static final String LOAD_TIMER = "blogs parse time";

	@SuppressLint("NewApi")
	@SuppressWarnings("unchecked")
	@Override
	public ParserResultList<DirtySubBlog> parse(DataLoaderRequest request, InputStream inputStream) throws IOException {
		this.result = new PagerDataParserResult<DirtySubBlog>();
        this.result.setResult(new ArrayList<DirtySubBlog>());

        SHLOG.resetTimer(LOAD_TIMER);
        String data = readAll(inputStream);
        try {
			JSONObject object = new JSONObject(data);
			parseJson(object);
		} catch (JSONException e) {
			if (Versions.isApiLevelAvailable(9)) {
				throw new IOException(e);
			} else {
				SHLOG.e("error parsing blogs", e);
				throw new IOException(e.getMessage());
			}
		}
        SHLOG.logTimer(LOAD_TIMER);
        
        return result;
	}

	private void parseJson(JSONObject object) throws JSONException {
		parseNextRequest(object);
		
		JSONArray domains = object.getJSONArray("domains");
		parseDomains(domains);
	}

	private void parseNextRequest(JSONObject object) throws JSONException {
		if (!object.isNull("offset")) {
			int offset = object.getInt("offset");
	        result.setNextDataRequest(requestProvider.createRequestForBlogs(offset));
		}
	}

	private void parseDomains(JSONArray domains) throws JSONException {
		for (int i = 0; i < domains.length(); i++) {
			JSONObject obj = domains.getJSONObject(i);
			parseDomain(obj);
		}
	}

	private void parseDomain(JSONObject obj) throws JSONException {
		String desrc = obj.isNull("description") ? "" : obj.getString("description");
		String title = obj.isNull("title") ? "" : obj.getString("title");
		String name = obj.isNull("name") ? "" : obj.getString("name");
		long id = obj.getLong("id");
		SHLOG.d("parsing blog " + id + ", " + title);
		String url = obj.getString("url");
		if (url != null && url.contains(".d3.ru")) {
			url = url.replace(".d3.ru", "");
		}
		int readersCount = obj.getInt("readers_count");
		
		JSONObject owner = obj.getJSONObject("owner");
		String userLogin = owner.getString("login");
		long userId = owner.getLong("id");
		
		DirtySubBlog blog = new DirtySubBlog();
		blog.setAuthor(userLogin);
		blog.setAuthorId(userId);
		blog.setBlogId(id);
		blog.setDescription(desrc);
		blog.setDescriptionLower(desrc != null ? desrc.toLowerCase() : null);
		blog.setName(name);
		blog.setNameLower(name != null ? name.toLowerCase() : null);
		blog.setReadersCount(readersCount);
		blog.setTitle(title);
		blog.setTitleLower(title != null ? title.toLowerCase() : null);
		blog.setUrl(url);
		
		result.getResult().add(blog);
	}

	private String readAll(InputStream inputStream) throws IOException {
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		StringBuilder responseStrBuilder = new StringBuilder();

		String inputStr;
		while ((inputStr = streamReader.readLine()) != null) {
			responseStrBuilder.append(inputStr);
		}
		return responseStrBuilder.toString();
	}

}
