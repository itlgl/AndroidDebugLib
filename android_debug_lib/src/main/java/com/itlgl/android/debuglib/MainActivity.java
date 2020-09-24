package com.itlgl.android.debuglib;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.itlgl.android.debuglib.utils.AppUtils;

public class MainActivity extends Activity {

    ImageView iv_app_icon;
    TextView tv_app_name;
    TextView tv_app_package;

    EditText et_port;
    Button btn_start_debug;
    Button btn_stop_debug;
    TextView tv_adb_cmd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.android_debug_lib_activity_main);
        initView();
    }

    private void initView() {
        if(getActionBar() != null) {
            getActionBar().hide();
        }

        iv_app_icon = findViewById(R.id.iv_app_icon);
        tv_app_name = findViewById(R.id.tv_app_name);
        tv_app_package = findViewById(R.id.tv_app_package);

        et_port = findViewById(R.id.et_port);
        btn_start_debug = findViewById(R.id.btn_start_debug);
        btn_stop_debug = findViewById(R.id.btn_stop_debug);
        tv_adb_cmd = findViewById(R.id.tv_adb_cmd);

        iv_app_icon.setImageBitmap(AppUtils.getBitmap(this));
        tv_app_name.setText(String.format("%s(%s)", AppUtils.getAppName(this), AppUtils.getVersionName(this)));
        tv_app_package.setText(AppUtils.getPackageName(this));

        btn_start_debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int port = 0;
                try {
                    port = Integer.parseInt(et_port.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Invalid port", Toast.LENGTH_SHORT).show();
                    return;
                }
                AndroidDebugLib.startDebug(getApplicationContext(), port);
            }
        });

        btn_stop_debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndroidDebugLib.stopDebug();
            }
        });
    }
}
