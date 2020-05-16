package com.itlgl.androiddebugapp;

import android.app.Application;

import com.itlgl.android.debuglib.AndroidDebugLib;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AndroidDebugLib.startDebug(this, 8080);
    }
}
