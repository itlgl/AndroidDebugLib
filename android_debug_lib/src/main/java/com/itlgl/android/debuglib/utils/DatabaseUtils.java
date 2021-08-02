package com.itlgl.android.debuglib.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.itlgl.java.util.ByteUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseUtils {
    private static Map<File, SQLiteDatabase> DB_CACHE = new HashMap<>();

    public static SQLiteDatabase openDb(File dbFile) throws Exception {
        if(dbFile == null) {
            return null;
        }
        if(DB_CACHE.containsKey(dbFile)) {
            SQLiteDatabase sqLiteDatabase = DB_CACHE.get(dbFile);
            if(!sqLiteDatabase.isOpen()) {
                DB_CACHE.remove(dbFile);
            } else {
                return sqLiteDatabase;
            }
        }
        SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
        DB_CACHE.put(dbFile, sqLiteDatabase);
        return sqLiteDatabase;
    }

    public static void closeAllDatabase() {
        for (SQLiteDatabase db : DB_CACHE.values()) {
            db.close();
        }
    }

    public static String execSQL(File dbFile, String sql) {
        if(dbFile == null || sql == null || TextUtils.isEmpty(sql.trim())) {
            return "";
        }
        sql = sql.trim();

        SQLiteDatabase database = null;
        try {
            database = openDb(dbFile);
            String firstWordUpperCase = getFirstWord(sql).toUpperCase();
            switch (firstWordUpperCase) {
                case "UPDATE":
                case "DELETE":
                    return executeUpdateDelete(database, sql);
                case "INSERT":
                    return executeInsert(database, sql);
                case "SELECT":
                case "PRAGMA":
                case "EXPLAIN":
                    return executeSelect(database, sql);
                default:
                    return executeRawQuery(database, sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "execSQL error:" + e.getMessage();
        } finally {
            if(database != null) {
                database.close();
            }
        }
    }

    public static String getDbTablesInfo(File dbFile) {
        if(dbFile == null) {
            return "";
        }

        SQLiteDatabase database = null;
        try {
            database = openDb(dbFile);
            return executeSelect(database, "select * from sqlite_master");
        } catch (Exception e) {
            e.printStackTrace();
            return "getDbTablesInfo error:" + e.getMessage();
        } finally {
            if(database != null) {
                database.close();
            }
        }
    }

    private static String executeRawQuery(SQLiteDatabase database, String sql) {
        database.execSQL(sql);
        return "execSQL success";
    }

    private static String executeSelect(SQLiteDatabase database, String sql) {
        Cursor cursor = database.rawQuery(sql, null);
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("query result:\n");
        try {
            int columnCount = cursor.getColumnCount();
            while (cursor.moveToNext()) {
                for (int i = 0; i < columnCount; i++) {
                    String value = null;
                    do {
                        try {
                            value = cursor.getString(i);
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            byte[] blob = cursor.getBlob(i);
                            value = "[BLOB]" + ByteUtil.toHex(blob);
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        value = "[non-string-value]";
                    } while (false);


                    if(i != 0) {
                        resultBuilder.append(", ");
                    }
                    resultBuilder.append(value);
                }
                resultBuilder.append("\n");
            }

        } finally {
            cursor.close();
        }
        return resultBuilder.toString();
    }

    private static String executeInsert(SQLiteDatabase database, String sql) {
        SQLiteStatement statement = database.compileStatement(sql);
        long count = statement.executeInsert();
        return "insert " + count + " rows";
    }

    private static String executeUpdateDelete(SQLiteDatabase database, String sql) {
        SQLiteStatement statement = database.compileStatement(sql);
        int count = statement.executeUpdateDelete();
        return count + " rows change";
    }

    private static String getFirstWord(String s) {
        s = s.trim();
        int firstSpace = s.indexOf(' ');
        return firstSpace >= 0 ? s.substring(0, firstSpace) : s;
    }
}
