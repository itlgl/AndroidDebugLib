package com.itlgl.androiddebugapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.itlgl.android.debuglib.AndroidDebugLib;
import com.itlgl.androiddebugapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initView();
        checkPermissionTask();

        AndroidDebugLib.registerCommandCallback(new AndroidDebugLib.ICommand() {
            @Override
            public byte[] handleCommand(byte[] command) throws Exception {
                return new byte[]{(byte) 0x90, 0x00};
            }
        });
    }

    private void checkPermissionTask() {
        if (!hasPermission()) {
            requestPermissionTask();
        }
    }

    private static final String[] PERMISSION_PARAMS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : PERMISSION_PARAMS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void requestPermissionTask() {
        ActivityCompat.requestPermissions(this, PERMISSION_PARAMS, 1);
    }

    private void initView() {
        binding.btnCreateDb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDb();
            }
        });

        binding.btnCreateSp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSP();
            }
        });
    }

    private void createDb() {
        {
            MySQLiteOpenHelper helper = new MySQLiteOpenHelper(this, "test1.db");
            SQLiteDatabase db = helper.getWritableDatabase();
            db.execSQL("insert into `tbl_test` (`name`, `telephone`, `age`, `bytes`) values (?,?,?,?)",
                    new Object[]{"张三", "13122233445", 18, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}});
        }

        {
            MySQLiteOpenHelper helper = new MySQLiteOpenHelper(this, "test2.db");
            SQLiteDatabase db = helper.getWritableDatabase();
            db.execSQL("insert into `tbl_test` (`name`, `telephone`, `age`, `bytes`) values (?,?,?,?)",
                    new Object[]{"张三", "13122233445", 18, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}});
        }

        {
            MySQLiteOpenHelper helper = new MySQLiteOpenHelper(this, "test3.db");
            SQLiteDatabase db = helper.getWritableDatabase();
            db.execSQL("insert into `tbl_test` (`name`, `telephone`, `age`, `bytes`) values (?,?,?,?)",
                    new Object[]{"张三", "13122233445", 18, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}});
        }
    }

    private void createSP() {
        {
            SharedPreferences sp = getSharedPreferences("test1", Context.MODE_PRIVATE);
            sp.edit().putString("test", "hello world").apply();
        }

        {
            SharedPreferences sp = getSharedPreferences("test2", Context.MODE_PRIVATE);
            sp.edit().putString("test", "hello world").apply();
        }

        {
            SharedPreferences sp = getSharedPreferences("test3", Context.MODE_PRIVATE);
            sp.edit().putString("test", "hello world").apply();
        }
    }
}
