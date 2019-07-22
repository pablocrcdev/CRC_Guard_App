package com.guard.security.crc.crc_guard_app.webview;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.guard.security.crc.crc_guard_app.R;
import com.guard.security.crc.crc_guard_app.activities.MainActivity;

import java.io.File;
import java.io.IOException;

public class ManagerChromeClient extends WebChromeClient {
    private static final int gvFILECHOOSER_RESULTCODE = 1;
    private ProgressBar gvProgressBar;
    public MainActivity app;

    public ManagerChromeClient(ProgressBar pProgressBar, MainActivity main) {
        this.gvProgressBar = pProgressBar;
        this.app = main;
    }

    public void onProgressChanged(WebView view, int progress) {
        if (progress < 100 && gvProgressBar.getVisibility() == ProgressBar.GONE) {
            gvProgressBar.setVisibility(ProgressBar.VISIBLE);
        }
        gvProgressBar.setProgress(progress);
        if (progress == 100) {
            gvProgressBar.setVisibility(ProgressBar.GONE);
        }
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        if (app.gvFilePathCallback != null) {
            app.gvFilePathCallback.onReceiveValue(null);
        }
        app.gvFilePathCallback = filePathCallback;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(app.getPackageManager()) != null) {

            // create the file where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", app.gvCameraPhotoPath);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("UPLOADFILE", "No se pudo tomar la imagen.", ex);
            }

            // continue only if the file was successfully created
            if (photoFile != null) {
                app.gvCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
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
        chooserIntent.putExtra(Intent.EXTRA_TITLE, app.getString(R.string.app_name));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        app.startActivityForResult(chooserIntent, gvFILECHOOSER_RESULTCODE);

        return true;
    }

    // creating image files (Lollipop only)
    private File createImageFile() throws IOException {

        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs();
        }

        // create an image file name
        imageStorageDir = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
        return imageStorageDir;
    }

    // openFileChooser for Android 3.0+
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        app.gvUploadMessage = uploadMsg;

        try {
            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

            if (!imageStorageDir.exists()) {
                imageStorageDir.mkdirs();
            }

            File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

            app.gvCapturedImageURI = Uri.fromFile(file); // save to the private variable

            final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, app.gvCapturedImageURI);

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");

            Intent chooserIntent = Intent.createChooser(i, app.getString(R.string.app_name));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

            app.startActivityForResult(chooserIntent, gvFILECHOOSER_RESULTCODE);
        } catch (Exception e) {
            //Toast.makeText(getBaseContext(), "Camera Exception:" + e, Toast.LENGTH_LONG).show();
        }

    }

    // openFileChooser for Android < 3.0
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        openFileChooser(uploadMsg, "");
    }

    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        openFileChooser(uploadMsg, acceptType);
    }

}
