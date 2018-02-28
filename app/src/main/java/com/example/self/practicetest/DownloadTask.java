package com.example.self.practicetest;


import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer>{
    private static final String APPTAG = "DownloadTask";

    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener mListener;

    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    public DownloadTask(DownloadListener listener) {
        mListener = listener;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        Log.e(APPTAG, "do in background");
        InputStream inputStream = null;
        RandomAccessFile savedFile = null;
        File file = null;

        try {
            if (strings.length == 0) {
                Log.e(APPTAG, "failed");
                return TYPE_FAILED;
            }
            long downloadedLength = 0; // 记录已经下载的文件的长度
            String downloadUrl = strings[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            Log.e(APPTAG, "filename: " + fileName);
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            if (file.exists()) {
                downloadedLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);

            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {
                return TYPE_SUCCESS;
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-") // 断点下载，指定从哪个字节开始下载
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();

            if (response != null && response.isSuccessful()) {
                inputStream = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw"); // 指定下载内容要写入的文件， 操作类型为“读写”
                savedFile.seek(downloadedLength); // 跳过已经下载好的字节
                byte[] bytes = new byte[1024];
                int total = 0;
                int len;

                while((len = inputStream.read(bytes)) != -1) {
                    if (isCanceled) {
                        Log.e(APPTAG, "isCanceled");
                        return TYPE_CANCELED;
                    } else if (isPaused) {
                        Log.e(APPTAG, "isPaused");
                        return TYPE_PAUSED;
                    } else {
                        total += len;
                        savedFile.write(bytes, 0, len);

                        int progress = (int) ((total + downloadedLength) * 100 / contentLength); // 计算下载的百分比
                        publishProgress(progress);
                    }
                }

                response.body().close();
                return TYPE_SUCCESS;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (savedFile != null) {
                    savedFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete(); // 下载活动被取消，且系统中有下载文件存在，则将文件删除
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            mListener.onProgress(progress); // 更新下载的 progress bar
            lastProgress = progress; // 更新最新的下载长度
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        Log.e(APPTAG, "on progress Executed");
        switch (status) {
            case TYPE_SUCCESS:
                mListener.onSuccess();
                break;
            case TYPE_CANCELED:
                mListener.onCanceled();
                break;
            case TYPE_PAUSED:
                mListener.onPaused();
                break;
            case TYPE_FAILED:
                mListener.onFailed();
                break;
            default:
                break;
        }
    }

    public void pauseDownload() {
        Log.e(APPTAG, "pause download");
        isPaused = true;
    }

    public void cancelDownload() {
        Log.e(APPTAG, "cancel download");
        isCanceled = true;
    }
}
