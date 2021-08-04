package com.itlgl.android.debuglib.server;

import android.content.Context;

import fi.iki.elonen.NanoHTTPD;

public abstract class HttpMethodHandler {
    protected Context mContext;

    public HttpMethodHandler(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public abstract NanoHTTPD.Response handleMethod(NanoHTTPD.IHTTPSession session) throws Exception;
}
