package com.itlgl.android.debuglib.server;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.itlgl.android.debuglib.server.model.PtyRequest;
import com.itlgl.android.debuglib.utils.AppUtils;
import com.itlgl.android.debuglib.utils.LogUtils;
import com.itlgl.java.util.ByteUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import green_green_avk.ptyprocess.PtyProcess;

public class PtyWebSocket extends NanoWSD.WebSocket {

    private boolean mInit = false;
    private PtyProcess mPtyProcess;
    private OutputStream mPtyOut;
    private InputStream mPtyIn;
    private Thread mWatcherThread;
    private Thread mReaderThread;
    private Context mContext;
    private final Gson mGson = new Gson();

    public PtyWebSocket(Context context, NanoHTTPD.IHTTPSession handshakeRequest) {
        super(handshakeRequest);
        mContext = context;
    }

    @Override
    protected void onOpen() {
        LogUtils.d("PtyWebSocket onOpen");
        initPty();
    }

    private synchronized void initPty() {
        if(mInit) {
            return;
        }

        final Map<String, String> env = new HashMap<>(System.getenv());
        env.put("SHELL_SESSION_TOKEN", Long.toHexString(System.currentTimeMillis()).toUpperCase());
        env.put("TERM", "xterm");
        env.put("DATA_DIR", mContext.getApplicationInfo().dataDir);
        env.put("APP_DIR", "/data/data/" + AppUtils.getPackageName(mContext));
        if (Build.VERSION.SDK_INT >= 24) {
            env.put("PROTECTED_DATA_DIR", mContext.getApplicationInfo().deviceProtectedDataDir);
        }
        final File extDataDir = mContext.getExternalFilesDir(null);
        if (extDataDir != null) {
            env.put("EXTERNAL_DATA_DIR", extDataDir.getAbsolutePath());
            env.put("SHARED_DATA_DIR", extDataDir.getAbsolutePath());
        }
        env.put("PUBLIC_DATA_DIR", Environment.getExternalStorageDirectory().getAbsolutePath());
        env.put("LIB_DIR", mContext.getApplicationInfo().nativeLibraryDir);
        env.put("APP_APK", mContext.getApplicationInfo().sourceDir);
        env.put("APP_ID", AppUtils.getPackageName(mContext));
        env.put("APP_VERSION", AppUtils.getAppName(mContext));
        //env.put("APP_TARGET_SDK", Integer.toString(BuildConfig.TARGET_SDK_VERSION));
        env.put("MY_DEVICE_ABIS", TextUtils.join(" ", AppUtils.getAbis()));
        env.put("MY_ANDROID_SDK", Integer.toString(Build.VERSION.SDK_INT));
//        // Input URIs
//        for (final Map.Entry<String, String> ei : envInput.entrySet())
//            env.put("INPUT_" + ei.getKey(), ei.getValue());

//        String execute = "export TMPDIR=\"$DATA_DIR/tmp\"\n" +
//                "mkdir -p \"$TMPDIR\"\n" +
//                "export TERMSH=\"$LIB_DIR/libtermsh.so\"\n" +
//                "cd \"$DATA_DIR\"\n" +
//                "sh";
        String execute = "cd \"$APP_DIR\"\n" +
                "sh";
        try {
            mPtyProcess = PtyProcess.system(execute, env);
        } catch (Exception e) {
            LogUtils.e("PtyWebSocket fork pty error, %s", e);
            try {
                send("fork pty error, " + e + "\r\n");
            } catch (IOException ex) {
                //e.printStackTrace();
            }
            closeWebSocket();
            return;
        }
        mPtyOut = mPtyProcess.getOutputStream();
        mPtyIn = mPtyProcess.getInputStream();

        initWatcherThread();
        initReadThread();

        mInit = true;
    }

    private void initWatcherThread() {
        // watcher thread
        mWatcherThread = new Thread() {
            @Override
            public void run() {
                LogUtils.i("PtyWebSocket waiting exit");
                int status;
                while (true) {
                    try {
                        status = mPtyProcess.waitFor();
                    } catch (final InterruptedException ignored) {
                        continue;
                    }
                    break;
                }

                LogUtils.i("PtyWebSocket Subprocess exited: %s", status);
                closeWebSocket();
                closePty();
            }
        };
        mWatcherThread.setName("Process watcher");
        //mWatcherThread.start();
    }

    private void initReadThread() {
        mReaderThread = new Thread() {
            private final byte[] mBuffer = new byte[4096];

            @Override
            public void run() {
                try {
                    while(true) {
                        int read = mPtyIn.read(mBuffer);
                        if (read == -1) {
                            // EOF -- process exited
                            break;
                        }

                        if(read > 0) {
                            send(Arrays.copyOfRange(mBuffer, 0, read));
                        }
                    }
                } catch (IOException e) {
                }

                LogUtils.i("PtyWebSocket pty eof closed");
                closeWebSocket();
                closePty();
            }
        };
        mReaderThread.setName("TermSession input reader");
        mReaderThread.start();
    }

    private void closePty() {
        if(mPtyProcess != null) {
            mPtyProcess.destroy();
        }
        try {
            mPtyIn.close();
            mPtyOut.close();
        } catch (IOException e) {
            // We don't care if this fails
        } catch (NullPointerException e) {
            // ignore
        }
    }

    private void closeWebSocket() {
        try {
            send("terminal exit");
        } catch (IOException e) {
            //e.printStackTrace();
        }
        try {
            close(NanoWSD.WebSocketFrame.CloseCode.GoingAway,
                    "pty closed",
                    false);
            LogUtils.i("PtyWebSocket close");
        } catch (IOException e) {
            LogUtils.e("PtyWebSocket close error, %s", e);
        }
    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        LogUtils.d("PtyWebSocket Close [%s] %s:%s",
                initiatedByRemote ? "Remote" : "Self",
                code != null ? code : "UnknownCloseCode[" + code + "]",
                reason != null && !reason.isEmpty() ? reason : "");
        closePty();
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame message) {
        // 解析窗口参数
        String textPayload = message.getTextPayload();
        if(textPayload != null && textPayload.startsWith("{") && textPayload.endsWith("}")) {
            try {
                PtyRequest ptyRequest = mGson.fromJson(textPayload, PtyRequest.class);
                int w = ptyRequest.cols;
                int h = ptyRequest.rows;
                mPtyProcess.resize(w, h, w * 16, h * 16);
                LogUtils.d("PtyWebSocket resize w=%s, h=%s", w, h);
            } catch (Exception e) {
                // ignore
                LogUtils.d("PtyWebSocket parse term json error, %s", e);
            }
            return;
        }

        byte[] binaryPayload = message.getBinaryPayload();
        try {
            mPtyOut.write(binaryPayload);
        } catch (IOException e) {
            LogUtils.e("PtyWebSocket mTermOutputStream write error, %s", e);
        }
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame pong) {
        LogUtils.d("PtyWebSocket Pong " + pong);
    }

    @Override
    protected void onException(IOException exception) {
//        exception.printStackTrace();
        LogUtils.d("PtyWebSocket exception occured, %s", exception);
    }
}
