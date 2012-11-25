package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.DataLoaderRequest;
import com.shaubert.blogadapter.client.HttpDataLoaderRequest;
import com.shaubert.blogadapter.client.PagerDataParserResult;
import com.shaubert.blogadapter.client.Parser;
import com.shaubert.blogadapter.client.ParserResultList;
import com.shaubert.dirty.client.DirtyRecord.Image;
import com.shaubert.dirty.client.HtmlTagFinder.AttributeWithValue.Constraint;
import com.shaubert.dirty.client.HtmlTagFinder.Callback;
import com.shaubert.dirty.client.HtmlTagFinder.Rule;
import com.shaubert.dirty.client.HtmlTagFinder.TagNode;
import com.shaubert.util.HtmlHelper;
import com.shaubert.util.HtmlHelper.ImageHandler;
import com.shaubert.util.Shlog;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirtyPostParser extends HtmlParser implements Parser {
    
    private static final String POST_HTML_PARSING_TIMER = "posts receiving and html parsing time";
    
    private static final String POST_GOLDEN_DIV_CLASS_PART = "golden";
    private static final String POST_DIV_CLASS = "post";
    private static final String GERTRUDA_DIV_CLASS = "b-gertruda";
    private static final String TOTAL_PAGES_DIV_ID = "total_pages";    
    
    private static final Pattern DIGITS = Pattern.compile("[0-9]*");
    
    private static String gertrudaUrl = null;
    
    public static String getGertrudaUrl() {
        return gertrudaUrl;
    }
    
    private static final Shlog SHLOG = new Shlog(DirtyPostParser.class.getSimpleName());

    private DirtyRecordParser helperParser;
    private HttpDataLoaderRequest request;
    private PagerDataParserResult<DirtyPost> result;
    
    public DirtyPostParser() {
        helperParser = new DirtyRecordParser();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ParserResultList<DirtyPost> parse(DataLoaderRequest request, InputStream inputStream) throws IOException {
        this.request = (HttpDataLoaderRequest)request;
        this.result = new PagerDataParserResult<DirtyPost>();
        this.result.setResult(new ArrayList<DirtyPost>());
        
        addTagFinder(new HtmlTagFinder(new Rule("div").withAttributeWithValue("class", POST_DIV_CLASS, Constraint.STARTS_WITH), postCallback));
        addTagFinder(new HtmlTagFinder(new Rule("div").withAttributeWithValue("class", GERTRUDA_DIV_CLASS), gertrudaCallback));
        addTagFinder(new HtmlTagFinder(new Rule("div").withAttributeWithValue("id", TOTAL_PAGES_DIV_ID), totalPagesCallback));
        
        SHLOG.d("start loading and parsing html data");
        SHLOG.resetTimer(POST_HTML_PARSING_TIMER);
        parse(inputStream);
        SHLOG.logTimer(POST_HTML_PARSING_TIMER);
        
        SHLOG.d("gertruda = " + gertrudaUrl);
        
        return result;
    }

    private HtmlTagFinder.Callback gertrudaCallback = new Callback() {
        @Override
        public void onTagFounded(HtmlTagFinder finder, HtmlTagFinder.TagNode tag) {
            parseGertruda(tag);
        }
    };
    
    private HtmlTagFinder.Callback postCallback = new Callback() {
        @Override
        public void onTagFounded(HtmlTagFinder finder, HtmlTagFinder.TagNode tag) {
            parsePost(tag);
        }
    };
    
    private HtmlTagFinder.Callback totalPagesCallback = new Callback() {
        @Override
        public void onTagFounded(HtmlTagFinder finder, HtmlTagFinder.TagNode tag) {
            parseNextPageLink(tag);
        }
    };
       
    private void parseGertruda(HtmlTagFinder.TagNode gertrudaTag) {
        try {
            gertrudaUrl = gertrudaTag.findPath("a", "img").get(0).getAttributes().getValue("", "src");
        } catch (Exception ex) {
            SHLOG.w(ex);
        }
    }

    private void parsePost(HtmlTagFinder.TagNode post) {
        DirtyPost dirtyPost = new DirtyPost();
        dirtyPost.setServerId(Long.parseLong(post.getAttributes().getValue("", "id").substring(1)));
        SHLOG.d("parsing post " + dirtyPost.getServerId());
        boolean isGolden = post.getAttributes().getValue("", "class").contains(POST_GOLDEN_DIV_CLASS_PART);
		dirtyPost.setGolden(isGolden);
        
        List<HtmlTagFinder.TagNode> childTags = post.getNotContentChilds();
        HtmlTagFinder.TagNode body = childTags.get(0);
        parseBody(body, dirtyPost);
        
        HtmlTagFinder.TagNode info = childTags.get(1);
        List<TagNode> aTags = info.findAll(new Rule("a"));
        TagNode authorTag = aTags.get(0);
		dirtyPost.setAuthor(authorTag.getChilds().get(0).getText());
        dirtyPost.setAuthorLink("http://d3.ru" + authorTag.getAttributes().getValue("", "href"));
        String postDate = info.getChilds().get(2).getText().substring(2);
        dirtyPost.setCreationDate(helperParser.parseDate(postDate));
        dirtyPost.setCommentsCount(0);
        TagNode commentsTag = info.findAll(new Rule("span")).get(0);
        TagNode commentsHref = commentsTag.getNotContentChilds().get(0);
        String commentsHrefUrl = commentsHref.getAttributes().getValue("href");
        String d3BlogRef = commentsHrefUrl.startsWith("http") 
        		? commentsHrefUrl.split("/")[2] : commentsHrefUrl.split("/")[1];
        dirtyPost.setSubBlogName(d3BlogRef);
		String commentsString = commentsHref.getChilds().get(0).getText();
        Matcher commentsCountMatcher = DIGITS.matcher(commentsString);
        if (commentsCountMatcher.find() && commentsCountMatcher.group().length() > 0) {
            try {
                dirtyPost.setCommentsCount(Integer.parseInt(commentsCountMatcher.group()));
            } catch (NumberFormatException ex) {
                SHLOG.w(ex);
            }
        }
        TagNode voteTag = info.findAll(new Rule("div").withAttributeWithValue("class", "vote")).get(0);
    	dirtyPost.setVotesCount(Integer.parseInt(voteTag.getNotContentChilds().get(0).getChilds().get(0).getText()));
        this.result.getResult().add(dirtyPost);
    }
    
    private void parseBody(HtmlTagFinder.TagNode body, final DirtyPost dirtyPost) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        final List<Image> images = new ArrayList<Image>();
        HtmlHelper.convertToSpannable(body, builder, null, new ImageHandler() {
            @Override
            public void onImgTag(String src, int widht, int height) {
                images.add(new Image(src, widht, height));
                
                String videoUrl = helperParser.tryToConvertToVideoUrl(src);
                if (!TextUtils.isEmpty(videoUrl)) {
                    dirtyPost.setVideoUrl(videoUrl);
                }
            }
        });
        dirtyPost.setSpannedText(builder);
        dirtyPost.setFormattedText(HtmlHelper.toHtml(builder));
        dirtyPost.setMessage(builder.toString());
        dirtyPost.setImages(images.toArray(new Image[images.size()]));        
    }
           
    private void parseNextPageLink(HtmlTagFinder.TagNode tag) {
        int totalPages = Integer.parseInt(tag.getNotContentChilds().get(0).getChilds().get(0).getText());
        SHLOG.d("parsed total pages = " + totalPages);
        String curUrl = ((HttpDataLoaderRequest) request).getUrl();
        int currentPage = curUrl.contains("all/last") ? 1 : Integer.parseInt(curUrl.substring(curUrl.lastIndexOf('/') + 1));
        SHLOG.d("current pages = " + currentPage);
        HttpDataLoaderRequest nextRequest = null;
        if (currentPage <= totalPages) {
            nextRequest = new HttpDataLoaderRequest();
            nextRequest.setUrl("http://www.dirty.ru/pages/" + (currentPage + 1));
        }
        this.result.setNextDataRequest(nextRequest);
    }
}
