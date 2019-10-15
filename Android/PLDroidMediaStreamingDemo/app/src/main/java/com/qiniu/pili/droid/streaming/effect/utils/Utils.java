package com.qiniu.pili.droid.streaming.effect.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Utils {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Is the live streaming still available
     * @return is the live streaming is available
     */
    public static boolean isLiveStreamingAvailable() {
        // Todo: Please ask your app server, is the live streaming still available
        return true;
    }

    public static void showToastTips(final Context context, final String tips) {
        Toast.makeText(context, tips, Toast.LENGTH_SHORT).show();
    }

    public static String requestPublishUrl(String paramString) { return doGetRequest("http://api-demo.qnsdk.com/v1/live/stream/" + paramString); }

    public static String requestPlayUrl(String paramString) { return doGetRequest("http://api-demo.qnsdk.com/v1/live/play/" + paramString + "/rtmp"); }

    private static String doGetRequest(String paramString) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = (new Request.Builder()).url(paramString).build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful())
                return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
