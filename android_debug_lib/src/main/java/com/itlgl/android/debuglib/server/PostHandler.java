package com.itlgl.android.debuglib.server;

import android.content.Context;

import fi.iki.elonen.NanoHTTPD;

public class PostHandler extends HttpMethodHandler {

    public PostHandler(Context context) {
        super(context);
    }

    @Override
    public NanoHTTPD.Response handleMethod(NanoHTTPD.IHTTPSession session) throws Exception {
        return null;
    }
}
