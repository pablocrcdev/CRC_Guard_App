package com.guard.security.crc.crc_guard_app.webview;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.guard.security.crc.crc_guard_app.activities.LocalHomeActivity;
import com.guard.security.crc.crc_guard_app.util.ErrorController;

public class ManagerWebClient extends WebViewClient {
    //=============================VARIABLES GLOBALES=============================================//
    boolean timeout;
    private Context gvContext;
    private NfcAdapter gvNfcAdapter;
    //============================================================================================//
    // El contructor se define con el parametro de contexto para refenrenciar siempre al activity
    // que este en primer plano y poder aplicar funciones sobre el mismo
    public ManagerWebClient(Context pcontext) {
        this.gvContext = pcontext;
        timeout = true;
        gvNfcAdapter = NfcAdapter.getDefaultAdapter(gvContext);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // Se define el hilo para manejar el tiempo de espera de respuesta
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // Manejo de la excepcion en caso de error
                }
                if(timeout) {
                    if (gvNfcAdapter != null) {
                        Intent intent = new Intent(gvContext, LocalHomeActivity.class);
                        gvContext.startActivity(intent);
                    }else{
                        new ErrorController(gvContext).showErrorDialog();
                   }
                    // Si es excedido el tiempo de espera se efectua la instruccion
                }
            }
        }).start();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        // Al terminar de cargar si la pagina no devuelve respuesta se define el tiempo de respuesta como falso
        timeout = false;
    }
    // La funcion solo se ejecutara al determinar algun error al cargar el webview
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        //
        if (gvNfcAdapter != null) {
            Intent intent = new Intent(gvContext, LocalHomeActivity.class);
            gvContext.startActivity(intent);
        }else{
            view.setVisibility(View.INVISIBLE);
            new ErrorController(gvContext).showErrorDialog();
        }

    }
}
