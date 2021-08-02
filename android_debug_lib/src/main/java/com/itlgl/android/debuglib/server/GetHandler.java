package com.itlgl.android.debuglib.server;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.itlgl.android.debuglib.db.DebugDBFactory;
import com.itlgl.android.debuglib.db.DebugSQLiteDB;
import com.itlgl.android.debuglib.db.SQLiteDB;
import com.itlgl.android.debuglib.db.model.Response;
import com.itlgl.android.debuglib.db.model.TableDataResponse;
import com.itlgl.android.debuglib.db.utils.DatabaseHelper;
import com.itlgl.android.debuglib.utils.AppUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class GetHandler extends HttpMethodHandler {

    private static final String URI_ROOT = "/";
    private static final String URI_ROOT2 = "/index.html";
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
            System.out.println("params=" + session.getParms());
            File file = new File(strUri.substring("/app".length()));
            if(!file.exists()) {
                return Utils.response404(strUri);
            }
            if(file.isDirectory()) {
                return responseFileList(session, file);
            } else {
                return responseFileView(session, file);
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

    /**
     * 文件列表查看html
     */
    private NanoHTTPD.Response responseFileList(NanoHTTPD.IHTTPSession session, File directory) throws Exception {
        String fileListStr = Utils.readAssetsFileString(getContext(), "file_list.html");
        String fileListItemTemp = Utils.readAssetsFileString(getContext(), "file_list_item.html");
        StringBuilder builder = new StringBuilder();
        do {
            if(directory == null || !directory.isDirectory() || !directory.canRead()) {
                break;
            }
            File[] files = directory.listFiles();
            if(files == null || files.length == 0) {
                break;
            }
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (File childFile : files) {
                String fileDate = "";
                try {
                    fileDate = SIMPLE_DATE_FORMAT.format(new Date(childFile.lastModified()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String fileSize = childFile.isFile() && childFile.canRead() ? Utils.formatFileSize(childFile.length()) : "";
                String item = fileListItemTemp
                        .replace("${fileHref}", "/app" + childFile.getAbsolutePath())
                        .replace("${fileName}", childFile.getName() + (childFile.isDirectory() ? "/" : ""))
                        .replace("${fileSize}", fileSize)
                        .replace("${updateTime}", fileDate)
                        .replace("${downloadHref}", "/download" + childFile.getAbsolutePath())
                        .replace("${hideDownload}", childFile.isFile() && childFile.canRead() ? "" : "layui-hide");
                builder.append(item).append("\n");
            }
        } while (false);

        File parentDir = directory != null ? directory.getParentFile() : null;
        String htmlStr = fileListStr
                .replace("${file_list}", builder.toString())
                .replace("${path}", directory == null ? "path" : directory.getAbsolutePath())
                .replace("${parentFile}", parentDir != null ? "/app" + parentDir.getAbsolutePath() : "#");
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", htmlStr);
    }

    private NanoHTTPD.Response responseFileView(NanoHTTPD.IHTTPSession session, File file) throws Exception {
        if(file == null || file.isDirectory()) {
            return Utils.response404(session.getUri());
        }
        if(!file.canRead()) {
            return NanoHTTPD.newFixedLengthResponse("无读取此文件的权限");
        }
        String viewType = session.getParms().get("viewType");
        if("txt".equalsIgnoreCase(viewType) || file.getName().endsWith(".txt")) {
            return responseFileViewTxt(session, file);
        }
        if("xml".equalsIgnoreCase(viewType) || file.getName().endsWith(".xml")) {
            return responseFileViewXml(session, file);
        }
        if("json".equalsIgnoreCase(viewType) || file.getName().endsWith(".json")) {
            return responseFileViewJson(session, file);
        }
        if("db".equalsIgnoreCase(viewType) || file.getName().endsWith(".db")) {
            return responseFileViewDb(session, file);
        }

        // not support html
        String notSupportHtml = Utils.readAssetsFileString(getContext(), "file_view_not_support.html");
        notSupportHtml = notSupportHtml
                .replace("${fileHref}", "/app" + file.getAbsolutePath())
                .replace("${filePath}", file.getAbsolutePath());
        return NanoHTTPD.newFixedLengthResponse(notSupportHtml);
    }

    private NanoHTTPD.Response responseFileViewXml(NanoHTTPD.IHTTPSession session, File file) throws FileNotFoundException {
        // TODO: 2021/8/2 如果是sp可以直接用列表展示sp并支持编辑和新增
        FileInputStream fis = new FileInputStream(file);
        return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, "application/xml", fis);
    }

    private NanoHTTPD.Response responseFileViewJson(NanoHTTPD.IHTTPSession session, File file) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, "application/json", fis);
    }

    private NanoHTTPD.Response responseFileViewTxt(NanoHTTPD.IHTTPSession session, File file) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, "text/plain", fis);
    }

    private NanoHTTPD.Response responseFileViewDb(NanoHTTPD.IHTTPSession session, File file) throws Exception {
        Map<String, String> parms = session.getParms();
        String sql = parms.get("sql");
        if(sql != null) {
            sql = URLDecoder.decode(sql).trim();
        }

        String dbHtml = Utils.readAssetsFileString(getContext(), "file_view_db.html");
        StringBuilder tableItemBuilder = new StringBuilder();

        SQLiteDB db = null;
        try {
            db = new DebugDBFactory().create(getContext(), file.getAbsolutePath(), null);
            Response allTableName = DatabaseHelper.getAllTableName(db);
            for (Object tableNameObj : allTableName.rows) {
                String tableName = (String) tableNameObj;
                String tableItemHtml = Utils.readAssetsFileString(getContext(), "file_view_db_item_table_name.html");
                tableItemHtml = tableItemHtml.replace("${tableName}", tableName)
                        // 添加 layui-menu-item-checked 可以让条目选中
                        .replace("${itemChecked}", sql != null && sql.contains(tableName) ? "layui-menu-item-checked" : "");
                tableItemBuilder.append(tableItemHtml);
            }

            dbHtml = dbHtml.replace("${filePath}", file.getAbsolutePath())
                    .replace("${sqlValue}", sql != null ? sql : "")
                    .replace("${dbTableItems}", tableItemBuilder.toString());

            // 如果sql不为空，执行一下sql
            if(sql != null) {
                TableDataResponse execResponse = DatabaseHelper.execSql(db, sql);
                StringBuilder sqlTitlesBuilder = new StringBuilder();
                StringBuilder sqlRowsBuilder = new StringBuilder();
                if(execResponse.isSuccessful) {
                    // 标题
                    if(execResponse.tableInfos != null && execResponse.tableInfos.size() != 0) {
                        for (TableDataResponse.TableInfo ti : execResponse.tableInfos) {
                            sqlTitlesBuilder.append("<th>").append(ti.title).append("</th>");
                        }
                    }

                    if(execResponse.rows != null && execResponse.rows.size() > 0) {
                        for (List<TableDataResponse.ColumnData> columnDataList : execResponse.rows) {
                            if(columnDataList != null && columnDataList.size() != 0) {

                                sqlRowsBuilder.append("<tr>");
                                for (TableDataResponse.ColumnData columnData : columnDataList) {
                                    sqlRowsBuilder.append("<th>").append(columnData.value).append("</th>");
                                }
                                sqlRowsBuilder.append("</tr>");
                            }
                        }
                    }
                } else {
                    sqlTitlesBuilder.append("<th>exec sql</th>");
                    sqlRowsBuilder.append(String.format("<tr><td>exec sql error, %s</td></tr>", execResponse.errorMessage));
                }

                dbHtml = dbHtml.replace("${sqlTitles}", sqlTitlesBuilder.toString())
                        .replace("${sqlRows}", sqlRowsBuilder.toString());
            } else {
                dbHtml = dbHtml.replace("${sqlTitles}", "")
                        .replace("${sqlRows}", "");
            }

        } finally {
            if(db != null) {
                db.close();
            }
        }

        return NanoHTTPD.newFixedLengthResponse(dbHtml);
    }
}
