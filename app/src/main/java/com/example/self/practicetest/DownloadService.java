package com.example.self.practicetest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service{
    private static final String APPTAG = "Download Service";

    private DownloadTask mDownloadTask;

    private String mDownloadUrl;

    private DownloadBinder mBinder = new DownloadBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private DownloadListener mListener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            Log.e(APPTAG, "on progress");
            getNotificationManager().notify(1, getNotification("Downloading...", progress));
        }

        @Override
        public void onSuccess() {
            Log.e(APPTAG, "onSuccess");
            mDownloadTask = null;
            stopForeground(true);  // 下载成功时，将前台服务通知关闭，并创建一个下载成功通知
            getNotificationManager().notify(1, getNotification("Download Success", -1));
            makeToast("Download Success");
        }

        @Override
        public void onFailed() {
            // 下载失败了
            Log.e(APPTAG, "onFailed");
            mDownloadTask = null;

            stopForeground(true);  // 下载成功时，将前台服务通知关闭，并创建一个下载成功通知
            getNotificationManager().notify(1, getNotification("Download Failed", -1));
            makeToast("Download Failed");
        }

        @Override
        public void onPaused() {
            Log.e(APPTAG, "onPaused");
            mDownloadTask = null;
            makeToast("Download Paused");
        }

        @Override
        public void onCanceled() {
            Log.e(APPTAG, "onFailed");
            mDownloadTask = null;
            stopForeground(true);  // 下载成功时，将前台服务通知关闭，并创建一个下载成功通知
            makeToast("Download Failed");
        }
    };

    class DownloadBinder extends Binder{

        public void startDownload(String url) {
            Log.e(APPTAG, "startDownload");
            if (mDownloadTask == null) {
                mDownloadUrl = url;
                mDownloadTask = new DownloadTask(mListener);
                mDownloadTask.execute(mDownloadUrl); //execute download task
                startForeground(1, getNotification("Downloading...", 0));
                makeToast("start download...");
            }
        }

        public void pauseDownload() {
            if (mDownloadTask != null) {
                Log.e(APPTAG, "pauseDownload");
                mDownloadTask.pauseDownload();
            }
        }

        public void cancelDownload() {
            Log.e(APPTAG, "cancelDownload");
            if (mDownloadTask != null) {
                mDownloadTask.cancelDownload();
            }
            if (mDownloadUrl != null) {
                String fileName = mDownloadUrl.substring(mDownloadUrl.lastIndexOf("/"));
                String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(directory + fileName);
                if (file.exists()) {
                    file.delete();
                }
                getNotificationManager().cancel(1);
                stopForeground(true);
                makeToast("Cancel Download");
            }
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        if (progress > 0) {
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }

    private void makeToast(String string) {
        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
    }
}
