package com.itlgl.android.debuglib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class AndroidDebugLib {
    private static DebugServer debugServer;
    private static Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public static void startDebug(final Context context, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (AndroidDebugLib.class) {
                    try {
                        if(debugServer != null) {
                            if(debugServer.isAlive()) {
                                debugServer.stop();
                            }
                            debugServer = null;
                        }
                        debugServer = new DebugServer(context, port);
                        debugServer.start();
                        showToast(context, "Debug Server start on port " + port);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast(context, "Debug Server start Error!\n" + e);
                    }
                }
            }
        }).start();
    }

    public static void stopDebug() {
        if(debugServer == null || !debugServer.isAlive()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (AndroidDebugLib.class) {
                    debugServer.stop();
                }
            }
        }).start();
    }

    public static void registerDebugCallback(DebugServerCallback callback) {
        if(debugServer != null) {
            debugServer.setDebugServerCallback(callback);
        }
    }

    private static void showToast(final Context context, final String msg) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
