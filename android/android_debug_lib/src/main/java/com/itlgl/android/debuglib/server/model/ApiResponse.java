package com.itlgl.android.debuglib.server.model;

public class ApiResponse {
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_NOT_REGISTER_IMPL = 1;
    public static final int CODE_COMMAND_INVALID = 2;
    public static final int CODE_EXECUTE_COMMAND_ERROR = 3;
    public static final int CODE_ERROR = 4;

    public int code;
    public String responseHex;

    public ApiResponse() {}

    public ApiResponse(int code) {
        this.code = code;
    }

    public ApiResponse(int code, String responseHex) {
        this.code = code;
        this.responseHex = responseHex;
    }
}
