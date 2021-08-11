package com.itlgl.android.debuglib.server;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import com.itlgl.android.debuglib.utils.LogUtils;
import com.itlgl.java.util.ByteUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import jackpal.androidterm.Exec;
import jackpal.androidterm.TermExec;

public class PtyWebSocket extends NanoWSD.WebSocket {

    private boolean mInit = false;
    private ParcelFileDescriptor mTermFd;
    private int mProcId;
    private OutputStream mTermOutputStream;
    private InputStream mTermInputStream;
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
        try {
            String homeDefValue = mContext.getDir("HOME", Context.MODE_PRIVATE).getAbsolutePath();

            String[] env = new String[3];
            env[0] = "TERM=" + "xterm";
            env[1] = "PATH=" + System.getenv("PATH");
            env[2] = "HOME=" + homeDefValue;

            mTermFd = ParcelFileDescriptor.open(new File("/dev/ptmx"), ParcelFileDescriptor.MODE_READ_WRITE);
            mProcId = TermExec.createSubprocess(mTermFd, "/system/bin/sh", null, env);
            LogUtils.d("PtyWebSocket open pty success, mProcId=%s", mProcId);

            mTermOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(mTermFd);
            mTermInputStream = new ParcelFileDescriptor.AutoCloseInputStream(mTermFd);

            Exec.setPtyWindowSizeInternal(mTermFd.getFd(), 80, 24, 0, 0);
            Exec.setPtyUTF8ModeInternal(mTermFd.getFd(), true);

            mTermOutputStream.write("ls\r".getBytes());

            initWatcherThread();
            initReadThread();

            mInit = true;
        } catch (Exception e) {
            LogUtils.e("PtyWebSocket open pty error, %s", e);
        }
    }

    private void initWatcherThread() {
        // watcher thread
        mWatcherThread = new Thread() {
            @Override
            public void run() {
                LogUtils.i("PtyWebSocket waiting for: " + mProcId);
                int result = TermExec.waitFor(mProcId);
                LogUtils.i("PtyWebSocket Subprocess exited: " + result);

                LogUtils.i("PtyWebSocket pty exit");
                closeWebSocket();
                closePty();
            }
        };
        mWatcherThread.setName("Process watcher");
        mWatcherThread.start();
    }

    private void initReadThread() {
        mReaderThread = new Thread() {
            private final byte[] mBuffer = new byte[4096];

            @Override
            public void run() {
                try {
                    while(true) {
                        int read = mTermInputStream.read(mBuffer);
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
        TermExec.sendSignal(-mProcId, 1);
        try {
            mTermInputStream.close();
            mTermOutputStream.close();
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
            mTermOutputStream.write(binaryPayload);
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
