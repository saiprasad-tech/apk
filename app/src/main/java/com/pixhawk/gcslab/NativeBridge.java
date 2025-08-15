package com.pixhawk.gcslab;

public class NativeBridge {
    static { System.loadLibrary("pixhawkcore"); }
    public static native void startTelemetry();
    public static native void stopTelemetry();
    public static native String getStats();
    public static native String getLatestBatch(int maxCount);
}
