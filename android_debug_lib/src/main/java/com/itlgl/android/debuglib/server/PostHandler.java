package com.itlgl.android.debuglib.server;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.itlgl.android.debuglib.AndroidDebugLib;
import com.itlgl.android.debuglib.server.model.ApiRequest;
import com.itlgl.android.debuglib.server.model.ApiResponse;
import com.itlgl.android.debuglib.utils.AppUtils;
import com.itlgl.android.debuglib.utils.LogUtils;
import com.itlgl.java.util.ByteUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class PostHandler extends HttpMethodHandler {

    private final Gson mGson = new Gson();

    public PostHandler(Context context) {
        super(context);
    }

    @Override
    public NanoHTTPD.Response handleMethod(NanoHTTPD.IHTTPSession session) throws Exception {
        String strUri = session.getUri();
        String strUriLower = strUri.toLowerCase();

        // api接口
        if (strUriLower.startsWith("/api")) {
            if (strUri.endsWith("/")) {
                strUri = strUri.substring(strUri.length() - 1);
            }

            Map<String, String> jsonFiles = new HashMap<>();
            session.parseBody(jsonFiles);
            String jsonStr = jsonFiles.get("postData");
            LogUtils.i(jsonStr);

            ApiRequest request = mGson.fromJson(jsonStr, ApiRequest.class);

            if (strUriLower.equals("/api/toast")) {
                return responseToast(session, request);
            }
            if (strUriLower.equals("/api/clipboard")) {
                return responseClipboard(session, request);
            }
            if (strUriLower.equals("/api/command")) {
                return responseCommand(session, request);
            }
            if (strUriLower.equals("/api/console")) {
                return responseConsole(session, request);
            }
        }

        return Utils.response404(strUri);
    }

    private NanoHTTPD.Response responseToast(NanoHTTPD.IHTTPSession session, ApiRequest request) {
        responseConsole(session, request);

        if (TextUtils.isEmpty(request.message)) {
            return Utils.responseJson(mGson.toJson(new ApiResponse(1)));
        }
        AppUtils.showToast(mContext, request.message);
        return Utils.responseJson(mGson.toJson(new ApiResponse(0)));
    }

    private NanoHTTPD.Response responseClipboard(NanoHTTPD.IHTTPSession session, ApiRequest request) {
        if (TextUtils.isEmpty(request.message)) {
            return Utils.responseJson(mGson.toJson(new ApiResponse(1)));
        }
        AppUtils.copyToClipboardThread(mContext, request.message);
        return Utils.responseJson(mGson.toJson(new ApiResponse(0)));
    }

    private synchronized NanoHTTPD.Response responseCommand(NanoHTTPD.IHTTPSession session, ApiRequest request) {
        AndroidDebugLib.ICommand commandImpl = AndroidDebugLib.getCommandImpl();
        if (commandImpl == null) {
            String result = mGson.toJson(new ApiResponse(ApiResponse.CODE_NOT_REGISTER_IMPL));
            return Utils.responseJson(result);
        }

        try {
            byte[] command = ByteUtils.fromHex(request.message);

            if (command == null || command.length == 0) {
                String result = mGson.toJson(new ApiResponse(ApiResponse.CODE_COMMAND_INVALID));
                return Utils.responseJson(result);
            }

            byte[] commandResponse = commandImpl.handleCommand(command);

            ApiResponse response = new ApiResponse(ApiResponse.CODE_SUCCESS, ByteUtils.toHex(commandResponse));
            String result = mGson.toJson(response);
            return Utils.responseJson(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String result = mGson.toJson(new ApiResponse(ApiResponse.CODE_EXECUTE_COMMAND_ERROR));
        return Utils.responseJson(result);
    }

    private synchronized NanoHTTPD.Response responseConsole(NanoHTTPD.IHTTPSession session, ApiRequest request) {

        try {
            final Process process = Runtime.getRuntime().exec("sh");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            System.out.println("out -> " + line);
                        }
                    } catch (Exception e) {
                        System.out.println("out read error -> " + e);
                        //e.printStackTrace();
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            System.out.println("err -> " + line);
                        }
                    } catch (Exception e) {
                        System.out.println("err read error -> " + e);
                        //e.printStackTrace();
                    }
                }
            }).start();

            process.getOutputStream().write("cd /sdcard\n".getBytes());
            Thread.sleep(500);
            process.getOutputStream().write("ls\n".getBytes());
            Thread.sleep(500);
            process.getOutputStream().write("pwd\n".getBytes());
            Thread.sleep(500);
            process.getOutputStream().write("exit\n".getBytes());
            Thread.sleep(500);
            process.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Utils.responseJson(mGson.toJson(new ApiResponse(ApiResponse.CODE_SUCCESS)));
    }
}
