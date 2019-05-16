package com.guard.security.crc.crc_guard_app.webview;

import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class mWebClient  extends WebViewClient {

    @Override
    public void onPageFinished(WebView view, String url) {
        // Al terminar de cargar si la pagina no devuelve respuesta se define el tiempo de respuesta como falso
        view.loadUrl("javascript:prueba()");
    }
}
