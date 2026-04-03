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
        
        // --- HARDCORE SETTINGS ---
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); 
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(DESKTOP_USER_AGENT);
        s.setSupportMultipleWindows(false);

        // --- BLACK SCREEN KA PERMANENT ILAJ ---
        // Seedha YouTube load karo, local index.html ka intezar mat karo
        webView.loadUrl("https://m.youtube.com");

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
                injectSaktLogic(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectSaktLogic(view);
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

    // --- THE MASTER ENGINE: Background Play + Ad-Skipper ---
    private void injectSaktLogic(WebView view) {
        // 1. Visibility state hack (Lock to visible)
        view.evaluateJavascript("javascript:Object.defineProperty(document, 'visibilityState', {get: () => 'visible', configurable: true});", null);
        view.evaluateJavascript("javascript:Object.defineProperty(document, 'hidden', {get: () => false, configurable: true});", null);
        
        // 2. Ultra Ad-Skipper & Auto-Play Loop
        String jsLoop = "setInterval(() => {" +
            "document.querySelectorAll('.ad-showing, .ad-interrupting, ytm-promoted-video-renderer, .ytp-ad-overlay-container').forEach(v => { v.style.display='none'; });" +
            "const skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern');" +
            "if(skipBtn) skipBtn.click();" +
            "const video = document.querySelector('video');" +
            "if(video && video.paused && !video.ended && !video.seeking) video.play();" +
            "}, 800);"; // Thoda fast (800ms) check karega
        view.evaluateJavascript("javascript:" + jsLoop, null);
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
            injectSaktLogic(getBridge().getWebView());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getBridge() != null && getBridge().getWebView() != null) {
            injectSaktLogic(getBridge().getWebView());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().resumeTimers();
            injectSaktLogic(getBridge().getWebView());
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
