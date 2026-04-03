package com.ayush.ytpro;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
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
    // Desktop User Agent: Isse YouTube background play block nahi kar payega
    private final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WebView webView = this.bridge.getWebView();
        WebSettings s = webView.getSettings();
        
        // --- BRAVE LEVEL SETTINGS ---
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); 
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Desktop Mode Force: Sabse zaroori line
        s.setUserAgentString(DESKTOP_USER_AGENT);

        webView.setWebViewClient(new WebViewClient() {
            // SAKT AD-BLOCKING LIST
            private final List<String> adDomains = Arrays.asList(
                "googleads", "doubleclick", "adservice", "gen_204", 
                "googlesyndication", "youtube.com/pagead", "google.com/pagead"
            );

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                for (String domain : adDomains) {
                    if (url.contains(domain)) {
                        // Request ko beech mein hi maar do (Zero Byte response)
                        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // Har baar page start hote hi script inject karo
                view.evaluateJavascript("javascript:Object.defineProperty(document, 'visibilityState', {get: () => 'visible'});", null);
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
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.removeView(customView);
                customView = null;
                webView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // WebView ko 'Sone' mat do
        if (this.bridge != null && this.bridge.getWebView() != null) {
            this.bridge.getWebView().resumeTimers(); 
            // Isse YouTube ko lagega video abhi bhi samne chal raha hai
            this.bridge.getWebView().evaluateJavascript("javascript:window.dispatchEvent(new Event('visibilitychange'));", null);
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            this.bridge.getWebView().getWebChromeClient().onHideCustomView();
        } else if (this.bridge.getWebView().canGoBack()) {
            this.bridge.getWebView().goBack();
        } else {
            super.onBackPressed();
        }
    }
}
