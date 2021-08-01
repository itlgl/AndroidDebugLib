package com.itlgl.android.debuglib.server;

import android.content.Context;
import android.text.TextUtils;

import com.itlgl.android.debuglib.utils.LogUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Utils {

    public static final String MIME_TYPE_HTML = "text/html";

    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<>();
    static {
        MIME_TYPE_MAP.put(".html", "text/html");
        MIME_TYPE_MAP.put(".css", "text/css");
        MIME_TYPE_MAP.put(".js", "text/javascript");
        MIME_TYPE_MAP.put(".jpeg", "image/jpeg");
        MIME_TYPE_MAP.put(".jpg", "image/jpeg");
        MIME_TYPE_MAP.put(".png", "image/png");
        MIME_TYPE_MAP.put(".gif", "image/gif");
        MIME_TYPE_MAP.put(".ico", "image/x-icon");
        MIME_TYPE_MAP.put(".svg", "text/xml");
        MIME_TYPE_MAP.put(".ttf", "font/ttf");
        MIME_TYPE_MAP.put(".woff", "font/woff");
        MIME_TYPE_MAP.put(".woff2", "font/woff2");
        MIME_TYPE_MAP.put(".eot", "font/eot");
    }

    private static final String ERROR_HTML = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\"/>\n" +
            "\n" +
            "    <title>Error</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "<header>\n" +
            "    <h4>URI：${URI}</h4>\n" +
            "</header>\n" +
            "<hr/>\n" +
            "<pre>\n" +
            "${errorStack}\n" +
            "</pre>\n" +
            "<hr/>\n" +
            "</body>\n" +
            "</html>";

    private static final String HTML_404 = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\"/>\n" +
            "\n" +
            "    <title>Error</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "<header>\n" +
            "    <h4>URI：${URI}</h4>\n" +
            "</header>\n" +
            "<hr/>\n" +
            "<pre>\n" +
            "Not found\n" +
            "</pre>\n" +
            "<hr/>\n" +
            "</body>\n" +
            "</html>";
    private static final DecimalFormat FILE_SIZE_FORMAT = new DecimalFormat("#,##0.#");

    private Utils() {
        // This class in not publicly instantiable
    }

    public static String detectMimeType(String mimeType) {
        if(MIME_TYPE_MAP.containsKey(mimeType)) {
            return MIME_TYPE_MAP.get(mimeType);
        }
        return "application/octet-stream";
    }

    public static String getThrowableStackTraceString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public static String readAssetsFileString(Context context, String fileName) throws IOException {
        InputStream is = context.getAssets().open("android_debug_lib/" + fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line).append("\n");
        }
        br.close();
        return builder.toString();
    }

    public static NanoHTTPD.Response errorResponse(String uri, Throwable t) {
        String exStr = Utils.getThrowableStackTraceString(t);
        String errorHtml = ERROR_HTML.replace("${URI}", uri).replace("${errorStack}", exStr);
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, errorHtml);
    }

    public static NanoHTTPD.Response responseAssetsFile(Context context, String strUri) {
        try {
            String exName = strUri.substring(strUri.lastIndexOf('.'));
            String miniType = MIME_TYPE_MAP.get(exName);
            if(miniType == null) miniType = "application/octet-stream";
            String fileName = strUri.substring(1);
            InputStream is = context.getAssets().open("android_debug_lib/" + fileName);
            // logd("responseResourceFile is = " + is + ",miniType = " + miniType);

            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, miniType, is, is.available());
        } catch (Exception e) {
            //e.printStackTrace();
            LogUtils.i("response strUir=%s error, e=%s, return 404", strUri, e);
            return response404(strUri);
        }
    }

    public static NanoHTTPD.Response response404(String strUri) {
        String html = HTML_404.replace("${URI}", strUri);
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML, html);
    }

    public static String formatFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return FILE_SIZE_FORMAT.format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
