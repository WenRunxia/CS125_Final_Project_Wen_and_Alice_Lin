package com.example.finalproject;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;


public final class MainActivity extends AppCompatActivity {
    private static final int IMAGE_CAPTURE_REQUEST_CODE = 1;
    private static final String TAG = "photo catcher";
    private boolean canWriteToPublicStorage = false;
    private static final int READ_REQUEST_CODE = 43;
    private static final int REQUEST_WRITE_STORAGE = 112;
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageButton camera = findViewById(R.id.camera);
        camera.setOnClickListener(v -> {
            Log.d(TAG, "camera button clicked");
            startCamera();
        });
        final Button reset = findViewById(R.id.reset);
        findViewById(R.id.start).setOnClickListener(v -> {
            startInfo();
        });
        final ImageView menu = findViewById(R.id.menu);
        enableOrDisableButtons(false);

        canWriteToPublicStorage = (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        Log.d(TAG, "Do we have permission to write to external storage: "
                + canWriteToPublicStorage);
        if (!canWriteToPublicStorage) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
    }
    private boolean photoRequestActive = false;
    private File currentPhotoFile = null;
    private void startCamera() {
        if (photoRequestActive) {
            Log.w(TAG, "Overlapping photo requests");
            return;
        }
        // Set up an intent to launch the camera app and have it take a photo for us
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        currentPhotoFile = getSaveFilename();
        if (takePictureIntent.resolveActivity(getPackageManager()) == null
                || currentPhotoFile == null) {
            // Alert the user if there was a problem taking the photo
            Toast.makeText(getApplicationContext(), "Problem taking photo",
                    Toast.LENGTH_LONG).show();
            Log.w(TAG, "Problem taking photo");
            return;
        }

        // Configure and launch the intent
        Uri photoURI = FileProvider.getUriForFile(this,
                "com.example.finalproject.fileprovider", currentPhotoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        photoRequestActive = true;
        startActivityForResult(takePictureIntent, IMAGE_CAPTURE_REQUEST_CODE);
    }
    public void onActivityResult(final int requestCode, final int resultCode,
                                 final Intent resultData) {

        // If something went wrong we simply log a warning and return
        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "onActivityResult with code " + requestCode + " failed");
            if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
                photoRequestActive = false;
            }
            return;
        }

        // Otherwise we get a link to the photo either from the file browser or the camera,
        Uri currentPhotoURI;
        if (requestCode == READ_REQUEST_CODE) {
            currentPhotoURI = resultData.getData();
        } else if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
            currentPhotoURI = Uri.fromFile(currentPhotoFile);
            photoRequestActive = false;
        } else {
            Log.w(TAG, "Unhandled activityResult with code " + requestCode);
            return;
        }

        // Now load the photo into the view
        Log.d(TAG, "Photo selection produced URI " + currentPhotoURI);
        loadPhoto(currentPhotoURI);
    }
    void startInfo() {
        Intent intent = new Intent(this, Dish_Screen.class);
        startActivity(intent);
    }
    private void loadPhoto(final Uri currentPhotoURI) {
        enableOrDisableButtons(false);

        if (currentPhotoURI == null) {
            Toast.makeText(getApplicationContext(), "No image selected",
                    Toast.LENGTH_LONG).show();
            Log.w(TAG, "No image selected");
            return;
        }
        String uriScheme = currentPhotoURI.getScheme();

        byte[] imageData;
        try {
            assert uriScheme != null;
            switch (uriScheme) {
                case "file":
                    imageData = FileUtils.readFileToByteArray(new File(currentPhotoURI.getPath()));
                    break;
                case "content":
                    InputStream inputStream = getContentResolver().openInputStream(currentPhotoURI);
                    assert inputStream != null;
                    imageData = IOUtils.toByteArray(inputStream);
                    inputStream.close();
                    break;
                default:
                    Toast.makeText(getApplicationContext(), "Unknown scheme " + uriScheme,
                            Toast.LENGTH_LONG).show();
                    return;
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error processing file",
                    Toast.LENGTH_LONG).show();
            Log.w(TAG, "Error processing file: " + e);
            return;
        }

        /*
         * Resize the image appropriately for the display.
         */
        final ImageView photoView = findViewById(R.id.menu);
        int targetWidth = photoView.getWidth();
        int targetHeight = photoView.getHeight();

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageData, 0, imageData.length, decodeOptions);

        int actualWidth = decodeOptions.outWidth;
        int actualHeight = decodeOptions.outHeight;
        int scaleFactor = Math.min(actualWidth / targetWidth, actualHeight / targetHeight);

        BitmapFactory.Options modifyOptions = new BitmapFactory.Options();
        modifyOptions.inJustDecodeBounds = false;
        modifyOptions.inSampleSize = scaleFactor;

        // Actually draw the image
        updateCurrentBitmap(BitmapFactory.decodeByteArray(imageData,
                0, imageData.length, modifyOptions));
    }
    File getSaveFilename() {
        String imageFileName = "Menu dish" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        File storageDir;
        if (canWriteToPublicStorage) {
            storageDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        } else {
            storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Log.w(TAG, "Problem saving file: " + e);
            return null;
        }
    }
    private Bitmap currentBitmap;
    void updateCurrentBitmap(final Bitmap setCurrentBitmap) {
        currentBitmap = setCurrentBitmap;
        ImageView photoView = findViewById(R.id.menu);
        photoView.setImageBitmap(currentBitmap);
        enableOrDisableButtons(true);
    }
    private void enableOrDisableButtons(final boolean enableOrDisable) {

    }
}
