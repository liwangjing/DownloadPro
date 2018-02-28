package com.example.self.practicetest;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    AppCompatButton mStart, mPause, mCancel;

    private DownloadService.DownloadBinder mBinder;
    private Intent intent;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBinder = (DownloadService.DownloadBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        intent = new Intent(this, DownloadService.class);
        startService(intent);

        bindService(intent, mConnection, BIND_AUTO_CREATE);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void initView() {
        mStart = (AppCompatButton) findViewById(R.id.btn_download_start);
        mPause = (AppCompatButton) findViewById(R.id.btn_download_pause);
        mCancel = (AppCompatButton) findViewById(R.id.btn_download_stop);

        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBinder != null) {
                    String url = "http://raw.githubusercontent.com/guolindev/eclipse/master/eclipse-inst-win64.exe";
                    mBinder.startDownload(url);
                }
            }
        });

        mPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBinder != null) {
                    mBinder.pauseDownload();
                }
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBinder != null) {
                    mBinder.cancelDownload();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    makeToast("permission denied..");
                }
                break;
            case 2:
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        stopService(intent);
    }

    private void makeToast(String string) {
        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
    }
}
