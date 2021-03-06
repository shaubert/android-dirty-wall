package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.*;
import com.shaubert.dirty.client.HtmlTagFinder.AttributeWithValue.Constraint;
import com.shaubert.dirty.client.HtmlTagFinder.Callback;
import com.shaubert.dirty.client.HtmlTagFinder.Rule;
import com.shaubert.dirty.client.HtmlTagFinder.TagNode;
import com.shaubert.util.Shlog;

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
            if (gertrudaUrl != null && !gertrudaUrl.startsWith("http")) {
                gertrudaUrl = "http://d3.ru" + gertrudaUrl;
            }
        } catch (Exception ex) {
            SHLOG.w(ex);
        }
    }

    public void parsePost(HtmlTagFinder.TagNode post) {
        DirtyPost dirtyPost = new DirtyPost();
        dirtyPost.setServerId(Long.parseLong(post.getAttributes().getValue("", "id").substring(1)));
        SHLOG.d("parsing post " + dirtyPost.getServerId());
        boolean isGolden = post.getAttributes().getValue("", "class").contains(POST_GOLDEN_DIV_CLASS_PART);
		dirtyPost.setGolden(isGolden);
        
        List<HtmlTagFinder.TagNode> childTags = post.getNotContentChildren();
        HtmlTagFinder.TagNode body = childTags.get(0);
        helperParser.parseBody(body, dirtyPost);

        HtmlTagFinder.TagNode info = childTags.get(1);
        List<TagNode> aTags = info.findAll(new Rule("a"));
        TagNode authorTag = aTags.get(0);
		dirtyPost.setAuthor(authorTag.getChilds().get(0).getText());
        String authorHref = authorTag.getAttributes().getValue("", "href");
        dirtyPost.setAuthorLink(authorHref.startsWith("http") ? authorHref : ("http://d3.ru" + authorHref));
        List<TagNode> dateSpans = info.findAll(new Rule("span").withAttribute("data-epoch_date"));
        if (dateSpans.size() == 1) {
            TagNode dateSpan = dateSpans.get(0);
            String postDate = dateSpan.getAttributes().getValue("data-epoch_date");
            dirtyPost.setCreationDate(helperParser.parseEpochDate(postDate));
        }
        dirtyPost.setCommentsCount(0);
        TagNode commentsTag = findTag(info, "оммент");
        TagNode commentsHref = commentsTag.getParent();
        while (commentsHref != null && !commentsHref.getName().equals("a")) {
            commentsHref = commentsHref.getParent();
        }
        if (commentsHref != null) {
            String commentsHrefUrl = commentsHref.getAttributes().getValue("href");
            String d3BlogRef = "";
            if (commentsHrefUrl.contains(".d3.ru")) {
                d3BlogRef = commentsHrefUrl.split("\\.d3\\.ru")[0].replaceAll("\\\\", "\\/");
                int startIndex = d3BlogRef.lastIndexOf('/');
                if (startIndex > 0) {
                    d3BlogRef = d3BlogRef.substring(startIndex + 1);
                }
            }
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
        }
        TagNode voteTag = info.findAll(new Rule("div").withAttributeWithValue("class", "vote")).get(0);
        String votesString = voteTag.getNotContentChildren().isEmpty() ? "" : voteTag.getNotContentChildren().get(0).getChilds().get(0).getText();
        if (voteTag.getNotContentChildren().size() > 1) {
            votesString += voteTag.getNotContentChildren().get(1).getChilds().get(0).getText();
        }
        votesString = votesString.trim();
        if (votesString.length() > 0 && votesString.charAt(0) == '+') {
            votesString = votesString.substring(1);
        }
        try {
            dirtyPost.setVotesCount(Integer.parseInt(votesString));
        } catch (NumberFormatException ex) {
            SHLOG.w("error parsing votes count", ex);
            dirtyPost.setVotesCount(0);
        }
        this.result.getResult().add(dirtyPost);
    }

    private TagNode findTag(TagNode root, String query) {
        if (root.getText() != null && root.getText().contains(query)) {
            return root;
        } else {
            for (TagNode node : root.getChilds()) {
                TagNode result = findTag(node, query);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

    }

    private void parseNextPageLink(HtmlTagFinder.TagNode tag) {
        int totalPages = Integer.parseInt(tag.getNotContentChildren().get(0).getChilds().get(0).getText());
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
