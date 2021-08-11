package com.itlgl.android.debuglib.server;

import com.itlgl.android.debuglib.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

public class ProcessWebSocket extends NanoWSD.WebSocket {

    private boolean mInit = false;
    private Process mProcess;
    private OutputStream mProcessOut;
    private InputStream mProcessErrIn;
    private InputStream mProcessIn;

    private Thread mWatcherThread;
    private Thread mReaderThread;
    private Thread mErrReaderThread;

    public ProcessWebSocket(NanoHTTPD.IHTTPSession handshakeRequest) {
        super(handshakeRequest);
    }

    @Override
    protected void onOpen() {
        LogUtils.d("ProcessWebSocket onOpen");
        initProcess();
    }

    private synchronized void initProcess() {
        if(mInit) {
            return;
        }

        try {
            mProcess = Runtime.getRuntime().exec("sh");
            mProcessOut = mProcess.getOutputStream();
            mProcessIn = mProcess.getInputStream();
            mProcessErrIn = mProcess.getErrorStream();

            LogUtils.d("ProcessWebSocket open process success");

            initWatcherThread();
            initReadThread();

            mInit = true;
        } catch (Exception e) {
            LogUtils.e("ProcessWebSocket open pty error, %s", e);
        }
    }

    private void initWatcherThread() {
        // watcher thread
        mWatcherThread = new Thread() {
            @Override
            public void run() {
                LogUtils.i("ProcessWebSocket waiting for process");
                int result = 0;
                try {
                    result = mProcess.waitFor();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
                LogUtils.i("ProcessWebSocket Subprocess exited: " + result);

                LogUtils.i("ProcessWebSocket pty exit");
                closeWebSocket();
                closeProcess();
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
                        int read = mProcessIn.read(mBuffer);
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

                LogUtils.i("ProcessWebSocket in eof closed");
                closeWebSocket();
                closeProcess();
            }
        };
        mReaderThread.setName("Process input reader");
        mReaderThread.start();


        mErrReaderThread = new Thread() {
            private final byte[] mBuffer = new byte[4096];

            @Override
            public void run() {
                try {
                    while(true) {
                        int read = mProcessErrIn.read(mBuffer);
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

                LogUtils.i("ProcessWebSocket err in eof closed");
                closeWebSocket();
                closeProcess();
            }
        };
        mErrReaderThread.setName("Process err input reader");
        mErrReaderThread.start();
    }

    private void closeProcess() {
        mProcess.destroy();
        try {
            mProcessOut.close();
            mProcessIn.close();
            mProcessErrIn.close();
        } catch (IOException e) {
            // We don't care if this fails
        } catch (NullPointerException e) {
            // ignore
        }
    }

    private void closeWebSocket() {
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
        closeProcess();
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame message) {
        byte[] binaryPayload = message.getBinaryPayload();
        try {
            mProcessOut.write(binaryPayload);
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
