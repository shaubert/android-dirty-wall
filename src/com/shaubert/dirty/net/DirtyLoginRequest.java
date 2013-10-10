package com.shaubert.dirty.net;

import com.shaubert.dirty.client.DirtyBlog;
import com.shaubert.net.core.RequestBase;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.ExecutionContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DirtyLoginRequest extends RequestBase {

    public static final String LOGIN_PARAM = "login";
    public static final String PASSWORD_PARAM = "password";
    public static final String CAPTCHA_WORDS_PARAM = "captcha_words";
    public static final String CAPTCHA_URL_PARAM = "captcha_url";
    public static final String RESPONSE = "response";

    public DirtyLoginRequest() {
        this(null);
    }

    public DirtyLoginRequest(RequestStateBase state) {
        super(state);
    }

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        String login = getState().getString(LOGIN_PARAM);
        String password = getState().getString(PASSWORD_PARAM);
        String captchaWords = getState().getString(CAPTCHA_WORDS_PARAM);
        String captchaUrl = getState().getString(CAPTCHA_URL_PARAM);
        DirtyBlog dirtyBlog = DirtyBlog.getInstance();
        dirtyBlog.clearSession();
        InputStream inputStream = dirtyBlog.login(login, password, captchaUrl, captchaWords);
        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }
            getState().put(RESPONSE, responseStrBuilder.toString());
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }

    }
}
