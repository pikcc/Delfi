package com.steliosquotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int cRequest_ImportFile = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //load the layout
        setContentView(R.layout.activity_main);

        //the the filename
        updateFilePath();

        //enable button action "openFile"
        final Button btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scheduleOpenFile(null);
            }
        });

        final Button btnShowQuote = findViewById(R.id.btnShowQuote);
        btnShowQuote.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scheduleNotification();
            }
        });

        //show version information
        final TextView version = findViewById(R.id.tvVersionInfo);
        PackageManager manager = this.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
            version.setText("Version " + info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //evaluate file picker result
        if (requestCode == cRequest_ImportFile) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    //make the file persistently accessible
                    getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION));
                }

                //store the new path
                saveFilePath(uri);

                //show the content on the view
                updateFilePath();

                //notify the broadcast receiver
                scheduleNotification();
            }
        }
    }

    private void updateFilePath() {
        String newValue = getString(R.string.prefFilenameInvalid);
        final TextView fileInfo = findViewById(R.id.tvFileInfo);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (fileInfo != null && prefs != null) {
            String filePath = prefs.getString(getString(R.string.prefFilename), null);
            if (filePath != null) {
                final Uri fileUri = Uri.parse(filePath);
                if (fileUri != null) {
                    newValue = getFileName(fileUri);
                }
            }
        }
        fileInfo.setText(newValue);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver != null) {
                Cursor cursor = null;
                try {
                    cursor = contentResolver.query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } catch (Exception e) {
                    //we don't care
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void saveFilePath(Uri aUri) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(getString(R.string.prefFilename), aUri.toString());
        editor.commit();
    }

    private void scheduleOpenFile(Uri aInitialUri) {
        Intent intent = new Intent();
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            //to make it run with my old samsung ;o)
            //If you change these, please also change the
            //onActivityResult (see: takePersistableUriPermission)
            intent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            //HINT: you have to set the FLAG_GRANT_READ_URI_PERMISSION to do that
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        if (aInitialUri != null) {
            //show the latest used file location
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, aInitialUri);
        }

        startActivityForResult(intent, cRequest_ImportFile);
    }

    private void scheduleNotification() {
        Intent notificationIntent = new Intent(this, QuotesNotifier.class);
        sendBroadcast(notificationIntent);
    }


}