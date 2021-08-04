package com.itlgl.android.debuglib.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DebugDBFactory implements DBFactory {

    @Override
    public SQLiteDB create(Context context, String path, String password) {
        return new DebugSQLiteDB(SQLiteDatabase.openOrCreateDatabase(path, null));
    }

}
