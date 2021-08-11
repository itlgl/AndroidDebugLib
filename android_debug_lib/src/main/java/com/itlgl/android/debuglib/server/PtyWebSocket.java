package com.itlgl.android.debuglib.server;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

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

//        String execute = "sh" +
//                "export TMPDIR=\"$DATA_DIR/tmp\"" +
//                "mkdir -p \"$TMPDIR\"" +
//                "export TERMSH=\"$LIB_DIR/libtermsh.so\"" +
//                "cd \"$DATA_DIR\"";
        String execute = "sh";
        final PtyProcess p = PtyProcess.system(execute, env);
        mPtyProcess = p;
        mPtyOut = p.getOutputStream();
        mPtyIn = p.getInputStream();

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
                            System.out.println("read==" + ByteUtils.toHex(mBuffer, 0, read));
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
        mPtyProcess.destroy();
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
            send("pty closed");
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
        byte[] binaryPayload = message.getBinaryPayload();
        System.out.println("read from ws: " + message.getTextPayload());
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
