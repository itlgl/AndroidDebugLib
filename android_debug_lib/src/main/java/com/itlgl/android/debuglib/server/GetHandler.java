package com.itlgl.android.debuglib.server;

import android.content.Context;

import com.itlgl.android.debuglib.utils.AppUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import fi.iki.elonen.NanoHTTPD;

public class GetHandler extends HttpMethodHandler {

    private static final String URI_ROOT = "/";
    private static final String URI_ROOT2 = "/index.html";

    // 50 个空格
    private static final String FILE_ITEM_SPACE = "                                                  ";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd mm:ss");

    public GetHandler(Context context) {
        super(context);
    }

    @Override
    public NanoHTTPD.Response handleMethod(NanoHTTPD.IHTTPSession session) throws Exception {
        String strUri = session.getUri().toLowerCase();

        if(URI_ROOT.equals(strUri) || URI_ROOT2.equals(strUri)) {
            String indexHtml = Utils.readAssetsFileString(getContext(), "index.html");
            String appName = AppUtils.getAppName(getContext());
            String appPackage = AppUtils.getPackageName(getContext());
            String versionName = AppUtils.getVersionName(getContext());
            int versionCode = AppUtils.getVersionCode(getContext());
            indexHtml = indexHtml.replace("${AppName}", appName)
                    .replace("${AppPackage}", appPackage)
                    .replace("${AppVersionName}", versionName)
                    .replace("${AppVersionCode}", String.valueOf(versionCode));
            return NanoHTTPD.newFixedLengthResponse(indexHtml);
        }

        // app
        if(strUri.startsWith("/app")) {
            File file = new File(strUri.substring("/app".length()));
            if(!file.exists()) {
                return Utils.response404(strUri);
            }
            if(file.isDirectory()) {
                return fileListResponse(session, file);
            } else {
                return fileViewResponse(session, file);
            }
        }

        if(strUri.startsWith("/download")) {
            File file = new File(strUri.substring("/download".length()));
            if(!file.exists() || file.isDirectory()) {
                return Utils.response404(strUri);
            }
            InputStream is = new FileInputStream(file);
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream", is, is.available());
        }

        return Utils.responseAssetsFile(getContext(), strUri);
    }

    private NanoHTTPD.Response fileListResponse(NanoHTTPD.IHTTPSession session, File directory) throws Exception {
        String fileListStr = Utils.readAssetsFileString(getContext(), "file_list.html");
        String fileListItemTemp = Utils.readAssetsFileString(getContext(), "file_list_item.template");
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
                String fileSize = childFile.isFile() && childFile.canRead() ? Utils.formatFileSize(childFile.length()) : "";
                String item = fileListItemTemp
                        .replace("${fileHref}", "/app/" + childFile.getAbsolutePath())
                        .replace("${fileName}", childFile.getName() + (childFile.isDirectory() ? "/" : ""))
                        .replace("${fileSize}", fileSize)
                        .replace("${updateTime}", fileDate)
                        .replace("${downloadHref}", "/download/" + childFile.getAbsolutePath())
                        .replace("${hideDownload}", childFile.isFile() && childFile.canRead() ? "" : "layui-hide");
                builder.append(item).append("\n");
            }
        } while (false);
        String htmlStr = fileListStr
                .replace("${file_list}", builder.toString())
                .replace("${path}", directory == null ? "path" : directory.getAbsolutePath())
                .replace("${parentFile}", directory != null ? "/app/" + directory.getAbsolutePath() : "#");
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", htmlStr);
    }

    private NanoHTTPD.Response fileViewResponse(NanoHTTPD.IHTTPSession session, File file) throws Exception {
//        if(file == null || file.isDirectory()) {
//            return Utils.response404(session.getUri());
//        }
//        if(file.getName().endsWith(".db")) {
//            return fileViewDbResponse(session, file);
//        }
//        if(file.getName().endsWith(".xml")) {
//            return fileViewXmlResponse(session, file);
//        }
//
//        return responseText("can not view file:" + file.getName());
        return null;
    }
}
