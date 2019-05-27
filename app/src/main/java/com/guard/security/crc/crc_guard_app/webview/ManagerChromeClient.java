package com.guard.security.crc.crc_guard_app.webview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.guard.security.crc.crc_guard_app.R;

import java.io.File;
import java.io.IOException;

public class ManagerChromeClient extends WebChromeClient {
    //=============================VARIABLES GLOBALES=============================================//
    private ProgressBar gvProgressBar;
    private Context gvContext;
    // =================== Variables para permisos de android =================== //
    private static final int gvFILECHOOSER_RESULTCODE = 1;
    int gvPERMISSION_ALL = 1;
    String[] gvPERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
    // =============== Usadas para seleccion de archivos nativos =============== //
    private ValueCallback<Uri> gvUploadMessage;
    private Uri gvCapturedImageURI = null;
    private ValueCallback<Uri[]> gvFilePathCallback;
    private String gvCameraPhotoPath;
    //============================================================================================//
    // El contructor se define con el parametro de contexto para refenrenciar siempre al activity
    // que este en primer plano y poder aplicar funciones sobre el mismo
    public ManagerChromeClient(Context pcontext){
        gvContext = pcontext;
    }
    //============================================================================================//
    public void onProgressChanged(WebView view, int progress) {
        // Definimos la variable asociada al progress bar, esto aplicando el casting con el contexto
        // de la app que se este ejecutando y que referencia esta clase
        gvProgressBar = (ProgressBar) ((Activity)gvContext).findViewById(R.id.progressBar);
        // Validamos el progreso y mostramos si esta cargando
        if (progress < 100 && gvProgressBar.getVisibility() == ProgressBar.GONE) {
            gvProgressBar.setVisibility(ProgressBar.VISIBLE);
        }
        // Despues asignamos el valor de progreso a la barra de progreso
        gvProgressBar.setProgress(progress);
        // Si la carga se completo se oculta el item
        if (progress == 100) {
            gvProgressBar.setVisibility(ProgressBar.GONE);
        }
    }
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        if (gvFilePathCallback != null) {
            gvFilePathCallback.onReceiveValue(null);
        }
        gvFilePathCallback = filePathCallback;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(gvContext.getPackageManager()) != null) {

            // create the file where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", gvCameraPhotoPath);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("UPLOADFILE", "Unable to create Image File", ex);
            }

            // continue only if the file was successfully created
            if (photoFile != null) {
                gvCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }
        }
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        Intent[] intentArray;
        if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, gvContext.getString(R.string.app_name));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        ((Activity) gvContext).startActivityForResult(chooserIntent, gvFILECHOOSER_RESULTCODE);

        return true;
    }
    // creating image files (Lollipop only)
    private File createImageFile() throws IOException {

        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs();
        }

        // create an image file name
        imageStorageDir  = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
        return imageStorageDir;
    }

    // openFileChooser for Android 3.0+
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        gvUploadMessage = uploadMsg;

        try {
            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

            if (!imageStorageDir.exists()) {
                imageStorageDir.mkdirs();
            }

            File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

            gvCapturedImageURI = Uri.fromFile(file); // save to the private variable

            final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, gvCapturedImageURI);
            // captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");

            Intent chooserIntent = Intent.createChooser(i, gvContext.getString(R.string.app_name));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});


            ((Activity) gvContext).startActivityForResult(chooserIntent, gvFILECHOOSER_RESULTCODE);

        } catch (Exception e) {
            Toast.makeText(gvContext, "Camera Exception:" + e, Toast.LENGTH_LONG).show();
        }

    }

    // openFileChooser for Android < 3.0
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        openFileChooser(uploadMsg, "");
    }

    // openFileChooser for other Android versions
            /* may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
               https://code.google.com/p/android/issues/detail?id=62220
               however newer versions of KitKat fixed it on some devices */
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        openFileChooser(uploadMsg, acceptType);
    }

}
