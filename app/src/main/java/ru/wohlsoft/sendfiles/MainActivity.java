package ru.wohlsoft.sendfiles;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private String m_lastPath = Environment.getExternalStorageDirectory().getPath();
    ;
    public static final MediaType ANY = MediaType.parse("application/octet-stream");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openfb = (Button) findViewById(R.id.dropAFile);
        openfb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnOpenFileClick(view);
            }
        });
    }

    class DropAFileTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... urlAndJson) {
            try {
                File file = new File(urlAndJson[1]);

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("action", "oneday")
                        .addFormDataPart("File1", urlAndJson[1],
                                RequestBody.create(ANY, file))
                        .build();

                Request request = new Request.Builder()
                        .url(urlAndJson[0])
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                String sentFile = response.body().string();
                return sentFile;
            } catch (Exception e) {
                return e.toString();
            }
        }
    }

    String post(String url, String file) throws IOException {
        DropAFileTask d = new DropAFileTask();
        d.execute(url, file);
        try {
            return d.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return "SHIT HAPPEN!";
    }

    public void OnOpenFileClick(View view) {
        // Here, thisActivity is the current activity
        if (
                (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)
                        ||

                        (ContextCompat.checkSelfPermission(this,
                                Manifest.permission.INTERNET)
                                != PackageManager.PERMISSION_GRANTED)
                ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.INTERNET)
                    ) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle("Permission denied");
                b.setMessage("Sorry, but permission is denied!\n" +
                        "Please, check permissions to application!");
                b.setNegativeButton(android.R.string.ok, null);
                b.show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET}, 1);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            OpenFileDialog fileDialog = new OpenFileDialog(this)
                    //.setFilter(".*\\.mid|.*\\.MID|.*\\.kar|.*\\.KAR|.*\\.rmi|.*\\.RMI|.*\\.imf|.*\\.IMF")
                    .setCurrentDirectory(m_lastPath)
                    .setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
                        @Override
                        public void OnSelectedFile(String fileName, String lastPath) {
                            //Toast.makeText(getApplicationContext(), fileName, Toast.LENGTH_LONG).show();
                            m_lastPath = lastPath;
                            try {
                                String response = post("http://wohlnet.ru/sentfile/cmd_send_nolog.php", fileName);

                                if (response.startsWith("http://")) {
                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText("Sent file", response);
                                    clipboard.setPrimaryClip(clip);
                                }

                                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                            }
                        }
                    });
            fileDialog.show();
        }
    }

}
