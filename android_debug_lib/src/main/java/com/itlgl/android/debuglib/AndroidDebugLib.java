package com.itlgl.android.debuglib;

import com.itlgl.android.debuglib.utils.LogUtils;

public class AndroidDebugLib {
    private static ICommand sCommandImpl;

    public interface ICommand {
        byte[] handleCommand(byte[] command) throws Exception;
    }

    /**
     * 是否打印log
     * @param log false-不打印log true-打印log
     */
    public static void printLog(boolean log) {
        LogUtils.printLog(log);
    }

    public synchronized static void registerCommandCallback(ICommand command) {
        sCommandImpl = command;
    }

    public synchronized static ICommand getCommandImpl() {
        return sCommandImpl;
    }
}
