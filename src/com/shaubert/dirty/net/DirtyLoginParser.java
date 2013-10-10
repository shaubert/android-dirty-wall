package com.shaubert.dirty.net;

import android.text.TextUtils;
import com.shaubert.dirty.client.HtmlParser;
import com.shaubert.dirty.client.HtmlTagFinder;
import com.shaubert.util.Shlog;

import java.io.ByteArrayInputStream;

public class DirtyLoginParser extends HtmlParser {

    private static final Shlog SHLOG = new Shlog(DirtyLoginParser.class.getSimpleName());

    /*
    <img style="display:block;"
    alt="Проверка по слову reCAPTCHA"
    height="57"
    width="300"
    src="https://www.google.com/recaptcha/api/image?c=03AHJ_VuuIAtouHRrO4B9shBZqpz69cZQA2Kyu_GMjFxXuNNNVHId-ERApgqWIFR3hxT1JbRp9xiXwxMSrmfrLNI4L3g4hR8nbB_PaL61bCztQ3FzF0_W0i6FZYPlXWTXHJVgEqbv60kWnRuZsGzvwc2GPKiQPdmSkr1p-AfUnXm5f0NQ82x1wXz8">


    <a href="/login/" onclick="Recaptcha.reload(); return false;">на картинке написано</a>
     */

    private String recaptchaUrl;

    public boolean parse(String html) {
        recaptchaUrl = null;
        addTagFinder(new HtmlTagFinder(new HtmlTagFinder.Rule("div").withAttributeWithValue("id", "recaptcha_image"), recaptchaCallback));

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(html.getBytes("UTF-8"));
            parse(inputStream);
            return true;
        } catch (Exception ex) {
            SHLOG.e("error parsing login html", ex);
            return false;
        }
    }

    private HtmlTagFinder.Callback recaptchaCallback = new HtmlTagFinder.Callback() {
        @Override
        public void onTagFounded(HtmlTagFinder finder, HtmlTagFinder.TagNode foundTag) {
            if (!foundTag.getNotContentChildren().isEmpty()) {
                String src = foundTag.getNotContentChildren().get(0).getAttributes().getValue("src");
                if (!TextUtils.isEmpty(src) && src.contains("www.google.com/recaptcha/")) {
                    recaptchaUrl = src;
                }
            }
        }
    };

    public String getRecaptchaUrl() {
        return recaptchaUrl;
    }
}
