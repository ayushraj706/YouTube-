package com.ayush.ytpro;

import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import com.getcapacitor.BridgeActivity;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private View customView;
    private final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WebView webView = getBridge().getWebView();
        WebSettings s = webView.getSettings();
        
        // --- SAKT SETTINGS ---
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); 
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(DESKTOP_USER_AGENT);
        s.setSupportMultipleWindows(false);

        webView.setWebViewClient(new WebViewClient() {
            private final List<String> adDomains = Arrays.asList(
                "googleads", "doubleclick", "adservice", "gen_204", 
                "googlesyndication", "youtube.com/pagead", "google.com/pagead", 
                "mads", "ad_break", "get_midroll_info"
            );

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                for (String domain : adDomains) {
                    if (url.contains(domain)) {
                        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectBackgroundHack(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectBackgroundHack(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                customView = view;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.addView(customView, new FrameLayout.LayoutParams(-1, -1));
                getBridge().getWebView().setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.removeView(customView);
                customView = null;
                getBridge().getWebView().setVisibility(View.VISIBLE);
            }
        });
    }

    private void injectBackgroundHack(WebView view) {
        view.evaluateJavascript("javascript:Object.defineProperty(document, 'visibilityState', {get: () => 'visible'});", null);
        view.evaluateJavascript("javascript:Object.defineProperty(document, 'hidden', {get: () => false});", null);
        view.evaluateJavascript("javascript:window.dispatchEvent(new Event('visibilitychange'));", null);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(16, 9);
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().resumeTimers(); 
            injectBackgroundHack(getBridge().getWebView());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getBridge() != null && getBridge().getWebView() != null) {
            injectBackgroundHack(getBridge().getWebView());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().resumeTimers();
            injectBackgroundHack(getBridge().getWebView());
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            getBridge().getWebView().getWebChromeClient().onHideCustomView();
        } else if (getBridge().getWebView().canGoBack()) {
            getBridge().getWebView().goBack();
        } else {
            super.onBackPressed();
        }
    }
}
