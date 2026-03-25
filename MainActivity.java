package com.ayush.ytpro;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onPause() {
        super.onPause();
        if (this.bridge != null && this.bridge.getWebView() != null) {
            this.bridge.getWebView().resumeTimers();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (this.bridge != null && this.bridge.getWebView() != null) {
            this.bridge.getWebView().resumeTimers();
        }
    }
}

