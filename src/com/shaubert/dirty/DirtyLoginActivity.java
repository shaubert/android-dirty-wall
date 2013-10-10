package com.shaubert.dirty;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.shaubert.dirty.net.DataLoadRequest;
import com.shaubert.dirty.net.DirtyLoginParser;
import com.shaubert.dirty.net.DirtyLoginRequest;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.Request;
import com.shaubert.util.Files;
import com.shaubert.util.Shlog;
import com.shaubert.util.Versions;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import java.io.File;

public class DirtyLoginActivity extends JournalBasedFragmentActivity {

    private static final Shlog SHLOG = new Shlog(DirtyLoginActivity.class.getSimpleName());

    private EditText loginField;
    private EditText passwordField;
    private ImageView captchaImage;
    private EditText captchaField;
    private View progress;
    private Button loginButton;

    private WebView webView;

    private AsyncTask<Void, Void, Void> imageLoadTask;

    private String captchaImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        loginField = (EditText) findViewById(R.id.login_field);
        passwordField = (EditText) findViewById(R.id.password_field);
        captchaImage = (ImageView) findViewById(R.id.captcha_image);
        captchaField = (EditText) findViewById(R.id.captcha_field);
        loginButton = (Button) findViewById(R.id.login_button);
        progress = findViewById(R.id.progress);

        webView = (WebView) findViewById(R.id.webview);
        setupWebView();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress();
                login(loginField.getText().toString(), passwordField.getText().toString(), captchaField.getText().toString());
            }
        });
    }

    private void showProgress() {
        progress.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.GONE);

        loginField.setEnabled(false);
        passwordField.setEnabled(false);
        captchaField.setEnabled(false);
    }

    private void hideProgress() {
        progress.setVisibility(View.GONE);
        loginButton.setVisibility(View.VISIBLE);

        loginField.setEnabled(true);
        passwordField.setEnabled(true);
        captchaField.setEnabled(true);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setBlockNetworkImage(true);
        settings.setJavaScriptEnabled(true);
        settings.setSaveFormData(false);
        settings.setSavePassword(false);

        webView.addJavascriptInterface(new WebViewContentListener(new WebViewContentListener.Callback() {
            @Override
            public void onError() {
                Crouton.showText(DirtyLoginActivity.this, R.string.simple_error_text, Style.ALERT);
                hideProgress();
            }

            @Override
            public void onSuccess() {
                Toast.makeText(DirtyLoginActivity.this, R.string.logged_in_toast_message, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onRecaptchaParsed(String captchaImageUrl) {
                DirtyLoginActivity.this.captchaImageUrl = captchaImageUrl;
                startImageLoading(captchaImageUrl);
            }
        }), "HTMLOUT");

        webView.setWebViewClient(new WebViewClient() {
            long lastCallTime;
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (SystemClock.uptimeMillis() - lastCallTime > 2000) {
                    if (Versions.isApiLevelAvailable(17)) {
                        webView.loadUrl("javascript:HTMLOUT.processHTML(document.documentElement.outerHTML);");
                    } else {
                        webView.loadUrl("javascript:window.HTMLOUT.processHTML(document.documentElement.outerHTML);");
                    }
                }
            }
        });
    }

    private void login(String login, String password, String captchaWords) {
        DirtyLoginRequest loginRequest = new DirtyLoginRequest();
        loginRequest.getState().put(DirtyLoginRequest.LOGIN_PARAM, login);
        loginRequest.getState().put(DirtyLoginRequest.PASSWORD_PARAM, password);
        if (captchaImage.getVisibility() == View.VISIBLE) {
            loginRequest.getState().put(DirtyLoginRequest.CAPTCHA_WORDS_PARAM, captchaWords);
            loginRequest.getState().put(DirtyLoginRequest.CAPTCHA_URL_PARAM, captchaImageUrl);
        }
        loginRequest.setFullStateChangeListener(new DefaultStatusListener(this) {
            @Override
            public void onFinished(Request request) {
                super.onFinished(request);
                RequestStateBase stateBase = (RequestStateBase) request.getState();
                String response = stateBase.getString(DirtyLoginRequest.RESPONSE);
                if (!TextUtils.isEmpty(response)) {
                    webView.loadDataWithBaseURL("http://d3.ru", response, "text/html", "UTF-8", null);
                }
            }

            @Override
            public void onError(Request request) {
                super.onError(request);
                hideProgress();
                Crouton.showText(DirtyLoginActivity.this, R.string.simple_error_text, Style.ALERT);
            }
        });
        startRequest(loginRequest);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            Crouton.cancelAllCroutons();
        }
    }

    private void startImageLoading(final String url) {
        imageLoadTask = new AsyncTask<Void, Void, Void>() {
            private Bitmap bitmap;
            private File cache;

            @Override
            protected Void doInBackground(Void... params) {
                cache = Files.getCaptchaFile(DirtyLoginActivity.this, url);
                if (!decodeImage() && !isCancelled()) {
                    DataLoadRequest loadRequest = new DataLoadRequest(url, cache.getAbsolutePath());
                    try {
                        if (!isCancelled()) {
                            loadRequest.execute();
                        }
                        if (!isCancelled()) {
                            decodeImage();
                        }
                    } catch (Exception e) {
                        SHLOG.w(e);
                    }
                }
                return null;
            }

            private boolean decodeImage() {
                if (cache.exists()) {
                    try {
                        SHLOG.d("decoding image " + cache.getAbsolutePath());
                        if (!isCancelled()) {
                            bitmap = BitmapFactory.decodeFile(cache.getAbsolutePath());
                        }
                        return true;
                    } catch (Exception ex) {
                        SHLOG.w(ex);
                    }
                }
                return false;
            }

            protected void onPostExecute(Void result) {
                hideProgress();
                if (bitmap != null) {
                    captchaImage.setImageBitmap(bitmap);
                    captchaImage.setVisibility(View.VISIBLE);
                    captchaField.setVisibility(View.VISIBLE);
                } else {
                    captchaImage.setVisibility(View.GONE);
                    captchaField.setVisibility(View.GONE);
                    Crouton.showText(DirtyLoginActivity.this, R.string.simple_error_text, Style.ALERT);
                }
            };

        };
        imageLoadTask.execute();
    }

    public static class WebViewContentListener {
        public interface Callback {
            void onError();

            void onSuccess();

            void onRecaptchaParsed(String imageUrl);
        }

        private Callback callback;
        private DirtyLoginParser dirtyLoginParser = new DirtyLoginParser();

        public WebViewContentListener(Callback callback) {
            this.callback = callback;
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void processHTML(String html) {
            if (dirtyLoginParser.parse(html)) {
                String recaptchaUrl = dirtyLoginParser.getRecaptchaUrl();
                if (TextUtils.isEmpty(recaptchaUrl)) {
                    callback.onSuccess();
                } else {
                    callback.onRecaptchaParsed(recaptchaUrl);
                }
            } else {
                callback.onError();
            }
        }
    }

}
