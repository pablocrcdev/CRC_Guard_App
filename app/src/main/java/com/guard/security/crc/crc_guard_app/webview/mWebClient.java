package com.guard.security.crc.crc_guard_app.webview;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class mWebClient  extends WebViewClient {
    private String Carga = "0";

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (Carga != "0"){
            //Despues de haber cargado una vez se destruye el webview
            //Si no se destruye sigue realizando consultas (más de 30 consultas por cada wakelock)
            view.destroy();
        }
    }
    @Override
    public void onPageFinished(WebView view, String url) {
        Carga = "1";
    }
}
