package com.itlgl.android.debuglib.server;

import android.content.Context;

import com.itlgl.android.debuglib.utils.LogUtils;

import fi.iki.elonen.NanoHTTPD;

public class AndroidDebugServer extends NanoHTTPD {
    private final Context mContext;
    private final GetHandler mGetHandler;
    private final PostHandler mPostHandler;

    public AndroidDebugServer(Context context, int port) {
        super(port);

        mContext = context;
        mGetHandler = new GetHandler(mContext);
        mPostHandler = new PostHandler(mContext);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String strUri = session.getUri();
        LogUtils.i("request serve uri = %s, method = %s", strUri, method);

        Response response = null;
        try {
            if(Method.GET.equals(method)) {
                response = mGetHandler.handleMethod(session);
            } else if(Method.POST.equals(method)) {
                response = mPostHandler.handleMethod(session);
            }

            return response;
        } catch (Exception e) {
            return Utils.errorResponse(strUri, e);
        }
    }
}
