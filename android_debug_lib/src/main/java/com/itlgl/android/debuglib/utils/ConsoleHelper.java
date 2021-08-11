package com.itlgl.android.debuglib.utils;

import java.io.IOException;

/**
 * @author guanliang on 2021/8/6
 */
public class ConsoleHelper {
    private static final ConsoleHelper INSTANCE = new ConsoleHelper();

    public static ConsoleHelper getInstance() {
        return INSTANCE;
    }

    private Process mProcess;
    private StringBuffer mInputBuffer = new StringBuffer();
    private StringBuffer mErrorBuffer = new StringBuffer();

    private ConsoleHelper() {}

    private synchronized void initProcess() throws IOException {
        if(mProcess == null) {
            try {
                mProcess = Runtime.getRuntime().exec("sh");

                new Thread() {
                    @Override
                    public void run() {
//                        mProcess.get
                    }
                }.start();
            } catch (IOException e) {
                LogUtils.e("create process error, " + e);
                throw e;
            }
        }
    }

    public synchronized void close() {
        mProcess.destroy();
    }

//    public synchronized String exec(String cmd) {
//
//    }
}
