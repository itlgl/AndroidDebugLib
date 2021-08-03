package com.itlgl.android.debuglib;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;

import com.itlgl.android.debuglib.server.AndroidDebugServer;

import java.io.IOException;

public class AndroidDebugLibInitProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        int port = getContext().getResources().getInteger(R.integer.adl_port);
        AndroidDebugServer server = new AndroidDebugServer(getContext(), port);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo providerInfo) {
        if (providerInfo == null) {
            throw new NullPointerException("DebugDBInitProvider ProviderInfo cannot be null.");
        }
        // So if the authorities equal the library internal ones, the developer forgot to set his applicationId
        if ("com.itlgl.android.debuglib.AndroidDebugLibInitProvider".equals(providerInfo.authority)) {
            throw new IllegalStateException("Incorrect provider authority in manifest. Most likely due to a "
                    + "missing applicationId variable in application's build.gradle.");
        }
        super.attachInfo(context, providerInfo);
    }
}
