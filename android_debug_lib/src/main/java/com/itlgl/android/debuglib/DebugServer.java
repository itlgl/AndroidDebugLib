package com.itlgl.android.debuglib;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.itlgl.android.debuglib.utils.AppUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.itlgl.android.debuglib.utils.DatabaseUtils;
import com.itlgl.java.util.ByteUtil;

import fi.iki.elonen.NanoHTTPD;

public class DebugServer extends NanoHTTPD {
    private static final String TAG = "DebugServer";
    private static final String REQUEST_ROOT = "/";
    private static final Map<String, String> MINI_TYPE_MAP = new HashMap<>();
    static {
        MINI_TYPE_MAP.put(".html", "text/html");
        MINI_TYPE_MAP.put(".css", "text/css");
        MINI_TYPE_MAP.put(".js", "text/javascript");
        MINI_TYPE_MAP.put(".jpeg", "image/jpeg");
        MINI_TYPE_MAP.put(".jpg", "image/jpeg");
        MINI_TYPE_MAP.put(".png", "image/png");
        MINI_TYPE_MAP.put(".ico", "image/x-icon");
        MINI_TYPE_MAP.put(".svg", "text/xml");
        MINI_TYPE_MAP.put(".ttf", "font/ttf");
        MINI_TYPE_MAP.put(".woff", "font/woff");
        MINI_TYPE_MAP.put(".eot", "font/eot");
    }

    private Context context;
    private DebugServerCallback debugServerCallback;

    public DebugServer(Context context, int port) {
        super(port);
        this.context = context;
    }

    public void setDebugServerCallback(DebugServerCallback callback) {
        this.debugServerCallback = callback;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String strUri = session.getUri();
        logi("request serve uri = " + strUri + ", method = " + method);
        try {
            if(Method.GET.equals(method)) {
                return handleGetMethod(session);
            } else if(Method.POST.equals(method)) {
                return handlePostMethod(session);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return responseException(session, e);
        }

        return response404(session);
    }

    private Response responseException(IHTTPSession session, Exception e) {
        try {
            String strUri = session.getUri();

            String errorStack = "";
            if(e != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                errorStack = sw.toString();
            }

            String errorHtml = readAssetsFileString("error.html");
            errorHtml = errorHtml.replace("${URI}", strUri).replace("${errorStack}", errorStack);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", errorHtml);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return response404(session);
    }

    private Response handleGetMethod(IHTTPSession session) throws Exception {
        String strUri = session.getUri();

        if (REQUEST_ROOT.equals(strUri)) {
            return responseRootPage(session);
        }

        // app的相关接口
        if(strUri.toLowerCase().startsWith("/app")) {
            File file = new File(strUri.substring("/app".length()));
            if(!file.exists()) {
                return response404(session);
            }
            if(file.isDirectory()) {
                return fileListResponse(session, file);
            } else {
                return fileViewResponse(session, file);
            }
        }

        // 导出手机中的文件
        if(strUri.toLowerCase().startsWith("/download")) {
            File file = new File(strUri.substring("/download".length()));
            if(!file.exists()) {
                return response404(session);
            }
            if(file.isDirectory()) {
                return response404(session);
            }
            if(!file.canRead()) {
                return response404(session);
            }
            InputStream is = new FileInputStream(file);
            return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", is, is.available());
        }

        return responseAssetsFile(session, strUri);
    }

    private Response handlePostMethod(IHTTPSession session) throws Exception {
        String strUri = session.getUri();
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, String> params = session.getParms();
        logi("post: params=" + params);

        if(strUri.startsWith("/message")) {
            return responsePostMessage(session);
        } else if(strUri.startsWith("/app") && strUri.endsWith(".db")) {
            return responsePostExecSQL(session);
        }

        return responseTextError("uri=" + strUri + ",can not handle this request!");
    }

    private Response responsePostExecSQL(IHTTPSession session) throws Exception {
        String strUri = session.getUri();
        File dbFile = new File(strUri.substring("/app".length()));
        if(dbFile == null || !dbFile.exists()) {
            return responseTextError("db file not exist!");
        }
        String sql = session.getParms().get("sql");
        if(TextUtils.isEmpty(sql)) {
            return responseTextError("sql is null!");
        }
        String sqlResult = DatabaseUtils.execSQL(dbFile, sql);
        return responseText(sqlResult);
    }

    private Response responsePostMessage(IHTTPSession session) throws Exception {
        String strUri = session.getUri();
        Map<String, String> params = session.getParms();
        String method = params.get("method");
        String param = params.get("param");
        if(TextUtils.isEmpty(method)) {
            return responseTextError("uri=" + strUri + ",method is null!");
        }
        if("sendApdu".equalsIgnoreCase(method)) {
            if(debugServerCallback == null) {
                return responseTextError("uri=" + strUri + ",callback not impl!");
            }
            byte[] apdu = ByteUtil.fromHex(param);
            byte[] bytes = debugServerCallback.handleApdu(apdu);
            return responseText(ByteUtil.toHex(bytes));
        } else if("sendClipboard".equalsIgnoreCase(method)) {
            if(TextUtils.isEmpty(param)) {
                return responseTextError("uri=" + strUri + ",param is null!");
            }

            //获取剪贴板管理器：
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText(param, param);
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);

            return responseText("copy '" + param + "' to mobile clipboard");
        } else if("sendMessage".equalsIgnoreCase(method)) {
            if(debugServerCallback == null) {
                return responseTextError("uri=" + strUri + ",callback not impl!");
            }
            debugServerCallback.handleMessage(param);
            return responseText("send message success");
        } else {
            if(debugServerCallback == null) {
                return responseTextError("uri=" + strUri + ",callback not impl!");
            }
            String commandResponse = debugServerCallback.handleCommand(method, param);
            return responseText(commandResponse);
        }
    }

    private Response responseRootPage(IHTTPSession session) throws Exception {
        String indexStr = readAssetsFileString("index.html");

        String appName = AppUtils.getAppName(context);
        if(appName == null) {
            appName = "";
        }

        String appVersionName = AppUtils.getVersionName(context);
        if(appVersionName == null) {
            appVersionName = "";
        }

        String appVersionCode = AppUtils.getVersionCode(context) + "";

        String html = indexStr
                .replace("${appName}", appName)
                .replace("${appVersionName}", appVersionName)
                .replace("${appVersionCode}", appVersionCode)
                .replace("${packageName}", context.getPackageName());
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response responseAssetsFile(IHTTPSession session, String strUri) throws Exception {
        String exName = strUri.substring(strUri.lastIndexOf('.'));
        String miniType = MINI_TYPE_MAP.get(exName);
        if(miniType == null) miniType = "*";
        String fileName = strUri.substring(1);
        InputStream is = context.getAssets().open(fileName);
        // logd("responseResourceFile is = " + is + ",miniType = " + miniType);

        return newFixedLengthResponse(Response.Status.OK, miniType, is, is.available());
    }

    private Response responseText(String text) {
        return newFixedLengthResponse(Response.Status.OK, "text/plain", text);
    }

    private Response responseTextError(String errorText) {
        return newFixedLengthResponse(Response.Status.NOT_ACCEPTABLE, "text/plain", errorText);
    }

    private Response responseJson(IHTTPSession session, Object jsonObj) {
        return responseJson(session, new Gson().toJson(jsonObj));
    }

    private Response responseJson(IHTTPSession session, String jsonMsg) {
        return newFixedLengthResponse(Response.Status.OK, "application/json", jsonMsg);
    }

    // 50 个空格
    static final String FILE_ITEM_SPACE = "                                                  ";
    static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd mm:ss");

    private Response fileListResponse(IHTTPSession session, File directory) throws Exception {
        String fileListStr = readAssetsFileString("file_list.html");
        String fileListItemTemp = readAssetsFileString("file_list_item.template");
        StringBuilder builder = new StringBuilder();
        do {
            if(directory == null || !directory.isDirectory() || !directory.canRead()) {
                break;
            }
            File[] files = directory.listFiles();
            if(files == null || files.length == 0) {
                break;
            }
            for (File childFile : files) {
                String fileDate = "";
                try {
                    fileDate = SIMPLE_DATE_FORMAT.format(new Date(childFile.lastModified()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String item = fileListItemTemp
                        .replace("${href}", childFile.getName() + (childFile.isDirectory() ? "/" : ""))
                        .replace("${title}", childFile.getName())
                        .replace("${text}", childFile.getName())
                        .replace("${date}", fileDate)
                        .replace("${space}", FILE_ITEM_SPACE.substring(childFile.getName().length()));
                if(childFile.isFile() && childFile.canRead()) {
                    item = item
                            .replace("${style3}", "")
                            .replace("${href3}", "/download" + childFile.getAbsolutePath());
                } else {
                    item = item
                            .replace("${style3}", "visibility:hidden;");
                }
                builder.append(item).append("\n");
            }
        } while (false);
        String htmlStr = fileListStr
                .replace("${file_list}", builder.toString())
                .replace("${path}", directory == null ? "path" : directory.getAbsolutePath())
                .replace("${packageName}", context.getPackageName())
                .replace("${title}", directory == null ? "title" : directory.getAbsolutePath());
        return newFixedLengthResponse(Response.Status.OK, "text/html", htmlStr);
    }

    private Response fileViewResponse(IHTTPSession session, File file) throws Exception {
        if(file == null || file.isDirectory()) {
            return response404(session);
        }
        if(file.getName().endsWith(".db")) {
            return fileViewDbResponse(session, file);
        }
        if(file.getName().endsWith(".xml")) {
            return fileViewXmlResponse(session, file);
        }

        return responseText("can not view file:" + file.getName());
    }

    private Response fileViewXmlResponse(IHTTPSession session, File file) throws Exception {
        String xmlStr = readFileString(file);
        return newFixedLengthResponse(Response.Status.OK, "application/xml", xmlStr);
    }

    private Response fileViewDbResponse(IHTTPSession session, File dbFile) throws Exception {
        String html = readAssetsFileString("file_view_db.html");
        String dbInfo = DatabaseUtils.getDbTablesInfo(dbFile);
        html = html
                .replace("${title}", dbFile.getName())
                .replace("${dbInfo}", dbInfo)
                .replace("${path}", dbFile.getAbsolutePath());
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private String readFileString(File file) throws Exception {
        InputStream is = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

    private String readAssetsFileString(String fileName) throws Exception {
        InputStream is = context.getAssets().open(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

    /**
     * 调用的路径出错
     *
     * @param session
     * @return
     */
    private Response response404(IHTTPSession session) {
        String url = session.getUri();
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><body>");
        builder.append("Sorry, Can't Found " + url + " !");
        builder.append("</body></html>\n");
        String text = builder.toString();
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", text);
    }

    private static void logd(String msg) {
        Log.d(TAG, msg);
    }

    private static void logi(String msg) {
        Log.i(TAG, msg);
    }
}
