package com.guard.security.crc.crc_guard_app.webview;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.guard.security.crc.crc_guard_app.BuildConfig;
import com.guard.security.crc.crc_guard_app.activities.LocalHomeActivity;
import com.guard.security.crc.crc_guard_app.activities.MainActivity;
import com.guard.security.crc.crc_guard_app.util.ErrorController;
import com.guard.security.crc.crc_guard_app.util.Procesos;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.os.Environment.DIRECTORY_DCIM;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStorageDirectory;


public class ManagerWebClient extends WebViewClient implements DownloadListener {
    //=============================VARIABLES GLOBALES=============================================//
    boolean timeout;
    private Context gvContext;
    private NfcAdapter gvNfcAdapter;
    private int Reload = 0;
    private Activity App;
    private android.webkit.WebView Wv;
    private MainActivity Main;

    //============================================================================================//
    // El contructor se define con el parametro de contexto para refenrenciar siempre al activity
    // que este en primer plano y poder aplicar funciones sobre el mismo
    public ManagerWebClient(Context pcontext, Activity app, WebView wv, MainActivity main) {
        this.gvContext = pcontext;
        this.App = app;
        timeout = true;
        gvNfcAdapter = NfcAdapter.getDefaultAdapter(gvContext);
        this.Wv = wv;
        this.Main = main;
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
                if (timeout) {
                    if (gvNfcAdapter != null) {
                        Intent intent = new Intent(gvContext, LocalHomeActivity.class);
                        gvContext.startActivity(intent);
                    } else {
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
        try {
            timeout = false;
            Procesos P = new Procesos();
            if (P.Num_Pagina(url).equals("1")) {
                if (Reload == 0) {
                    view.loadUrl("javascript:setImei('" + obtenerIdentificador2(this.gvContext) + "'" +
                            ",'" + BuildConfig.VERSION_NAME + "');");
                    //Se pone en 1 para evitar que haga multiples llamados al javascript
                    Reload = 1;
                }
            } else {
                //Cada vez que sale de la pagina 1 resetea el valor
                Reload = 0;
            }
        }catch(Exception ex){

        }
    }


    public String obtenerIdentificador2(Context gvContext) {
        TelephonyManager telephonyManager = (TelephonyManager) gvContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(gvContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }

        ActivityCompat.requestPermissions(App, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE}, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.getImei();
        } else {
            return telephonyManager.getDeviceId();
        }
    }

    // La funcion solo se ejecutara al determinar algun error al cargar el webview
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (gvNfcAdapter != null) {
            if (error.getDescription().toString().equals("net::ERR_CACHE_MISS")) {
                Intent intent = new Intent(gvContext, MainActivity.class);
                gvContext.startActivity(intent);
            }
            if (error.getDescription().toString().equals("net::ERR_CONNECTION_ABORTED")) {
                Intent intent = new Intent(gvContext, LocalHomeActivity.class);
                gvContext.startActivity(intent);
            }
            if (error.getDescription().toString().equals("net::ERR_INTERNET_DISCONNECTED")) {
                Intent intent = new Intent(gvContext, LocalHomeActivity.class);
                gvContext.startActivity(intent);
            }
            if (error.getDescription().toString().equals("net::ERR_CONNECTION_REFUSED")) {
                Intent intent = new Intent(gvContext, LocalHomeActivity.class);
                gvContext.startActivity(intent);
            }

        } else {
            view.setVisibility(View.INVISIBLE);
            new ErrorController(gvContext).showErrorDialog();
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView v, String u) {
        v.loadUrl(u);
        if (ContextCompat.checkSelfPermission(this.gvContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        }
        v.setDownloadListener(this);
        return true;
    }

    @Override
    public void onDownloadStart(String url, String userAgent,
                                String contentDisposition, String mimeType,
                                long contentLength) {


        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(url));
        String[] Nombre = contentDisposition.split(";");
        String FNombre = "";
        for (int i = 0; i < Nombre.length; i++) {
            if (Nombre[i].contains("=")) {

                String[] File = Nombre[i].split("=");
                for (int f = 0; f < File.length; f++) {

                    if (!File[f].contains("filename")) {
                        FNombre = File[f].replace("\"", "");
                        break;
                    }
                }
            }
            if (FNombre != "") {
                break;
            }
        }
        mimeType = "application/vnd.android.package-archive";
        request.setMimeType(mimeType);

        String cookies = CookieManager.getInstance().getCookie(url);

        request.addRequestHeader("cookie", cookies);

        request.addRequestHeader("User-Agent", userAgent);

        request.setDescription("Descargando Actualización...");

        request.setTitle(FNombre);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        //request.setDestinationInExternalFilesDir(gvContext,DIRECTORY_DOWNLOADS,FNombre);

        request.setDestinationInExternalPublicDir("/"+DIRECTORY_DOWNLOADS,FNombre);
       // request.setDestinationUri(Uri.parse(DIRECTORY_DOWNLOADS));
        DownloadManager dm = (DownloadManager) App.getSystemService(DOWNLOAD_SERVICE);
        dm.enqueue(request);
        Toast.makeText(this.App, "Descargando Actualización",
                Toast.LENGTH_LONG).show();
    }
}
