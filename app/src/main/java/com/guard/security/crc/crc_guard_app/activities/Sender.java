package com.guard.security.crc.crc_guard_app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.guard.security.crc.crc_guard_app.R;
import com.guard.security.crc.crc_guard_app.webview.ManagerWebClient;
import com.guard.security.crc.crc_guard_app.webview.WebInterface;
import com.guard.security.crc.crc_guard_app.webview.mWebClient;

public class Sender extends BroadcastReceiver {
    private String mURL = "http://186.96.89.66:9090/crccoding/f?p=2560:9999";

    @Override
    public void onReceive(Context context, Intent intent) {
        WebView gv = new WebView(context);
        gv.setWebViewClient(new mWebClient());
        gv.getSettings().setJavaScriptEnabled(true);
        //gv.addJavascriptInterface(new WebInterface(context, gvGPS), "Android");
        gv.loadUrl(mURL);
        //gv.loadUrl("javascript:prueba");

        gv.setWebViewClient(new mWebClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:prueba()");
            }
        });


       Log.i("PRUEBA","CREADO");
    }
}
