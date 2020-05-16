package com.itlgl.android.debuglib;

public interface DebugServerCallback {
    void handleMessage(String message) throws Exception;
    byte[] handleApdu(byte[] apdu) throws Exception;
    String handleCommand(String commandType, String command) throws Exception;
    //void handleClipboard(String )
}
