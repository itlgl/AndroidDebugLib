package com.itlgl.android.debuglib;

import android.content.Context;

import java.io.IOException;

public class AndroidDebugLib {
    private static DebugServer debugServer;

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
                    } catch (IOException e) {
                        e.printStackTrace();
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
}
