package com.shaubert.util;

import com.shaubert.dirty.client.HtmlTagFinder;

import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;

import java.util.HashMap;

public class HtmlHelper {

    private static final Shlog SHLOG = new Shlog(HtmlHelper.class.getSimpleName());
    
    public static interface ImageHandler {
        public void onImgTag(String src, int widht, int height);
    }
    
    public static void convertToSpannable(HtmlTagFinder.TagNode tag, SpannableStringBuilder builder, Object[] spans, ImageHandler imageHandler) {
        SpannableStringBuilder result = new SpannableStringBuilder();
        for (HtmlTagFinder.TagNode node : tag.getChilds()) {
            if (node.isContentNode()) {
                String text = node.getText();
                if (!TextUtils.isEmpty(text)) {
                    char newCh = text.charAt(0);
                    if (result.length() > 0 && (newCh == '\n' || newCh == ' ' || newCh == '\t')) {
                        char prev = result.charAt(result.length() - 1);
                        int start = 0;
                        if (prev == '\n' || prev == ' ' || prev == '\t') {
                            while (start < text.length()) {
                                newCh = text.charAt(start);
                                if (newCh != '\n' && newCh != ' ' && newCh != '\t') {
                                    break;
                                }
                                start++;
                            }
                        }
                        if (start < text.length()) {
                            result.append(text.substring(start));
                        }
                    } else {
                        result.append(text);
                    }
                }
            } else {
                String name = node.getName();
                Object[] resSpan = createSpanForTag(node, imageHandler);
                
                if ("br".equalsIgnoreCase(name)) {
                    handleBr(result);
                } else if ("p".equalsIgnoreCase(name)) {
                    handleP(result);
                }
                
                if (!"script".equalsIgnoreCase(name)) {
                    convertToSpannable(node, result, resSpan, imageHandler);
                }
                
                if (isHeaderTag(name)) {
                    handleP(result);
                } else if ("p".equalsIgnoreCase(name)) {
                    handleP(result);
                }
            }
        }
        if (spans != null) {
            for (Object span : spans) {
                result.setSpan(span, 0, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        builder.append(result);
    }
    
    private static Object[] createSpanForTag(HtmlTagFinder.TagNode node, ImageHandler imageHandler) {
        String tag = node.getName();
        if (tag.equalsIgnoreCase("em")) {
            return collect(new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("b")) {
            return collect(new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("strong")) {
            return collect(new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("cite")) {
            return collect(new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("dfn")) {
            return collect(new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("i")) {
            return collect(new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("big")) {
            return collect(new RelativeSizeSpan(1.25f));
        } else if (tag.equalsIgnoreCase("small")) {
            return collect(new RelativeSizeSpan(0.8f));
        } else if (tag.equalsIgnoreCase("font")) {
            Object colorSpan = null;
            Object fontSpan = null;
            if (node.getAttributes().getValue("", "color") != null) {
                int c = getHtmlColor(node.getAttributes().getValue("", "color"));
                if (c != -1) {
                    colorSpan = new ForegroundColorSpan(c | 0xFF000000);
                }
            }                
            if (node.getAttributes().getValue("", "face") != null) {
                String face = node.getAttributes().getValue("", "face");
                fontSpan = new TypefaceSpan(face);
            }
            collect(colorSpan, fontSpan);
        } else if (tag.equalsIgnoreCase("blockquote")) {
            return collect(new QuoteSpan());
        } else if (tag.equalsIgnoreCase("tt")) {
            return collect(new TypefaceSpan("monospace"));
        } else if (tag.equalsIgnoreCase("a")) {
            String url = node.getAttributes().getValue("", "href");
            if (url.startsWith("/")) {
                url = "http://www.dirty.ru" + url;
            } else if (!url.startsWith("http")) {
                url = "http://" + url;
            }
            return collect(new URLSpan(url));
        } else if (tag.equalsIgnoreCase("u")) {
            return collect(new UnderlineSpan());
        } else if (tag.equalsIgnoreCase("sup")) {
            return collect(new SuperscriptSpan());
        } else if (tag.equalsIgnoreCase("sub")) {
            return collect(new SubscriptSpan());
        } else if (isHeaderTag(tag)) {
            int level = Integer.parseInt(String.valueOf(tag.charAt(1))) - 1;
            return collect(new RelativeSizeSpan(HEADER_SIZES[level]),
                    new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("img")) {
            String src = node.getAttributes().getValue("", "src");
            if (imageHandler != null && !TextUtils.isEmpty(src)) {
                String widthStr = node.getAttributes().getValue("", "width");
                String heightStr = node.getAttributes().getValue("", "height");
                int widht = -1;
                int height = -1;
                if (widthStr != null && heightStr != null) {
                    try {
                        widht = Integer.parseInt(widthStr);
                        height = Integer.parseInt(heightStr);
                    } catch (Exception ex) {
                        SHLOG.w(ex);
                        widht = height = -1;
                    }
                }
                imageHandler.onImgTag(src, widht, height);
            }
        } else if (tag.equalsIgnoreCase("div")) {
            //<div class="post_video" style="background-image:url(http://img.youtube.com/vi/dygrAlATTsg/2.jpg);">
            String classValue = node.getAttributes().getValue("", "class");
            if (classValue != null && classValue.equalsIgnoreCase("post_video")) {
                if (imageHandler != null) {
                    try {
                        String style = node.getAttributes().getValue("", "style");
                        String url = style.substring(style.indexOf("url(") + 4);
                        url = url.substring(0, url.lastIndexOf(')'));
                        imageHandler.onImgTag(url, 120, 90);
                    } catch (IndexOutOfBoundsException ex) {
                        SHLOG.w(ex);
                    }
                }
            }
        }
        return new Object[0];
    }

    private static boolean isHeaderTag(String tag) {
        return tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6';
    }

    private static Object[] collect(Object ... spans) {
        return spans;
    }

    private static void handleBr(SpannableStringBuilder text) {
        text.append("\n");
    }
    
    private static void handleP(SpannableStringBuilder text) {
        int len = text.length();

        if (len >= 1 && text.charAt(len - 1) == '\n') {
            if (len >= 2 && text.charAt(len - 2) == '\n') {
                return;
            }

            text.append("\n");
            return;
        }

        if (len != 0) {
            text.append("\n\n");
        }
    }
    
    private static final float[] HEADER_SIZES = {
        1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
    };
    
    private static int getHeaderTagIndex(float size) {
        for (int i = 0; i < HEADER_SIZES.length; i++) {
            if (size >= HEADER_SIZES[i]) {
                return i + 1;
            }
        }
        return HEADER_SIZES.length;
    }
    
    private static HashMap<String,Integer> COLORS = buildColorMap();

    private static HashMap<String,Integer> buildColorMap() {
        HashMap<String,Integer> map = new HashMap<String,Integer>();
        map.put("aqua", 0x00FFFF);
        map.put("black", 0x000000);
        map.put("blue", 0x0000FF);
        map.put("fuchsia", 0xFF00FF);
        map.put("green", 0x008000);
        map.put("grey", 0x808080);
        map.put("lime", 0x00FF00);
        map.put("maroon", 0x800000);
        map.put("navy", 0x000080);
        map.put("olive", 0x808000);
        map.put("purple", 0x800080);
        map.put("red", 0xFF0000);
        map.put("silver", 0xC0C0C0);
        map.put("teal", 0x008080);
        map.put("white", 0xFFFFFF);
        map.put("yellow", 0xFFFF00);
        return map;
    }
    
    public static int getHtmlColor(String color) {
        Integer i = COLORS.get(color.toLowerCase());
        if (i != null) {
            return i;
        } else {
            try {
                return XmlUtils.convertValueToInt(color, -1);
            } catch (NumberFormatException nfe) {
                return -1;
            }
        }
    }
    
    
    /**
     * Returns an HTML representation of the provided Spanned text.
     */
    public static String toHtml(Spanned text) {
        StringBuilder out = new StringBuilder();
        withinHtml(out, text);
        return out.toString();
    }

    private static void withinHtml(StringBuilder out, Spanned text) {
        int len = text.length();

        int next;
        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, len, ParagraphStyle.class);
            ParagraphStyle[] style = text.getSpans(i, next, ParagraphStyle.class);
            String elements = " ";
            boolean needDiv = false;

            for(int j = 0; j < style.length; j++) {
                if (style[j] instanceof AlignmentSpan) {
                    Layout.Alignment align = 
                        ((AlignmentSpan) style[j]).getAlignment();
                    needDiv = true;
                    if (align == Layout.Alignment.ALIGN_CENTER) {
                        elements = "align=\"center\" " + elements;
                    } else if (align == Layout.Alignment.ALIGN_OPPOSITE) {
                        elements = "align=\"right\" " + elements;
                    } else {
                        elements = "align=\"left\" " + elements;
                    }
                }
            }
            if (needDiv) {
                out.append("<div " + elements + ">");
            }

            withinDiv(out, text, i, next);

            if (needDiv) {
                out.append("</div>");
            }
        }
    }

    @SuppressWarnings("unused")
    private static void withinDiv(StringBuilder out, Spanned text,
            int start, int end) {
        int next;
        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, QuoteSpan.class);
            QuoteSpan[] quotes = text.getSpans(i, next, QuoteSpan.class);

            for (QuoteSpan quote: quotes) {
                out.append("<blockquote>");
            }

            if (i > start) {
                out.append("<p>");
            }
            
            withinBlockquote(out, text, i, next);

            if (i > start) {
                out.append("</p>");
            }
            
            for (QuoteSpan quote: quotes) {
                out.append("</blockquote>\n");
            }
        }
    }

    private static void withinBlockquote(StringBuilder out, Spanned text,
                                         int start, int end) {
        int next;
        for (int i = start; i < end; i = next) {
            next = TextUtils.indexOf(text, '\n', i, end);
            if (next < 0) {
                next = end;
            }

            int nl = 0;

            while (next < end && text.charAt(next) == '\n') {
                nl++;
                next++;
            }

            withinParagraph(out, text, i, next - nl, nl, next == end);
        }
    }

    private static void withinParagraph(StringBuilder out, Spanned text,
                                        int start, int end, int nl,
                                        boolean last) {
        int next;
        boolean hasHSpan = false;
        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, CharacterStyle.class);
            CharacterStyle[] style = text.getSpans(i, next,
                                                   CharacterStyle.class);

            for (int j = 0; j < style.length; j++) {
                if (style[j] instanceof RelativeSizeSpan) {
                    hasHSpan = true;
                }
            }
            
            for (int j = 0; j < style.length; j++) {
                if (style[j] instanceof RelativeSizeSpan) {
                    out.append("<h" + getHeaderTagIndex(((RelativeSizeSpan) style[j]).getSizeChange()) + ">");
                } 
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if (!hasHSpan && (s & Typeface.BOLD) != 0) {
                        out.append("<b>");
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("<i>");
                    }
                }
                if (style[j] instanceof TypefaceSpan) {
                    String s = ((TypefaceSpan) style[j]).getFamily();

                    if (s.equals("monospace")) {
                        out.append("<tt>");
                    }
                }
                if (style[j] instanceof SuperscriptSpan) {
                    out.append("<sup>");
                }
                if (style[j] instanceof SubscriptSpan) {
                    out.append("<sub>");
                }
                if (style[j] instanceof UnderlineSpan) {
                    out.append("<u>");
                }
                if (style[j] instanceof StrikethroughSpan) {
                    out.append("<strike>");
                }
                if (style[j] instanceof URLSpan) {
                    out.append("<a href=\"");
                    out.append(((URLSpan) style[j]).getURL());
                    out.append("\">");
                }
                if (style[j] instanceof ImageSpan) {
                    out.append("<img src=\"");
                    out.append(((ImageSpan) style[j]).getSource());
                    out.append("\">");

                    // Don't output the dummy character underlying the image.
                    i = next;
                }
                if (style[j] instanceof AbsoluteSizeSpan) {
                    out.append("<font size =\"");
                    out.append(((AbsoluteSizeSpan) style[j]).getSize() / 6);
                    out.append("\">");
                }
                if (style[j] instanceof ForegroundColorSpan) {
                    out.append("<font color =\"#");
                    String color = Integer.toHexString(((ForegroundColorSpan)
                            style[j]).getForegroundColor() + 0x01000000);
                    while (color.length() < 6) {
                        color = "0" + color;
                    }
                    out.append(color);
                    out.append("\">");
                }
            }

            withinStyle(out, text, i, next);

            for (int j = style.length - 1; j >= 0; j--) {
                if (style[j] instanceof ForegroundColorSpan) {
                    out.append("</font>");
                }
                if (style[j] instanceof AbsoluteSizeSpan) {
                    out.append("</font>");
                }
                if (style[j] instanceof URLSpan) {
                    out.append("</a>");
                }
                if (style[j] instanceof StrikethroughSpan) {
                    out.append("</strike>");
                }
                if (style[j] instanceof UnderlineSpan) {
                    out.append("</u>");
                }
                if (style[j] instanceof SubscriptSpan) {
                    out.append("</sub>");
                }
                if (style[j] instanceof SuperscriptSpan) {
                    out.append("</sup>");
                }
                if (style[j] instanceof TypefaceSpan) {
                    String s = ((TypefaceSpan) style[j]).getFamily();

                    if (s.equals("monospace")) {
                        out.append("</tt>");
                    }
                }
                if (style[j] instanceof RelativeSizeSpan) {
                    out.append("</h" + getHeaderTagIndex(((RelativeSizeSpan) style[j]).getSizeChange()) + ">");
                } 
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if (!hasHSpan && (s & Typeface.BOLD) != 0) {
                        out.append("</b>");
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("</i>");
                    }
                }                
            }
        }

        if (!hasHSpan) {
            for (int i = 0; i < Math.min(2, nl); i++) {
                out.append("<br>");
            }
        }
    }

    private static void withinStyle(StringBuilder out, Spanned text,
                                    int start, int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);

            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c > 0x7E || c < ' ') {
                out.append("&#" + ((int) c) + ";");
            } else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }
    }
    
}
